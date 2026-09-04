package tv.mango.app.ui.core

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * The header-plus-cards unit that makes up one row, and the surface that
 * recedes when the row does not hold focus.
 *
 * The override matters more than it looks. Setting alpha below 1 on a ViewGroup
 * normally makes the platform render the whole subtree into an off-screen layer
 * first, so the group can be composited as a unit - an allocation and an extra
 * render pass for every row on screen, on hardware that has neither to spare.
 *
 * A row's header and cards never overlap each other, so per-child alpha gives
 * an identical result. Declaring that here lets the platform skip the layer
 * entirely and blend each child as it draws.
 */
class RowContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        // A focused card in this row draws beyond the row's bounds.
        clipChildren = false
        clipToPadding = false
    }

    override fun hasOverlappingRendering(): Boolean = false

    /** Applies Settings -> Home Screen -> Home Layout's Home Density as the gap below this row. */
    fun setRowSpacing(bottomPaddingPx: Int) {
        if (paddingBottom == bottomPaddingPx) return
        setPadding(paddingLeft, paddingTop, paddingRight, bottomPaddingPx)
    }

    /** Animates the row between active and receded. */
    fun setReceded(receded: Boolean, animate: Boolean = true) {
        val target = if (receded) {
            FocusElevation.ALPHA_ROW_RECEDED
        } else {
            FocusElevation.ALPHA_ROW_ACTIVE
        }
        if (!animate) {
            animate().cancel()
            alpha = target
            return
        }
        if (alpha == target) return
        animate().cancel()
        animate()
            .alpha(target)
            .setDuration(MotionSpec.DURATION_STANDARD)
            .setInterpolator(MotionSpec.standard)
            .start()
    }
}
