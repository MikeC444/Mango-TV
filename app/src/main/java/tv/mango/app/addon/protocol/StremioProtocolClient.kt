package tv.mango.app.addon.protocol

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Talks to add-ons over the Stremio protocol.
 *
 * The transport, and nothing else. It fetches a URL and hands back a parsed
 * JSON object or a reason it could not; what the object means is the parsers'
 * business, and which add-on to ask is the manager's.
 *
 * Add-ons are untrusted third-party services, so every failure mode is a value
 * rather than an exception: unreachable, too slow, refused, wrong content,
 * unparseable. Nothing here throws into a coroutine that a screen is waiting
 * on, because one broken add-on must never take a screen down with it.
 */
class StremioProtocolClient(
    private val client: OkHttpClient,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface Outcome {
        data class Success(val body: JsonObject) : Outcome
        data class Failure(val reason: ProtocolFailure, val status: Int? = null) : Outcome
    }

    /**
     * Fetches and parses one resource.
     *
     * Cancellation is honoured: leaving a screen cancels the coroutine, which
     * cancels the underlying call rather than leaving it to finish into a
     * result nobody will read.
     */
    suspend fun fetch(url: String): Outcome = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        val response = runCatching { client.newCall(request).await() }
            .getOrElse { error ->
                return@withContext Outcome.Failure(
                    if (error is IOException) ProtocolFailure.UNREACHABLE
                    else ProtocolFailure.UNKNOWN,
                )
            }

        response.use { result ->
            if (!result.isSuccessful) {
                return@withContext Outcome.Failure(
                    when (result.code) {
                        404 -> ProtocolFailure.NOT_FOUND
                        in 500..599 -> ProtocolFailure.SERVER_ERROR
                        else -> ProtocolFailure.REFUSED
                    },
                    status = result.code,
                )
            }

            // Reading the body is a second chance to fail, and it fails
            // differently from connecting. A response whose headers arrived but
            // whose body then stalled is a transient network problem worth
            // retrying, not an add-on that returned nothing - so the two are
            // told apart here rather than collapsed into one reason.
            val body = try {
                result.body?.byteStream()?.readBoundedText() ?: BodyRead.Empty
            } catch (error: IOException) {
                return@withContext Outcome.Failure(ProtocolFailure.UNREACHABLE)
            } catch (error: Exception) {
                return@withContext Outcome.Failure(ProtocolFailure.UNKNOWN)
            }

            val text = when (body) {
                is BodyRead.Text -> body.value
                BodyRead.Empty -> return@withContext Outcome.Failure(ProtocolFailure.EMPTY_BODY)
                BodyRead.TooLarge -> return@withContext Outcome.Failure(ProtocolFailure.TOO_LARGE)
            }

            val parsed = runCatching { json.parseToJsonElement(text) }.getOrNull()
                ?: return@withContext Outcome.Failure(ProtocolFailure.MALFORMED)

            (parsed as? JsonObject)
                ?.let { Outcome.Success(it) }
                ?: Outcome.Failure(ProtocolFailure.MALFORMED)
        }
    }

    private sealed interface BodyRead {
        data class Text(val value: String) : BodyRead
        data object Empty : BodyRead
        data object TooLarge : BodyRead
    }

    /**
     * Reads the body with a ceiling on its size.
     *
     * An add-on is an untrusted service, and one returning an unbounded body -
     * by accident or otherwise - would be able to exhaust the heap of the
     * device that asked. Exceeding the ceiling abandons the read rather than
     * truncating it, because half a JSON document is not a smaller document,
     * it is a broken one.
     */
    private fun java.io.InputStream.readBoundedText(): BodyRead {
        val out = StringBuilder()
        val chars = CharArray(READ_CHUNK)
        var total = 0
        reader(Charsets.UTF_8).use { reader ->
            while (true) {
                val read = reader.read(chars)
                if (read < 0) break
                total += read
                if (total > MAX_BODY_CHARS) return BodyRead.TooLarge
                out.appendRange(chars, 0, read)
            }
        }
        return if (out.isBlank()) BodyRead.Empty else BodyRead.Text(out.toString())
    }

    private companion object {
        const val READ_CHUNK = 8 * 1024

        /**
         * Roughly eight megabytes of text. Far beyond any legitimate catalogue
         * page, and far below what would trouble the heap.
         */
        const val MAX_BODY_CHARS = 8 * 1024 * 1024
    }
}

/** Why a request to an add-on did not produce a usable response. */
enum class ProtocolFailure {
    /** No route, DNS failure, timeout, or the device is offline. */
    UNREACHABLE,

    /** The add-on answered 404: it does not have this. */
    NOT_FOUND,

    /** The add-on answered 5xx. */
    SERVER_ERROR,

    /** Any other unsuccessful status. */
    REFUSED,

    /** Answered successfully, with nothing in the body. */
    EMPTY_BODY,

    /** The body exceeded what this application is willing to hold in memory. */
    TOO_LARGE,

    /** Answered, but not with JSON this application can read. */
    MALFORMED,

    UNKNOWN,
    ;

    /** Whether asking again shortly is worth doing. */
    val isTransient: Boolean
        get() = this == UNREACHABLE || this == SERVER_ERROR
}

/**
 * Bridges OkHttp's callback API onto coroutines, cancelling the call if the
 * coroutine is cancelled.
 */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { runCatching { cancel() } }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        },
    )
}

private fun <T> CancellableContinuation<T>.resumeWithException(error: Throwable) {
    if (isActive) resumeWith(Result.failure(error))
}
