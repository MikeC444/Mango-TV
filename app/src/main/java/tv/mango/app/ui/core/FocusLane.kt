package tv.mango.app.ui.core

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * The rule that holds focus still and moves the content past it.
 *
 * The default behaviour scrolls the minimum distance needed to bring a newly
 * focused item into view, so the selection wanders around the screen and the
 * list only moves once the selection reaches an edge. That reads as a web page.
 *
 * Here the focused item always settles into the same lane and the list slides
 * beneath it, which reads as a physical surface being moved. Because the lane
 * sits at the same distance as the list's leading padding, the first item is
 * already in it and nothing scrolls until the viewer moves along.
 *
 * Extracted so the rows, the vertical stack of rows and the browse grid all
 * obey it, despite sitting on two different layout managers.
 */
object FocusLane {

    /**
     * @return true when a scroll was scheduled, matching the contract of
     *   [RecyclerView.LayoutManager.requestChildRectangleOnScreen].
     */
    fun scrollToLane(
        parent: RecyclerView,
        child: View,
        laneOffset: Int,
        vertical: Boolean,
        immediate: Boolean,
    ): Boolean {
        // Scaling a focused card is a transform and does not move its layout
        // bounds, so the child's own edge is the honest position to align.
        val delta = if (vertical) child.top - laneOffset else child.left - laneOffset
        if (delta == 0) return false

        val dx = if (vertical) 0 else delta
        val dy = if (vertical) delta else 0

        if (immediate) {
            // Restoring focus on return to this list: land, do not travel.
            parent.scrollBy(dx, dy)
        } else {
            parent.smoothScrollBy(dx, dy, MotionSpec.standard, MotionSpec.DURATION_EMPHASIZED.toInt())
        }
        return true
    }
}
