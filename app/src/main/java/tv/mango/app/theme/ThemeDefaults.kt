package tv.mango.app.theme

/**
 * Fixed appearance values every shared view class (`TvCardView`, `NavRail`,
 * `MangoButton`, the glass surfaces built by [ThemeDrawables]...) reads,
 * instead of a compiled resource - the same reason [MangoColors] exists as a
 * data class rather than a set of `@color` references.
 *
 * These were previously user-configurable through a Settings -> Home Screen
 * appearance editor. That editor (and the whole Home Screen it customised)
 * has been removed in favour of a WebView-hosted home screen; the shared
 * card/glass/typography/accessibility rendering it also drove is kept here as
 * fixed defaults so Browse, Detail and Settings keep their existing look
 * without needing a live configuration source behind it.
 */
object ThemeDefaults {
    val colors: MangoColors = MangoColors.defaults()
    val cards: CardConfig = CardConfig()
    val glass: GlassConfig = GlassConfig()
    val typography: TypographyConfig = TypographyConfig()
    val accessibility: AccessibilityConfig = AccessibilityConfig()
}

// ---------------------------------------------------------------------- cards

enum class CornerRadiusOption { SMALL, MEDIUM, LARGE, EXTRA_LARGE }

enum class FocusEffect { NONE, SCALE, GLOW, GLASS_GLOW, SCALE_GLOW }

data class CardConfig(
    val cornerRadius: CornerRadiusOption = CornerRadiusOption.MEDIUM,
    val focusEffect: FocusEffect = FocusEffect.SCALE_GLOW,
    /** Multiplier applied on top of resting scale. 1.0 is no lift at all. */
    val focusScale: Float = 1.08f,
)

// ---------------------------------------------------------------------- glass

enum class GlassEffectLevel { OFF, LOW, MEDIUM, HIGH }

enum class BlurLevel { LOW, MEDIUM, HIGH }

enum class BorderLevel { OFF, SUBTLE, STRONG }

enum class GlowLevel { OFF, SUBTLE, STRONG }

data class GlassConfig(
    val effect: GlassEffectLevel = GlassEffectLevel.MEDIUM,
    val opacity: Float = 0.55f,
    /**
     * A real-time blur is too expensive to run behind every scrolling row on
     * this hardware - see [ThemeDrawables]. This instead steps a cheap
     * stand-in built from translucency and a gradient sheen.
     */
    val blur: BlurLevel = BlurLevel.MEDIUM,
    val border: BorderLevel = BorderLevel.SUBTLE,
    val glow: GlowLevel = GlowLevel.SUBTLE,
    val cornerRadius: CornerRadiusOption = CornerRadiusOption.LARGE,
)

// -------------------------------------------------------------------- typography

enum class TextSizeOption { SMALL, NORMAL, LARGE }

data class TypographyConfig(
    val titleSize: TextSizeOption = TextSizeOption.NORMAL,
    val metadataSize: TextSizeOption = TextSizeOption.NORMAL,
)

// -------------------------------------------------------------------- accessibility

enum class AnimationLevel { FULL, REDUCED, OFF }

enum class FocusVisibility { SUBTLE, NORMAL, STRONG }

data class AccessibilityConfig(
    val animation: AnimationLevel = AnimationLevel.FULL,
    val focusVisibility: FocusVisibility = FocusVisibility.NORMAL,
    val highContrast: Boolean = false,
    val largerText: Boolean = false,
)
