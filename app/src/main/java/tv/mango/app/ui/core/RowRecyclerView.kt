package tv.mango.app.ui.core

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R

/**
 * One horizontally scrolling row of cards.
 *
 * Carries the behaviour every row in the application needs:
 *
 *  - Clipping is off in both directions, so a focused card can grow past the
 *    row's bounds and cast its shadow instead of being sliced by them.
 *  - Item animations are disabled. A row's contents are set once and do not
 *    reorder, so the animator would only add work and a frame of latency.
 *  - The row remembers which card the viewer left on. Moving down to another
 *    row and back returns to the same card rather than resetting to the start,
 *    which is what makes vertical movement feel like moving around one surface
 *    rather than reloading a page.
 */
class RowRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private var lastFocusedPosition = NO_POSITION

    init {
        val lane = resources.getDimensionPixelSize(R.dimen.safe_area_horizontal)

        // A focused card grows beyond its bounds and lifts. Both need room to
        // draw outside the row.
        clipChildren = false
        clipToPadding = false

        setPadding(lane, 0, lane, 0)
        layoutManager = FocusLaneLayoutManager(context, HORIZONTAL, lane)

        // Rows have a fixed height, so the parent never needs to re-measure
        // when their contents change.
        setHasFixedSize(true)

        // Contents are bound once; there is nothing to animate in or out.
        itemAnimator = null

        // The row is a container, not a stop on the focus path. Cards take
        // focus; the row itself never does.
        isFocusable = false
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        child?.let { lastFocusedPosition = getChildAdapterPosition(it) }
    }

    /**
     * Sends focus back to the card the viewer left on, rather than to whichever
     * card the platform's geometric search happens to prefer.
     */
    override fun onRequestFocusInDescendants(
        direction: Int,
        previouslyFocusedRect: Rect?,
    ): Boolean {
        if (lastFocusedPosition != NO_POSITION) {
            val holder = findViewHolderForAdapterPosition(lastFocusedPosition)
            if (holder != null && holder.itemView.requestFocus()) return true
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect)
    }

    /** Clears focus memory when a row is rebound to different content. */
    fun resetFocusMemory() {
        lastFocusedPosition = NO_POSITION
    }
}
