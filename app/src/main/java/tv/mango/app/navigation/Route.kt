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
}
