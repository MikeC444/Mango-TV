package tv.mango.app.ui.core

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R

/**
 * The vertical stack of content rows.
 *
 * Owns two things that make the screen feel like one surface rather than a list
 * of lists:
 *
 *  - The shared card pool. Every row's cards come from and return to the pool
 *    held here, so moving down the screen reuses views rather than inflating
 *    a fresh set per row.
 *  - Which row is active. Rows that do not hold focus recede, and that is
 *    decided here because only this view can see all of them. Exactly two rows
 *    animate per change - the one being left and the one being entered -
 *    regardless of how many are on screen.
 */
class RowsRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    /** Adapter position of the row currently holding focus. */
    var focusedRowPosition: Int = NO_POSITION
        private set

    init {
        val lane = resources.getDimensionPixelSize(R.dimen.safe_area_vertical)

        clipChildren = false
        clipToPadding = false
        setPadding(0, lane, 0, lane)

        // Moving down brings the entered row up to a fixed line rather than
        // scrolling it just barely into view.
        layoutManager = FocusLaneLayoutManager(context, VERTICAL, lane)

        setHasFixedSize(true)
        itemAnimator = null
        isFocusable = false
        descendantFocusability = FOCUS_AFTER_DESCENDANTS

        // Rows share one pool of cards. Five is roughly a screen's worth plus
        // the row being scrolled toward.
        recycledViewPool.setMaxRecycledViews(0, CARD_POOL_SIZE)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        val position = child?.let { getChildAdapterPosition(it) } ?: NO_POSITION
        if (position == NO_POSITION || position == focusedRowPosition) return

        (findViewHolderForAdapterPosition(focusedRowPosition)?.itemView as? RowContainerView)
            ?.setReceded(receded = true)
        (child as? RowContainerView)?.setReceded(receded = false)

        focusedRowPosition = position
    }

    private companion object {
        const val CARD_POOL_SIZE = 24
    }
}
