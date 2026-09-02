package tv.mango.app.ui.core

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A linear layout manager that obeys [FocusLane].
 *
 * Used horizontally for the cards within a row, and vertically for the rows
 * themselves, so both axes behave alike.
 */
class FocusLaneLayoutManager(
    context: Context,
    orientation: Int,
    private val laneOffset: Int,
) : LinearLayoutManager(context, orientation, false) {

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean,
        focusedChildVisible: Boolean,
    ): Boolean = FocusLane.scrollToLane(
        parent = parent,
        child = child,
        laneOffset = laneOffset,
        vertical = orientation == VERTICAL,
        immediate = immediate,
    )
}
