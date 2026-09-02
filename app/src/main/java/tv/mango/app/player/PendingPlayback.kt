package tv.mango.app.player

import tv.mango.app.models.Episode
import tv.mango.app.models.MediaItem

/**
 * Carries one playback request from [tv.mango.app.navigation.NavigationHost.requestPlayback]
 * to the stream picker screen it opens.
 *
 * A plain in-memory holder rather than Fragment arguments: [MediaItem] and
 * [Episode] are not Parcelable, and the screen that presses Play already
 * holds them in memory at that moment - the same way [tv.mango.app.ui.detail.DetailFragment]
 * already keeps the title it is showing in a plain field rather than a
 * Bundle. Consumed once by [take], so a stream picker reached any other way
 * never acts on a stale request. It does not survive process death, which
 * for a request this short-lived matches the risk this application already
 * accepts elsewhere.
 */
object PendingPlayback {

    data class Request(
        val item: MediaItem,
        val episode: Episode?,
        val startFromBeginning: Boolean,
    )

    private var pending: Request? = null

    fun set(request: Request) {
        pending = request
    }

    /** Returns the pending request and clears it, so it can only be read once. */
    fun take(): Request? {
        val request = pending
        pending = null
        return request
    }
}
