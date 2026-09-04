package tv.mango.app.settings.home

import kotlinx.serialization.Serializable

/**
 * Everything a viewer can personalise about the Home screen, gathered into one
 * value.
 *
 * This is the single source of truth the rest of the application reads from:
 * [tv.mango.app.theme.RuntimeTheme] holds the current instance, every Home
 * Screen settings screen edits it through
 * [tv.mango.app.repository.HomeScreenConfigRepository], and the screens that
 * actually render the Home experience - the hero, the rows, the cards, the
 * navigation rail - read it back from there. Nothing about a viewer's library,
 * watch history or installed add-ons lives here; this is presentation only,
 * which is what lets a preset or a reset touch it without touching any of that.
 */
@Serializable
data class HomeScreenConfig(
    val layout: HomeLayoutConfig = HomeLayoutConfig(),
    val rows: RowsConfig = RowsConfig(),
    val cards: CardConfig = CardConfig(),
    val colors: ColorsConfig = ColorsConfig(),
    val glass: GlassConfig = GlassConfig(),
    val hero: HeroConfig = HeroConfig(),
    val background: BackgroundConfig = BackgroundConfig(),
    val navigation: NavigationConfig = NavigationConfig(),
    val typography: TypographyConfig = TypographyConfig(),
    val accessibility: AccessibilityConfig = AccessibilityConfig(),
    val preset: HomePreset = HomePreset.DEFAULT,
) {
    companion object {
        fun default(): HomeScreenConfig = HomeScreenConfig()
    }
}

// --------------------------------------------------------------------- layout

@Serializable
enum class HomeDensity { COMPACT, BALANCED, SPACIOUS }

@Serializable
enum class ContentWidth { STANDARD, WIDE, MAXIMUM }

/** The default poster size every row uses unless it overrides its own. */
@Serializable
enum class PosterSizeOption { SMALL, MEDIUM, LARGE, CUSTOM }

@Serializable
data class HomeLayoutConfig(
    val density: HomeDensity = HomeDensity.BALANCED,
    val contentWidth: ContentWidth = ContentWidth.STANDARD,
    val posterSize: PosterSizeOption = PosterSizeOption.MEDIUM,
    /** Only read when [posterSize] is [PosterSizeOption.CUSTOM]. */
    val customPosterWidthDp: Int = 130,
)

// ----------------------------------------------------------------------- rows

@Serializable
enum class RowLayoutStyle { STANDARD, LARGE_POSTERS, COMPACT_POSTERS, LANDSCAPE }

@Serializable
enum class RowPosterSize { SMALL, MEDIUM, LARGE }

@Serializable
enum class RowSpacing { COMPACT, NORMAL, WIDE }

/**
 * One row's own customisation, keyed elsewhere by the row's own id - the same
 * id [tv.mango.app.models.ContentRow.id] carries, so a row from the bundled
 * catalogue or from an add-on's catalogue keeps its customisation across a
 * session without this file needing to know where rows come from.
 */
@Serializable
data class RowConfig(
    val visible: Boolean = true,
    val layoutStyle: RowLayoutStyle = RowLayoutStyle.STANDARD,
    /** Null inherits [HomeLayoutConfig.posterSize]. */
    val posterSize: RowPosterSize? = null,
    val showTitle: Boolean = false,
    val showYear: Boolean = false,
    val showRating: Boolean = false,
    val showRuntime: Boolean = false,
    val showProgressBar: Boolean = true,
    val showWatchedIndicator: Boolean = true,
    val spacing: RowSpacing = RowSpacing.NORMAL,
    val itemsDisplayed: Int = 15,
    /** A viewer's own name for the row. The row's identity and its catalogue never change. */
    val customTitle: String? = null,
)

@Serializable
data class RowsConfig(
    /**
     * Explicit viewer ordering, by row id. Rows not listed here - a row from
     * an add-on installed after the order was last touched, say - are appended
     * after these, in whatever order the catalogue itself supplied them.
     */
    val order: List<String> = emptyList(),
    /** Per-row customisation, by row id. A row with no entry uses [RowConfig]'s defaults. */
    val rows: Map<String, RowConfig> = emptyMap(),
) {
    fun configFor(rowId: String): RowConfig = rows[rowId] ?: RowConfig()
}

// ---------------------------------------------------------------------- cards

@Serializable
enum class CornerRadiusOption { SMALL, MEDIUM, LARGE, EXTRA_LARGE }

@Serializable
enum class FocusEffect { NONE, SCALE, GLOW, GLASS_GLOW, SCALE_GLOW }

@Serializable
data class CardConfig(
    val cornerRadius: CornerRadiusOption = CornerRadiusOption.MEDIUM,
    val focusEffect: FocusEffect = FocusEffect.SCALE_GLOW,
    /** Multiplier applied on top of resting scale. 1.0 is no lift at all. */
    val focusScale: Float = 1.08f,
    val showTitle: Boolean = false,
    val showYear: Boolean = false,
    val showRating: Boolean = false,
    val showRuntime: Boolean = false,
    val showWatchedStatus: Boolean = true,
)

// -------------------------------------------------------------------- colours

@Serializable
enum class AccentColor { DEFAULT, BLUE, PURPLE, RED, GREEN, ORANGE, PINK, CYAN, MONOCHROME, CUSTOM }

/**
 * Every role a viewer can recolour, beyond the accent itself. Each is an
 * optional override - `null` means "derive from the accent and the base
 * palette", which is how changing only the accent still updates every one of
 * these without a viewer having to touch nine separate controls.
 */
