package tv.mango.app.ui.settings.home

import tv.mango.app.settings.home.AccentColor
import tv.mango.app.settings.home.AnimationLevel
import tv.mango.app.settings.home.BackgroundType
import tv.mango.app.settings.home.BlurLevel
import tv.mango.app.settings.home.BorderLevel
import tv.mango.app.settings.home.ContentWidth
import tv.mango.app.settings.home.CornerRadiusOption
import tv.mango.app.settings.home.FocusEffect
import tv.mango.app.settings.home.FocusVisibility
import tv.mango.app.settings.home.GlassEffectLevel
import tv.mango.app.settings.home.GlowLevel
import tv.mango.app.settings.home.HeroArtworkMode
import tv.mango.app.settings.home.HeroOverlay
import tv.mango.app.settings.home.HeroRotation
import tv.mango.app.settings.home.HeroSize
import tv.mango.app.settings.home.HeroTransition
import tv.mango.app.settings.home.HomeDensity
import tv.mango.app.settings.home.NavItemId
import tv.mango.app.settings.home.NavStyle
import tv.mango.app.settings.home.PosterSizeOption
import tv.mango.app.settings.home.RowLayoutStyle
import tv.mango.app.settings.home.RowPosterSize
import tv.mango.app.settings.home.RowSpacing
import tv.mango.app.settings.home.TextSizeOption

/**
 * What a Home Screen settings row calls each option.
 *
 * Plain Kotlin strings rather than `strings.xml` entries: this application
 * ships one locale (`resourceConfigurations += listOf("en")`, see
 * `app/build.gradle.kts`), and a resource per enum constant here would be
 * several dozen indirections that only ever resolve to the exact word already
 * sitting next to them in the `when`. Every screen title and row *label*
 * still comes from `strings.xml`, same as the rest of the application - only
 * this closed, code-defined set of option names does not.
 */

fun HomeDensity.displayName(): String = when (this) {
    HomeDensity.COMPACT -> "Compact"
    HomeDensity.BALANCED -> "Balanced"
    HomeDensity.SPACIOUS -> "Spacious"
}

fun ContentWidth.displayName(): String = when (this) {
    ContentWidth.STANDARD -> "Standard"
    ContentWidth.WIDE -> "Wide"
    ContentWidth.MAXIMUM -> "Maximum"
}

fun PosterSizeOption.displayName(): String = when (this) {
    PosterSizeOption.SMALL -> "Small"
    PosterSizeOption.MEDIUM -> "Medium"
    PosterSizeOption.LARGE -> "Large"
    PosterSizeOption.CUSTOM -> "Custom"
}

fun RowLayoutStyle.displayName(): String = when (this) {
    RowLayoutStyle.STANDARD -> "Standard Posters"
    RowLayoutStyle.LARGE_POSTERS -> "Large Posters"
    RowLayoutStyle.COMPACT_POSTERS -> "Compact Posters"
    RowLayoutStyle.LANDSCAPE -> "Landscape Cards"
}

fun RowPosterSize.displayName(): String = when (this) {
    RowPosterSize.SMALL -> "Small"
    RowPosterSize.MEDIUM -> "Medium"
    RowPosterSize.LARGE -> "Large"
}

fun RowSpacing.displayName(): String = when (this) {
    RowSpacing.COMPACT -> "Compact"
    RowSpacing.NORMAL -> "Normal"
    RowSpacing.WIDE -> "Wide"
}

fun CornerRadiusOption.displayName(): String = when (this) {
    CornerRadiusOption.SMALL -> "Small"
    CornerRadiusOption.MEDIUM -> "Medium"
    CornerRadiusOption.LARGE -> "Large"
    CornerRadiusOption.EXTRA_LARGE -> "Extra Large"
}

fun FocusEffect.displayName(): String = when (this) {
    FocusEffect.NONE -> "None"
    FocusEffect.SCALE -> "Scale"
    FocusEffect.GLOW -> "Glow"
    FocusEffect.GLASS_GLOW -> "Glass Glow"
    FocusEffect.SCALE_GLOW -> "Scale + Glow"
}

