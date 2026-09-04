package tv.mango.app.data.refresh

import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState

/**
 * What a screen shows while it is being refreshed.
 *
 * A refresh is not the same event as a first load, even though the data layer
 * produces the identical sequence for both. On a first load there is nothing to
 * show and Loading is the honest answer. On a refresh there is a screen full of
 * content the viewer is looking at, and replacing it with a blank while new
 * data arrives is worse than showing them slightly old content for a moment -
 * it loses their place, and it looks like a fault.
 *
 * So the two are told apart here, by whether anything is already held.
 */
data class Refreshable<out T>(
    val content: UiState<T>,
    /** A refresh is in flight over content already on screen. */
    val isRefreshing: Boolean = false,
    /** The last refresh failed, and the content on screen is the old one. */
    val refreshFailed: Boolean = false,
) {
    val hasContent: Boolean get() = content is UiState.Content
}

/**
 * Folds what the data layer just produced against what is already on screen.
 *
 * Kept as a pure function, deliberately. This is the whole of the behaviour
 * worth getting right, and as a function of two values it can be tested
 * directly rather than inferred from a running screen on hardware that is not
 * available here.
 */
object RefreshFold {

    fun <T> next(previous: Refreshable<T>, incoming: UiState<T>): Refreshable<T> =
        when (incoming) {
            // New content always wins, and clears whatever the last attempt
            // concluded.
            is UiState.Content -> Refreshable(content = incoming)

            // Suppressed when there is something to keep looking at. This is
            // the emission that used to blank the home screen on every return.
            is UiState.Loading -> if (previous.hasContent) {
                previous.copy(isRefreshing = true, refreshFailed = false)
            } else {
                Refreshable(content = UiState.Loading)
            }

            // A failed refresh leaves the working screen alone and says so
            // quietly. Only a failure with nothing to fall back on becomes the
            // screen.
            is UiState.Error -> if (previous.hasContent) {
                previous.copy(isRefreshing = false, refreshFailed = true)
            } else {
                Refreshable(content = incoming)
            }

            // An empty result is a real answer about the catalogue rather than
            // a transport failure, so it replaces what came before even when
            // that was content: a provider whose catalogue has emptied should
            // not keep showing titles it no longer offers.
            is UiState.Empty -> Refreshable(content = UiState.Empty)
        }

    /** The state a screen starts in, before anything has been requested. */
    fun <T> initial(): Refreshable<T> = Refreshable(content = UiState.Loading)

    /** Convenience for the reason behind a surfaced error, if there is one. */
    fun <T> failureReasonOf(state: Refreshable<T>): FailureReason? =
        (state.content as? UiState.Error)?.reason
}
