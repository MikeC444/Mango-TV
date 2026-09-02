package tv.mango.app.navigation

import tv.mango.app.models.Episode
import tv.mango.app.models.MediaItem

/**
 * What a screen needs from whatever is hosting it.
 *
 * Screens depend on this rather than on MainActivity, so no screen assumes
 * which activity is showing it, and a screen can be exercised in isolation.
 */
interface NavigationHost {

    fun openDetail(item: MediaItem)

    /**
     * Opens any other route on top of the current screen.
     *
     * [openDetail] stays as its own method because a screen holding a
     * [MediaItem] should not have to know which [Route] a film or a series
     * maps to; everything without that ambiguity - Settings' own screens
     * among them - goes through this instead of NavigationHost growing one
     * method per destination.
     */
    fun push(route: Route)

    /**
     * Every route into playback: a title's primary action, starting one over
     * from the beginning, a trailer, or an episode.
     *
     * Deliberately one entry point rather than one per button. It opens the
     * stream picker, which queries every enabled add-on for a source and lets
     * the viewer choose among them before anything plays.
     *
     * @param startFromBeginning ignore any saved position and start over.
     */
    fun requestPlayback(
        item: MediaItem,
        episode: Episode? = null,
        startFromBeginning: Boolean = false,
    )

    /**
     * Hands focus to the navigation rail.
     *
     * For a screen that has nothing to focus. Without it such a screen depends
     * on focus happening to still be wherever the viewer left it, and if it is
     * not, the remote does nothing at all - a screen the viewer cannot leave.
     */
    fun focusNavigation()
}