fun AccentColor.displayName(): String = when (this) {
    AccentColor.DEFAULT -> "Default"
    AccentColor.BLUE -> "Blue"
    AccentColor.PURPLE -> "Purple"
    AccentColor.RED -> "Red"
    AccentColor.GREEN -> "Green"
    AccentColor.ORANGE -> "Orange"
    AccentColor.PINK -> "Pink"
    AccentColor.CYAN -> "Cyan"
    AccentColor.MONOCHROME -> "Monochrome"
    AccentColor.CUSTOM -> "Custom"
}

fun GlassEffectLevel.displayName(): String = when (this) {
    GlassEffectLevel.OFF -> "Off"
    GlassEffectLevel.LOW -> "Low"
    GlassEffectLevel.MEDIUM -> "Medium"
    GlassEffectLevel.HIGH -> "High"
}

fun BlurLevel.displayName(): String = when (this) {
    BlurLevel.LOW -> "Low"
    BlurLevel.MEDIUM -> "Medium"
    BlurLevel.HIGH -> "High"
}

fun BorderLevel.displayName(): String = when (this) {
    BorderLevel.OFF -> "Off"
    BorderLevel.SUBTLE -> "Subtle"
    BorderLevel.STRONG -> "Strong"
}

fun GlowLevel.displayName(): String = when (this) {
    GlowLevel.OFF -> "Off"
    GlowLevel.SUBTLE -> "Subtle"
    GlowLevel.STRONG -> "Strong"
}

fun HeroSize.displayName(): String = when (this) {
    HeroSize.COMPACT -> "Compact"
    HeroSize.NORMAL -> "Normal"
    HeroSize.LARGE -> "Large"
}

fun HeroArtworkMode.displayName(): String = when (this) {
    HeroArtworkMode.DYNAMIC -> "Dynamic"
    HeroArtworkMode.STATIC -> "Static"
    HeroArtworkMode.BACKDROP_ONLY -> "Backdrop Only"
}

fun HeroOverlay.displayName(): String = when (this) {
    HeroOverlay.LIGHT -> "Light"
    HeroOverlay.MEDIUM -> "Medium"
    HeroOverlay.STRONG -> "Strong"
}

fun HeroRotation.displayName(): String = when (this) {
    HeroRotation.OFF -> "Off"
    HeroRotation.SEC_10 -> "10 seconds"
    HeroRotation.SEC_15 -> "15 seconds"
    HeroRotation.SEC_20 -> "20 seconds"
    HeroRotation.SEC_30 -> "30 seconds"
}

fun HeroTransition.displayName(): String = when (this) {
    HeroTransition.FADE -> "Fade"
    HeroTransition.CROSSFADE -> "Crossfade"
    HeroTransition.SLIDE -> "Slide"
    HeroTransition.NONE -> "None"
}

fun BackgroundType.displayName(): String = when (this) {
    BackgroundType.SOLID -> "Solid"
    BackgroundType.GRADIENT -> "Gradient"
    BackgroundType.CINEMATIC -> "Cinematic"
    BackgroundType.DYNAMIC_ARTWORK -> "Dynamic Artwork"
    BackgroundType.BLURRED_ARTWORK -> "Blurred Artwork"
}

fun NavStyle.displayName(): String = when (this) {
    NavStyle.EXPANDED -> "Expanded"
    NavStyle.COLLAPSED -> "Collapsed"
    NavStyle.AUTO -> "Auto"
}

fun NavItemId.displayName(): String = when (this) {
    NavItemId.HOME -> "Home"
    NavItemId.MOVIES -> "Movies"
    NavItemId.SERIES -> "TV Shows"
    NavItemId.SEARCH -> "Search"
    NavItemId.LIBRARY -> "Watchlist"
    NavItemId.SETTINGS -> "Settings"
}

fun TextSizeOption.displayName(): String = when (this) {
    TextSizeOption.SMALL -> "Small"
    TextSizeOption.NORMAL -> "Normal"
    TextSizeOption.LARGE -> "Large"
}

fun AnimationLevel.displayName(): String = when (this) {
    AnimationLevel.FULL -> "Full"
    AnimationLevel.REDUCED -> "Reduced"
    AnimationLevel.OFF -> "Off"
}

fun FocusVisibility.displayName(): String = when (this) {
    FocusVisibility.SUBTLE -> "Subtle"
    FocusVisibility.NORMAL -> "Normal"
    FocusVisibility.STRONG -> "Strong"
}
