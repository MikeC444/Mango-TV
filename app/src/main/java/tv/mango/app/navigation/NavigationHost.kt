package tv.mango.app.navigation

import tv.mango.app.models.MediaItem

/**
 * What a screen needs from whatever is hosting it.
 *
 * Screens depend on this rather than on MainActivity, so no screen assumes
 * which activity is showing it, and a screen can be exercised in isolation.
 */
interface NavigationHost {
    fun openDetail(item: MediaItem)
}
