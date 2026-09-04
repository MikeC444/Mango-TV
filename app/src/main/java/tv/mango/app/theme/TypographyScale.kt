package tv.mango.app.theme

import android.util.TypedValue
import android.widget.TextView

/**
 * A card caption's title/metadata text size, combined with Accessibility's
 * Larger Text into the one multiplier a caption actually applies.
 *
 * Sizes are always set from a fixed base sp value, never from whatever a
 * `TextView` currently measures - reading back an already-scaled size and
 * scaling it again is how repeated binds would compound into runaway text.
 */
object TypographyScale {

    private fun multiplier(size: TextSizeOption): Float = when (size) {
        TextSizeOption.SMALL -> 0.85f
        TextSizeOption.NORMAL -> 1f
        TextSizeOption.LARGE -> 1.2f
    }

    fun titleScale(typography: TypographyConfig, accessibility: AccessibilityConfig): Float =
        multiplier(typography.titleSize) * largerTextBoost(accessibility)

    fun metadataScale(typography: TypographyConfig, accessibility: AccessibilityConfig): Float =
        multiplier(typography.metadataSize) * largerTextBoost(accessibility)

    private fun largerTextBoost(accessibility: AccessibilityConfig): Float = if (accessibility.largerText) 1.15f else 1f

    /** Sets [view]'s text size to [baseSp] scaled by [scale], in sp - never relative to whatever size it already has. */
    fun apply(view: TextView, baseSp: Float, scale: Float) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp * scale)
    }
}
