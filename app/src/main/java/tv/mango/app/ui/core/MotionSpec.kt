package tv.mango.app.ui.core

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator

/**
 * The application's motion vocabulary.
 *
 * Every animation in Mango TV draws its duration and curve from here, so the
 * whole interface moves with one hand. Interpolators are allocated once and
 * shared: they are stateless, and a new [PathInterpolator] per focus change
 * would allocate on the hottest path in the app.
 *
 * Motion is used to explain a change, never to decorate one. Between changes
 * the interface is completely still.
 */
object MotionSpec {

    /** Small state flips: a scrim, a label, an indicator. */
    const val DURATION_FAST = 150L

    /** The default. Focus changes, dimming, rail expansion. */
    const val DURATION_STANDARD = 200L

    /** Movements that cover distance: row scrolling, screen changes. */
    const val DURATION_EMPHASIZED = 280L

    /** Crossfades of large imagery, where a slower settle reads as deliberate. */
    const val DURATION_IMAGE = 320L

    /**
     * Decelerate. Leaves quickly, arrives softly - the curve of something with
     * mass coming to rest, rather than a spring settling.
     */
    val standard: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

    /** Accelerate then decelerate, for movement that starts and ends on screen. */
    val emphasized: Interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    /** Exits: accelerate away without a soft landing. */
    val exit: Interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)

    val linear: Interpolator = LinearInterpolator()
}
