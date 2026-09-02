package tv.mango.app.ui.core

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * The grid behind the browse screens.
 *
 * Two departures from a plain [GridLayoutManager]:
 *
 *  - The column count is derived from the width actually available rather than
 *    fixed. A television is not one size: the same layout has to sit sensibly
 *    on a 1080p panel and a 4K one, and hard-coding a count would leave one of
 *    them either cramped or half empty.
 *  - The focused row settles at the same line as everywhere else in the
 *    application, via [FocusLane], instead of scrolling just barely into view.
 */
class BrowseGridLayoutManager(
    context: Context,
    private val itemWidthPx: Int,
    private val laneOffset: Int,
) : GridLayoutManager(context, 1) {

    private var lastMeasuredWidth = 0

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int,
    ) {
        val available = View.MeasureSpec.getSize(widthSpec) - (paddingStart + paddingEnd)
        if (available > 0 && available != lastMeasuredWidth) {
            lastMeasuredWidth = available
            spanCount = (available / itemWidthPx).coerceAtLeast(MIN_COLUMNS)
        }
        super.onMeasure(recycler, state, widthSpec, heightSpec)
    }

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
        vertical = true,
        immediate = immediate,
    )

    private companion object {
        /** Below this a grid stops reading as a grid. */
        const val MIN_COLUMNS = 3
    }
}
