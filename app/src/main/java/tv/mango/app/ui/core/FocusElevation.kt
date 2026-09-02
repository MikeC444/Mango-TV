package tv.mango.app.ui.core

import android.animation.ValueAnimator
import android.view.View

/**
 * The physical response of a focusable surface.
 *
 * A focused element rises toward the viewer, grows very slightly, and brightens
 * a little. The effect is meant to be felt rather than noticed - closer to a
 * key rising under a finger than to a menu item lighting up.
 *
 * Performance notes, because this is the most frequently exercised path in a
 * television interface:
 *
 *  - [android.view.ViewPropertyAnimator] animates the view's transform
 *    properties directly, with no reflection and no per-frame allocation.
 *  - Any in-flight animation is cancelled before a new one starts, so holding a
 *    direction on the remote cannot stack animators on a single view.
 *  - Scaled surfaces deliberately contain no text. Scaling a view that draws
 *    text re-rasterises glyphs every frame; scaling one that draws a bitmap is
 *    a transform the GPU applies for free. Cards that must carry text pass
 *    `useLayer`, so the view is rasterised once and scaled as a texture.
 */
object FocusElevation {

    /** Focused scale. Enough to read as a lift, not enough to read as a pop. */
    const val SCALE_FOCUSED = 1.05f
    const val SCALE_RESTING = 1f

    /**
     * Rows that do not hold focus recede rather than disappear. Applied at row
     * level - one animation per focus change instead of one per neighbour.
     */
    const val ALPHA_ROW_ACTIVE = 1f
    const val ALPHA_ROW_RECEDED = 0.65f

    /**
     * Animates [view] into or out of its focused state.
     *
     * @param elevationPx resting-to-focused translationZ, in pixels.
     * @param useLayer rasterise the view for the duration of the animation.
     *   Worth it only for views that draw text; wasteful for pure artwork.
     * @param updateListener optional per-frame hook, so a caller can drive a
     *   secondary property from the same animator rather than starting a second
     *   one. Pass a listener held by the view, not a fresh lambda.
     */
    fun animate(
        view: View,
        focused: Boolean,
        elevationPx: Float = 0f,
        scale: Float = SCALE_FOCUSED,
        useLayer: Boolean = false,
        duration: Long = MotionSpec.DURATION_STANDARD,
        updateListener: ValueAnimator.AnimatorUpdateListener? = null,
    ) {
        val target = if (focused) scale else SCALE_RESTING
        val targetZ = if (focused) elevationPx else 0f

        view.animate().cancel()
        val animator = view.animate()
            .scaleX(target)
            .scaleY(target)
            .translationZ(targetZ)
            .setDuration(duration)
            .setInterpolator(MotionSpec.standard)
            .setUpdateListener(updateListener)

        if (useLayer) animator.withLayer()
        animator.start()
    }

    /**
     * How far through the resting-to-focused transition [view] currently is.
     * Lets a secondary property ride the scale animation instead of running its
     * own clock.
     */
    fun progressOf(view: View, scale: Float = SCALE_FOCUSED): Float =
        ((view.scaleX - SCALE_RESTING) / (scale - SCALE_RESTING)).coerceIn(0f, 1f)

    /** Applies a focus state with no animation, for binding and recycling. */
    fun applyImmediately(view: View, focused: Boolean, elevationPx: Float = 0f) {
        view.animate().cancel()
        val target = if (focused) SCALE_FOCUSED else SCALE_RESTING
        view.scaleX = target
        view.scaleY = target
        view.translationZ = if (focused) elevationPx else 0f
    }
}
