package tv.mango.app.data

/**
 * What a screen is currently showing.
 *
 * Empty is separate from Success-with-an-empty-list on purpose: "no results for
 * that search" and "here are your results" want different words on screen, and
 * separating them here means no screen has to remember to check `isEmpty()`.
 */
sealed interface UiState<out T> {

    data object Loading : UiState<Nothing>

    data class Content<T>(val value: T) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error(val reason: FailureReason) : UiState<Nothing>
}
