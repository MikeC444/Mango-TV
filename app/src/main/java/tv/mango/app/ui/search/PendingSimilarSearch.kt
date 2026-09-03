package tv.mango.app.ui.search

import tv.mango.app.models.MediaItem

/**
 * Carries one "Find Similar" request from a long-press menu to the search
 * screen it opens.
 *
 * The same shape as [tv.mango.app.player.PendingPlayback] and for the same
 * reason: a [MediaItem] is not something a [tv.mango.app.navigation.Route]
 * can carry, and the screen offering the action already holds it in memory.
 * Consumed once by [take], so the search screen reached any other way never
 * acts on a stale request.
 */
object PendingSimilarSearch {

    private var pending: MediaItem? = null

    fun set(item: MediaItem) {
        pending = item
    }

    /** Returns the pending request and clears it, so it can only be read once. */
    fun take(): MediaItem? {
        val item = pending
        pending = null
        return item
    }
}
