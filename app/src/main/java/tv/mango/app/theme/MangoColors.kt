package tv.mango.app.theme

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import tv.mango.app.settings.home.ColorPalettes
import tv.mango.app.settings.home.ColorsConfig

/**
 * The palette actually drawn on screen right now, resolved once per
 * [tv.mango.app.settings.home.HomeScreenConfig] change rather than re-derived
 * by every view that needs a colour.
 *
 * Field-for-field, this mirrors `colors.xml` - it is what every shared view
 * class (`TvCardView`, `NavRail`, the glass surfaces built by
 * [ThemeDrawables]...) reads instead of a compiled resource, which is what
 * lets an accent change reach every one of them without a single `@color`
 * reference baked into a drawable anywhere in the application.
 */
data class MangoColors(
    @ColorInt val accent: Int,
    @ColorInt val accentDim: Int,
    @ColorInt val accentPressed: Int,
    @ColorInt val primaryBackground: Int,
    @ColorInt val secondaryBackground: Int,
    @ColorInt val glassTint: Int,
    @ColorInt val glassTintFocused: Int,
    @ColorInt val glassBorder: Int,
    @ColorInt val glassBorderFocused: Int,
    @ColorInt val glassSheen: Int,
    @ColorInt val focusGlow: Int,
    @ColorInt val primaryText: Int,
    @ColorInt val secondaryText: Int,
    @ColorInt val tertiaryText: Int,
    @ColorInt val buttonColor: Int,
    @ColorInt val selectedNavColor: Int,
    @ColorInt val textOnAccent: Int,
) {
    companion object {

        /** The look of the application before a viewer has ever touched a colour control. */
        fun defaults(): MangoColors = resolve(ColorsConfig())

        fun resolve(colors: ColorsConfig): MangoColors {
            val accent = colors.customAccentArgb
                ?: ColorPalettes.accentArgb(colors.accent)
                ?: DEFAULT_ACCENT

            val primaryText = colors.primaryTextArgb ?: DEFAULT_TEXT_PRIMARY
            val secondaryText = colors.secondaryTextArgb ?: DEFAULT_TEXT_SECONDARY
            val primaryBackground = colors.primaryBackgroundArgb ?: DEFAULT_SURFACE_BASE
            val secondaryBackground = colors.secondaryBackgroundArgb ?: DEFAULT_SURFACE_RAISED
            val glassTint = colors.glassTintArgb ?: DEFAULT_GLASS_FILL
            val glassBorder = colors.glassBorderArgb ?: withAlpha(accent, GLASS_BORDER_ACCENT_ALPHA)
            val focusGlow = colors.focusGlowArgb ?: accent
            val buttonColor = colors.buttonColorArgb ?: accent
            val selectedNavColor = colors.selectedNavColorArgb ?: accent

            return MangoColors(
                accent = accent,
                accentDim = withAlpha(accent, ACCENT_DIM_ALPHA),
                accentPressed = ColorUtils.blendARGB(accent, 0xFF000000.toInt(), PRESSED_DARKEN_FRACTION),
                primaryBackground = primaryBackground,
                secondaryBackground = secondaryBackground,
                glassTint = glassTint,
                glassTintFocused = ColorUtils.setAlphaComponent(secondaryBackground, GLASS_FILL_FOCUSED_ALPHA),
                glassBorder = glassBorder,
                glassBorderFocused = withAlpha(accent, GLASS_BORDER_FOCUSED_ALPHA),
                glassSheen = DEFAULT_GLASS_SHEEN,
                focusGlow = focusGlow,
                primaryText = primaryText,
                secondaryText = secondaryText,
                tertiaryText = DEFAULT_TEXT_TERTIARY,
                buttonColor = buttonColor,
                selectedNavColor = selectedNavColor,
                textOnAccent = textOn(buttonColor),
            )
        }

        /** Black or white, whichever reads legibly on [background] - used for text sitting on an arbitrary accent. */
        @ColorInt
        fun textOn(@ColorInt background: Int): Int =
            if (ColorUtils.calculateLuminance(background) > LUMINANCE_THRESHOLD) DARK_TEXT else LIGHT_TEXT

        @ColorInt
        private fun withAlpha(@ColorInt color: Int, alpha: Int): Int = ColorUtils.setAlphaComponent(color, alpha)

        // `toInt()` calls below are not compile-time constants, so these are
        // plain `val`s rather than `const val`s - each is still computed once,
        // the first time the companion object is touched.
        private val DEFAULT_ACCENT = 0xFFD9A05B.toInt()
        private val DEFAULT_SURFACE_BASE = 0xFF0B0B0C.toInt()
        private val DEFAULT_SURFACE_RAISED = 0xFF141416.toInt()
        private val DEFAULT_GLASS_FILL = 0x4D141416
        private val DEFAULT_GLASS_SHEEN = 0x14FFFFFF
        private val DEFAULT_TEXT_PRIMARY = 0xFFF2EFE9.toInt()
        private val DEFAULT_TEXT_SECONDARY = 0xFF9C9A96.toInt()
        private val DEFAULT_TEXT_TERTIARY = 0xFF7E7E83.toInt()

        private val DARK_TEXT = 0xFF120D06.toInt()
        private val LIGHT_TEXT = 0xFFF2EFE9.toInt()
        private const val LUMINANCE_THRESHOLD = 0.42

        private const val ACCENT_DIM_ALPHA = 0x66
        private const val GLASS_FILL_FOCUSED_ALPHA = 0x6B
        private const val GLASS_BORDER_ACCENT_ALPHA = 0x26
        private const val GLASS_BORDER_FOCUSED_ALPHA = 0x40
        private const val PRESSED_DARKEN_FRACTION = 0.12f
    }
}
