package tv.mango.app.navigation

/**
 * Every destination in the application, named once.
 *
 * Screens never construct each other's fragments or know each other's class
 * names; they ask [Navigator] for a route. Adding a screen means adding a case
 * here and a branch in the navigator, not touching the screens that link to it.
 */
sealed interface Route {

    /** Destinations reachable from the navigation rail. */
    sealed interface Section : Route

    data object Home : Section
    data object Movies : Section
    data object Series : Section
    data object Search : Section
    data object Library : Section
    data object Settings : Section

    data class MovieDetail(val id: String) : Route
    data class SeriesDetail(val id: String) : Route

    /** Reached from Settings. The installed add-ons, in priority order. */
    data object AddonList : Route

    /** Paste a manifest URL, preview what it offers, confirm installing it. */
    data object AddAddon : Route

    data class AddonDetail(val addonId: String) : Route

    /**
     * Reached by pressing Play. Carries no payload of its own - the request
     * behind it travels through [tv.mango.app.player.PendingPlayback], since
     * a [tv.mango.app.models.MediaItem] is not something a route can carry.
     */
    data object StreamPicker : Route

    /**
     * Reached from a card's long-press menu. Carries no payload of its own -
     * the source title travels through
     * [tv.mango.app.ui.search.PendingSimilarSearch], the same way
     * [StreamPicker] carries its own request.
     */
    data object SimilarTitles : Route
}