@Serializable
data class ColorsConfig(
    val accent: AccentColor = AccentColor.DEFAULT,
    /** ARGB. Only read when [accent] is [AccentColor.CUSTOM]. */
    val customAccentArgb: Int? = null,
    val primaryBackgroundArgb: Int? = null,
    val secondaryBackgroundArgb: Int? = null,
    val glassTintArgb: Int? = null,
    val glassBorderArgb: Int? = null,
    val focusGlowArgb: Int? = null,
    val primaryTextArgb: Int? = null,
    val secondaryTextArgb: Int? = null,
    val buttonColorArgb: Int? = null,
    val selectedNavColorArgb: Int? = null,
)

// ---------------------------------------------------------------------- glass

@Serializable
enum class GlassEffectLevel { OFF, LOW, MEDIUM, HIGH }

@Serializable
enum class BlurLevel { LOW, MEDIUM, HIGH }

@Serializable
enum class BorderLevel { OFF, SUBTLE, STRONG }

@Serializable
enum class GlowLevel { OFF, SUBTLE, STRONG }

@Serializable
data class GlassConfig(
    val effect: GlassEffectLevel = GlassEffectLevel.MEDIUM,
    val opacity: Float = 0.55f,
    /**
     * A real-time blur is too expensive to run behind every scrolling row on
     * this hardware - see [tv.mango.app.theme.ThemeDrawables]. This instead
     * steps a cheap stand-in built from translucency and a gradient sheen.
     */
    val blur: BlurLevel = BlurLevel.MEDIUM,
    val border: BorderLevel = BorderLevel.SUBTLE,
    val glow: GlowLevel = GlowLevel.SUBTLE,
    val focusGlow: GlowLevel = GlowLevel.SUBTLE,
    val cornerRadius: CornerRadiusOption = CornerRadiusOption.LARGE,
)

// ---------------------------------------------------------------------- hero

@Serializable
enum class HeroSize { COMPACT, NORMAL, LARGE }

@Serializable
enum class HeroArtworkMode { DYNAMIC, STATIC, BACKDROP_ONLY }

@Serializable
enum class HeroOverlay { LIGHT, MEDIUM, STRONG }

@Serializable
enum class HeroRotation { OFF, SEC_10, SEC_15, SEC_20, SEC_30 }

@Serializable
enum class HeroTransition { FADE, CROSSFADE, SLIDE, NONE }

@Serializable
data class HeroConfig(
    val enabled: Boolean = true,
    val size: HeroSize = HeroSize.NORMAL,
    val artworkMode: HeroArtworkMode = HeroArtworkMode.DYNAMIC,
    val showTitle: Boolean = true,
    val showDescription: Boolean = true,
    val showMetadata: Boolean = true,
    val showPlayButton: Boolean = true,
    val showSecondaryActions: Boolean = true,
    val overlay: HeroOverlay = HeroOverlay.MEDIUM,
    val rotation: HeroRotation = HeroRotation.OFF,
    val transition: HeroTransition = HeroTransition.CROSSFADE,
)

// -------------------------------------------------------------------- background

@Serializable
enum class BackgroundType { SOLID, GRADIENT, CINEMATIC, DYNAMIC_ARTWORK, BLURRED_ARTWORK }

@Serializable
data class BackgroundConfig(
    val type: BackgroundType = BackgroundType.SOLID,
    val brightness: Float = 1f,
    val opacity: Float = 1f,
    val gradientStrength: Float = 0.6f,
    val artworkVisibility: Float = 0.35f,
    val blurStrength: Float = 0.5f,
)

// -------------------------------------------------------------------- navigation

@Serializable
enum class NavStyle { EXPANDED, COLLAPSED, AUTO }

/** The rail destinations a viewer can individually hide. Home and Settings never leave the list. */
@Serializable
enum class NavItemId { HOME, MOVIES, SERIES, SEARCH, LIBRARY, SETTINGS }

@Serializable
data class NavigationConfig(
    val style: NavStyle = NavStyle.AUTO,
    val hiddenItems: Set<NavItemId> = emptySet(),
) {
    fun isVisible(item: NavItemId): Boolean =
        item == NavItemId.HOME || item == NavItemId.SETTINGS || item !in hiddenItems
}

// -------------------------------------------------------------------- typography

@Serializable
enum class TextSizeOption { SMALL, NORMAL, LARGE }

@Serializable
data class TypographyConfig(
    val textSize: TextSizeOption = TextSizeOption.NORMAL,
    val titleSize: TextSizeOption = TextSizeOption.NORMAL,
    val metadataSize: TextSizeOption = TextSizeOption.NORMAL,
)

// -------------------------------------------------------------------- accessibility

@Serializable
enum class AnimationLevel { FULL, REDUCED, OFF }

@Serializable
enum class FocusVisibility { SUBTLE, NORMAL, STRONG }

@Serializable
data class AccessibilityConfig(
    val animation: AnimationLevel = AnimationLevel.FULL,
    val focusVisibility: FocusVisibility = FocusVisibility.NORMAL,
    val highContrast: Boolean = false,
    val largerText: Boolean = false,
)

// ------------------------------------------------------------------------ preset

@Serializable
enum class HomePreset { DEFAULT, CINEMATIC, COMPACT, MINIMAL, LIQUID_GLASS, STREAMER, CUSTOM }
