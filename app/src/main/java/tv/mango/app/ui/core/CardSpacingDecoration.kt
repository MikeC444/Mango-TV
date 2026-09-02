package tv.mango.app.ui.core

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * The gaps between cards.
 *
 * Held here rather than as a margin on the card layout so that one card layout
 * can serve both a row and a grid. A margin baked into the card would be
 * correct in a row and wrong in a grid, where the leading and trailing columns
 * need different treatment from the ones between.
 */
sealed class CardSpacingDecoration : RecyclerView.ItemDecoration() {

    /** Even gaps along a horizontal row; nothing after the last card. */
    class Row(private val gap: Int) : CardSpacingDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            val position = parent.getChildAdapterPosition(view)
            val last = state.itemCount - 1
            outRect.right = if (position == last) 0 else gap
        }
    }

    /**
     * Even gaps in a grid.
     *
     * Offsets are distributed across the columns so every card ends up the same
     * width and the rows stay flush at both edges - splitting the gap evenly
     * per item instead would leave the outer columns narrower than the inner
     * ones and the grid visibly out of true.
     */
    class Grid(private val gap: Int) : CardSpacingDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            val manager = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = manager.spanCount
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return

            val column = position % spanCount
            outRect.left = column * gap / spanCount
            outRect.right = gap - (column + 1) * gap / spanCount
            if (position >= spanCount) outRect.top = gap
        }
    }
}
