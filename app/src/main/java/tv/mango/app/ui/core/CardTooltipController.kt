package tv.mango.app.ui.core

import android.view.View
import android.view.ViewTreeObserver
import tv.mango.app.R
import tv.mango.app.models.MediaItem

/**
 * Drives a [CardTooltipView] from wherever focus actually is, for a whole
 * screen at once.
 *
 * Deliberately not wired through each row or grid's own focus callback.
 * [MediaCardAdapter] already reports focus for its own reasons - following the
 * hero, remembering a row's position - and duplicating that plumbing here for
 * every screen that wants a tooltip would mean every future screen needing it
 * remembers to wire a second callback through by hand. A global focus listener
 * sees every card on the screen the moment it is focused, reads the
 * [MediaItem] [MediaCardAdapter] already tags it with, and shows or hides
 * accordingly - so adding the tooltip to a new screen is exactly [attach] and
 * [detach], nothing else.
 *
 * Debounced the same way the home screen's hero already is: holding a
 * direction on the remote crosses a dozen cards a second, and showing a panel
 * for each of them only to immediately replace it is work, and flicker, for
 * nothing.
 */
class CardTooltipController(
    private val tooltip: CardTooltipView,
    private val root: View,
) {

    private var pendingShow: Runnable? = null

    private val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
        cancelPending()
        val anchor = newFocus
        val item = anchor?.getTag(R.id.card_media_item) as? MediaItem
        if (anchor == null || item == null) {
            tooltip.hide()
            return@OnGlobalFocusChangeListener
        }
        val show = Runnable { tooltip.show(item, anchor) }
        pendingShow = show
        root.postDelayed(show, DEBOUNCE_MS)
    }

    fun attach() {
        root.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
    }

    fun detach() {
        cancelPending()
        root.viewTreeObserver.removeOnGlobalFocusChangeListener(focusListener)
        tooltip.hide()
    }

    private fun cancelPending() {
        pendingShow?.let(root::removeCallbacks)
        pendingShow = null
    }

    private companion object {
        /** Matches the home screen hero's own debounce for the same reason. */
        const val DEBOUNCE_MS = 220L
    }
}
