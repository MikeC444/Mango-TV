package tv.mango.app.ui.core

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import tv.mango.app.R
import tv.mango.app.theme.AnimationLevel
import tv.mango.app.theme.FocusEffect
import tv.mango.app.theme.ThemeDefaults
import tv.mango.app.theme.ThemeDrawables
import tv.mango.app.theme.ThemeDrawables.cardRadiusDp

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
 *
 * Style - corner radius, focus effect, focus scale, colours - is read from
 * [ThemeDefaults] once, at construction, rather than re-read on every bind: it
 * is fixed for the lifetime of the process, so there is nothing to pick up
 * later. What does vary per bind - a card's size and its optional caption - is applied
 * separately, by [tv.mango.app.ui.core.MediaCardAdapter], because it varies
 * per *row*, not per screen.
 */
class TvCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cardConfig = ThemeDefaults.cards
    private val accessibility = ThemeDefaults.accessibility
    private val colors = ThemeDefaults.colors

    private val cornerRadius = cardConfig.cornerRadius.cardRadiusDp() * resources.displayMetrics.density
    private val focusElevation = resources.getDimension(R.dimen.focus_elevation)

    /** Only [FocusEffect.SCALE] and [FocusEffect.SCALE_GLOW] lift the card at all. */
    private val liftsOnFocus = cardConfig.focusEffect == FocusEffect.SCALE ||
        cardConfig.focusEffect == FocusEffect.SCALE_GLOW
    private val focusScale = if (liftsOnFocus) cardConfig.focusScale else FocusElevation.SCALE_RESTING

    private val animationDuration = when (accessibility.animation) {
        AnimationLevel.OFF -> 0L
        AnimationLevel.REDUCED -> MotionSpec.DURATION_STANDARD / 2
        AnimationLevel.FULL -> MotionSpec.DURATION_STANDARD
    }

    private val focusOverlay = ThemeDrawables.cardFocusOverlay(
        colors,
        cardConfig.focusEffect,
        cornerRadius,
        accessibility.focusVisibility,
        accessibility.highContrast,
    )
        // mutate() is essential: without it every card in the application would
        // share one drawable's alpha, and focusing any card would light them all.
        .mutate()
        .also { it.alpha = 0 }

    /**
     * Held for the lifetime of the view rather than created per focus change.
     * Reads the scale the focus animator is already producing and maps it onto
     * the overlay, so the brightness lift and the physical lift are the same
     * motion by construction and cost one animator between them.
     */
    private val overlaySync = ValueAnimator.AnimatorUpdateListener {
        val drawable = focusOverlay
        val progress = if (liftsOnFocus) FocusElevation.progressOf(this, focusScale) else overlayAlphaProgress
        drawable.alpha = (progress * 255f).toInt()
        invalidate()
    }

    /** Drives the overlay's own fade when the effect carries no scale to ride ([FocusEffect.GLOW], [FocusEffect.GLASS_GLOW]). */
    private var overlayAlphaProgress = 0f

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // A shadow is never clipped by clipToOutline - the platform draws
            // it outside the view's own bounds - so tinting it is the cheapest
            // way to get an actual glow past a card's edge rather than one
            // that stops dead at it. Pre-28 falls back to the platform's
            // default dark shadow, which still carries the lift.
            outlineSpotShadowColor = colors.focusGlow
            outlineAmbientShadowColor = colors.focusGlow
        }

        foreground = focusOverlay
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (liftsOnFocus) {
            FocusElevation.animate(
                view = this,
                focused = gainFocus,
                elevationPx = focusElevation,
                scale = focusScale,
                useLayer = rasteriseWhileScaling,
                duration = animationDuration,
                updateListener = overlaySync,
            )
        } else {
            // No lift to ride: the overlay animates its own alpha directly, on
            // a plain ValueAnimator instead of a ViewPropertyAnimator.
            animateOverlayAlpha(gainFocus)
        }
    }

    private var overlayAnimator: ValueAnimator? = null

    private fun animateOverlayAlpha(focused: Boolean) {
        overlayAnimator?.cancel()
        val target = if (focused) 1f else 0f
        if (animationDuration <= 0L) {
            overlayAlphaProgress = target
            focusOverlay.alpha = (target * 255f).toInt()
            invalidate()
            return
        }
        overlayAnimator = ValueAnimator.ofFloat(overlayAlphaProgress, target).apply {
            duration = animationDuration
            interpolator = MotionSpec.standard
            addUpdateListener {
                overlayAlphaProgress = it.animatedValue as Float
                focusOverlay.alpha = (overlayAlphaProgress * 255f).toInt()
                invalidate()
            }
            start()
        }
    }

    /** Applies this row's card size. Cheap: only ever a `LayoutParams` write. */
    fun applySize(widthPx: Int, heightPx: Int) {
        val params = layoutParams
        if (params != null && params.width == widthPx && params.height == heightPx) return
        layoutParams = (params ?: LayoutParams(widthPx, heightPx)).apply {
            width = widthPx
            height = heightPx
        }
    }

    /** Restores the resting state when a recycled holder is rebound. */
    fun resetFocusState() {
        animate().cancel()
        overlayAnimator?.cancel()
        FocusElevation.applyImmediately(this, focused = false)
        overlayAlphaProgress = 0f
        focusOverlay.alpha = 0
        invalidate()
    }
}
