package tv.mango.app.ui.core

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import tv.mango.app.R

/**
 * The focusable surface every piece of artwork sits on.
 *
 * Deliberately two views deep - this container and the artwork inside it. A
 * content row holds dozens of these, so every extra view in a card is an extra
 * view multiplied across the whole screen.
 *
 * The focused treatment (a brightness lift and a thin accent edge) is one
 * foreground drawable whose alpha rides the scale animation, rather than two
 * more child views and a second animator. The edge is inset by a pixel so
 * [setClipToOutline] cannot shave it.
 */
class TvCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cornerRadius = resources.getDimension(R.dimen.card_corner)
    private val focusElevation = resources.getDimension(R.dimen.focus_elevation)

    private val focusOverlay: Drawable? =
        // mutate() is essential: without it every card in the application would
        // share one drawable's alpha, and focusing any card would light them all.
        ContextCompat.getDrawable(context, R.drawable.card_focus_overlay)
            ?.mutate()
            ?.also { it.alpha = 0 }

    /**
     * Held for the lifetime of the view rather than created per focus change.
     * Reads the scale the focus animator is already producing and maps it onto
     * the overlay, so the brightness lift and the physical lift are the same
     * motion by construction and cost one animator between them.
     */
    private val overlaySync = ValueAnimator.AnimatorUpdateListener {
        val drawable = focusOverlay ?: return@AnimatorUpdateListener
        drawable.alpha = (FocusElevation.progressOf(this) * 255f).toInt()
        invalidate()
    }

    /** Set for cards that draw text, so scaling does not re-rasterise glyphs. */
    var rasteriseWhileScaling: Boolean = false

    init {
        isFocusable = true
        // A television app is never in touch mode. Leaving this false stops the
        // platform clearing focus if the app is run on a device with a screen.
        isFocusableInTouchMode = false

        // Focus stops here: the artwork inside is never independently focusable,
        // so the D-pad always lands on whole cards.
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS

        // Rounds the artwork, and gives the lift a shape to cast a shadow from.
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        clipToOutline = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The platform's default focus highlight is a flat grey rectangle.
            // Mango TV draws its own.
            defaultFocusHighlightEnabled = false
        }

        foreground = focusOverlay
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        FocusElevation.animate(
            view = this,
            focused = gainFocus,
            elevationPx = focusElevation,
            useLayer = rasteriseWhileScaling,
            updateListener = overlaySync,
        )
    }

    /** Restores the resting state when a recycled holder is rebound. */
    fun resetFocusState() {
        FocusElevation.applyImmediately(this, focused = false)
        focusOverlay?.alpha = 0
        invalidate()
    }
}
