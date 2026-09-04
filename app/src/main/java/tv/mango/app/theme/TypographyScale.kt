package tv.mango.app.theme

import android.util.TypedValue
import android.widget.TextView
import tv.mango.app.settings.home.AccessibilityConfig
import tv.mango.app.settings.home.TextSizeOption
import tv.mango.app.settings.home.TypographyConfig

/**
 * Settings -> Home Screen -> Typography and -> Accessibility's Larger Text,
 * combined into the one multiplier every scaled text view on Home actually
 * applies.
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

    /** [TypographyConfig.textSize] combined with Accessibility's Larger Text - body and metadata copy. */
    fun textScale(typography: TypographyConfig, accessibility: AccessibilityConfig): Float =
        multiplier(typography.textSize) * largerTextBoost(accessibility)

    /** [TypographyConfig.titleSize] combined with Accessibility's Larger Text - titles and headings. */
    fun titleScale(typography: TypographyConfig, accessibility: AccessibilityConfig): Float =
        multiplier(typography.titleSize) * largerTextBoost(accessibility)

    /** [TypographyConfig.metadataSize] combined with Accessibility's Larger Text - year, rating, runtime lines. */
    fun metadataScale(typography: TypographyConfig, accessibility: AccessibilityConfig): Float =
        multiplier(typography.metadataSize) * largerTextBoost(accessibility)

    private fun largerTextBoost(accessibility: AccessibilityConfig): Float = if (accessibility.largerText) 1.15f else 1f

    /** Sets [view]'s text size to [baseSp] scaled by [scale], in sp - never relative to whatever size it already has. */
    fun apply(view: TextView, baseSp: Float, scale: Float) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSp * scale)
    }
}
