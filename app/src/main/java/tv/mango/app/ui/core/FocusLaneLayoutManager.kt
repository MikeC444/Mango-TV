package tv.mango.app.ui.core

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A layout manager that holds focus still and moves the content past it.
 *
 * The default behaviour scrolls the minimum distance needed to bring a newly
 * focused item into view, so the selection wanders around the screen and the
 * list only moves once the selection reaches an edge. That reads as a web page.
 *
 * Here the focused item always settles into the same lane and the list slides
 * beneath it, which reads as a physical carousel being turned. Because the lane
 * sits at the same distance as the list's leading padding, the first item is
 * already in it and nothing scrolls until the viewer moves along the list.
 *
 * Used horizontally for the cards within a row, and vertically for the rows
 * themselves, so both axes obey the same rule.
 */
class FocusLaneLayoutManager(
    context: Context,
    orientation: Int,
    /** Distance from the list's leading edge at which the focused item rests. */
    private val laneOffset: Int,
) : LinearLayoutManager(context, orientation, false) {

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean,
        focusedChildVisible: Boolean,
    ): Boolean {
        val horizontal = orientation == HORIZONTAL

        // Scaling a focused card is a transform and does not move its layout
        // bounds, so the child's own edge is the honest position to align.
        val delta = if (horizontal) child.left - laneOffset else child.top - laneOffset
        if (delta == 0) return false

        val dx = if (horizontal) delta else 0
        val dy = if (horizontal) 0 else delta

        if (immediate) {
            // Restoring focus on return to this list: land, do not travel.
            parent.scrollBy(dx, dy)
        } else {
            parent.smoothScrollBy(dx, dy, MotionSpec.standard, MotionSpec.DURATION_EMPHASIZED.toInt())
        }
        return true
    }
}
