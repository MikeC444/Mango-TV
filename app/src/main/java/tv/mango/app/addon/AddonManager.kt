package tv.mango.app.addon

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.protocol.ProtocolFailure
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.utilities.Logger

/**
 * Decides which add-ons to ask, and asks them all at once.
 *
 * Two jobs, both of which the resolvers above would otherwise each reimplement.
 *
 * **Routing.** An add-on is asked only if its manifest says it can answer -
 * that it serves the resource, for that content type, for that kind of
 * identifier. Nothing anywhere names a particular add-on: capability is read
 * from the manifest, so an add-on this application has never heard of works
 * exactly as well as one it was written alongside.
 *
 * **Fan-out.** Five add-ons that can each answer are asked simultaneously, not
 * one after another, because asking sequentially would make the wait the sum of
 * five services rather than the slowest of them. Every request is bounded by
 * its own timeout and its own error handling, so a broken or hanging add-on
 * costs its own result and nothing else. The slowest member cannot hold up the
 * ones that already answered.
 */
class AddonManager(
    private val store: AddonStore,
    val client: StremioProtocolClient,
) {

    /**
     * Enabled add-ons that can answer this request, highest priority first.
     *
     * @param type content type, or null to ignore it.
     * @param id an identifier the add-on must recognise the shape of.
     */
    suspend fun capableOf(
        resource: AddonResourceName,
        type: String? = null,
        id: String? = null,
    ): List<Addon> = store.enabled().filter { addon ->
        addon.manifest.supports(resource, type) &&
            (id == null || addon.manifest.handlesId(resource, id))
    }

    /**
     * Runs [query] against every add-on at once and collects what came back.
     *
     * Failures are values here, not exceptions. Each branch is wrapped so that
     * a add-on which throws, hangs, or answers with nonsense is recorded as
     * failed and the others are unaffected - which is the whole reason this
     * exists rather than a bare `map { }`.
     *
     * Structured concurrency means the caller cancelling - leaving the screen,
     * typically - cancels every outstanding request rather than leaving them to
     * finish into a result nobody will read.
     */
    suspend fun <T> fanOut(
        addons: List<Addon>,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        query: suspend (Addon) -> AddonReply<T>,
    ): Aggregated<T> = fanOutOver(addons, { it }, timeoutMillis, query)

    /**
     * The same, over arbitrary requests rather than over add-ons.
     *
     * One add-on can be the target of several requests at once - an add-on
     * publishing four catalogues contributes four rows - and those should be
     * fetched simultaneously too, not collapsed into one because they share a
     * provider. [addonOf] says which add-on a request belongs to, for routing
     * and for reporting which one failed.
     */
    suspend fun <R, T> fanOutOver(
        requests: List<R>,
        addonOf: (R) -> Addon,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        query: suspend (R) -> AddonReply<T>,
    ): Aggregated<T> = coroutineScope {
        if (requests.isEmpty()) return@coroutineScope Aggregated()

        val replies = requests.map { request ->
            async {
                val addon = addonOf(request)
                val reply = withTimeoutOrNull(timeoutMillis) {
                    runCatching { query(request) }.getOrElse { error ->
                        // An add-on cannot be trusted not to produce something
                        // the parser chokes on. That is its problem, not the
                        // screen's.
                        Logger.w("Add-on ${addon.id} failed", error)
                        AddonReply.Failed(ProtocolFailure.UNKNOWN)
                    }
                } ?: AddonReply.Failed(ProtocolFailure.UNREACHABLE)
                addon to reply
            }
        }.awaitAll()

        val items = mutableListOf<T>()
        val succeeded = mutableListOf<Addon>()
        val failed = mutableListOf<Addon>()

        replies.forEach { (addon, reply) ->
            when (reply) {
                is AddonReply.Answered -> {
                    items += reply.values
                    if (addon !in succeeded) succeeded += addon
                }
                is AddonReply.Failed -> if (addon !in failed) failed += addon
            }
        }

        // An add-on with several requests in flight may have some succeed and
        // some fail. It counts as having answered, so a single failed catalogue
        // does not report the whole add-on as broken.
        Aggregated(items = items, succeeded = succeeded, failed = failed - succeeded.toSet())
    }

    /**
     * Asks add-ons in priority order and stops at the first usable answer.
     *
     * For requests where one answer is what is wanted - a title's metadata,
     * say - rather than everything anyone has. Sequential on purpose: the
     * highest-priority add-on usually answers, and asking all of them
     * simultaneously to then discard all but one is work and bandwidth spent
     * for nothing on a device with little of either.
     */
    suspend fun <T> firstAnswer(
        addons: List<Addon>,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        query: suspend (Addon) -> T?,
    ): Pair<Addon, T>? {
        addons.forEach { addon ->
            val answer = withTimeoutOrNull(timeoutMillis) {
                runCatching { query(addon) }.getOrElse { error ->
                    Logger.w("Add-on ${addon.id} failed", error)
                    null
                }
            }
            if (answer != null) return addon to answer
        }
        return null
    }

    companion object {
        /**
         * Per add-on, not for the request as a whole. An add-on that has not
         * answered in this long is one the viewer is already waiting on.
         */
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L

        /** Shorter, for the home screen, which must not feel slow to open. */
        const val CATALOG_TIMEOUT_MILLIS = 6_000L

        /**
         * Longer than the default. Resolving a playable source - scraping,
         * hashing, checking a debrid account - routinely takes longer than
         * listing a catalogue entry, and a viewer who pressed Play is waiting
         * for sources rather than browsing past them.
         */
        const val STREAM_TIMEOUT_MILLIS = 12_000L
    }
}

/** What one add-on had to say. */
sealed interface AddonReply<out T> {
    data class Answered<T>(val values: List<T>) : AddonReply<T>
    data class Failed(val reason: ProtocolFailure) : AddonReply<Nothing>
}

/**
 * The combined result of asking several add-ons.
 *
 * Keeps which add-ons failed rather than only what succeeded, so a screen can
 * say that some sources could not be loaded without pretending everything was
 * fine and without treating a partial result as a total failure.
 */
data class Aggregated<T>(
    val items: List<T> = emptyList(),
    val succeeded: List<Addon> = emptyList(),
    val failed: List<Addon> = emptyList(),
) {
    /** Some answered and some did not: worth mentioning, not worth alarming over. */
    val isPartial: Boolean get() = failed.isNotEmpty() && succeeded.isNotEmpty()

    /** Nobody answered. Only this is an error state. */
    val isTotalFailure: Boolean get() = succeeded.isEmpty() && failed.isNotEmpty()
}
