package tv.mango.app.data

/**
 * The outcome of a data request.
 *
 * Providers return this rather than throwing. A failed network call is an
 * ordinary, expected outcome for a streaming client on a home connection, not
 * an exceptional one, and modelling it in the return type means no screen can
 * forget to handle it.
 */
sealed interface DataResult<out T> {

    data class Success<T>(val value: T) : DataResult<T>

    data class Failure(
        val reason: FailureReason,
        val cause: Throwable? = null,
    ) : DataResult<Nothing>
}

/**
 * Why a request failed, in terms the interface can act on.
 *
 * Deliberately coarse. The viewer is told what to do next, never what went
 * wrong internally, so anything finer than this would only ever be logged.
 */
enum class FailureReason {
    /** Unreachable or timed out. Retrying is worth offering. */
    NETWORK,

    /** The provider answered, but has nothing under that identifier. */
    NOT_FOUND,

    UNKNOWN,
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}
