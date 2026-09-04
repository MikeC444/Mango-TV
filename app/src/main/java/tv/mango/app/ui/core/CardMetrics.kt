package tv.mango.app.ui.core

import tv.mango.app.settings.home.HomeDensity
import tv.mango.app.settings.home.HomeLayoutConfig
import tv.mango.app.settings.home.PosterSizeOption
import tv.mango.app.settings.home.RowConfig
import tv.mango.app.settings.home.RowLayoutStyle
import tv.mango.app.settings.home.RowPosterSize
import tv.mango.app.settings.home.RowSpacing

/**
 * How large one row's cards are, and what shape.
 *
 * A row's own [RowConfig.posterSize] and [RowConfig.layoutStyle] override the
 * screen-wide [HomeLayoutConfig] where a viewer has set one; everything else
 * inherits the screen-wide default. Sizes are expressed in dp - the caller
 * converts to pixels once, against its own [android.content.res.Resources].
 */
object CardMetrics {

    private const val PORTRAIT_BASE_WIDTH_DP = 130f
    private const val PORTRAIT_BASE_HEIGHT_DP = 195f
    private const val LANDSCAPE_BASE_WIDTH_DP = 260f
    private const val LANDSCAPE_BASE_HEIGHT_DP = 146f

    /** Whether this row's cards should show landscape backdrops rather than portrait posters. */
    fun usesBackdropArt(layoutStyle: RowLayoutStyle): Boolean = layoutStyle == RowLayoutStyle.LANDSCAPE

    /** The card size for one row, in dp, as (width, height). */
    fun sizeDp(layout: HomeLayoutConfig, row: RowConfig): Pair<Float, Float> {
        val style = row.layoutStyle

        if (layout.posterSize == PosterSizeOption.CUSTOM && row.posterSize == null) {
            val width = layout.customPosterWidthDp.toFloat()
            val aspect = if (usesBackdropArt(style)) {
                LANDSCAPE_BASE_HEIGHT_DP / LANDSCAPE_BASE_WIDTH_DP
            } else {
                PORTRAIT_BASE_HEIGHT_DP / PORTRAIT_BASE_WIDTH_DP
            }
            return width to (width * aspect)
        }

        val (baseWidth, baseHeight) = if (usesBackdropArt(style)) {
            LANDSCAPE_BASE_WIDTH_DP to LANDSCAPE_BASE_HEIGHT_DP
        } else {
            PORTRAIT_BASE_WIDTH_DP to PORTRAIT_BASE_HEIGHT_DP
        }

        val styleMultiplier = when (style) {
            RowLayoutStyle.LARGE_POSTERS -> 1.3f
            RowLayoutStyle.COMPACT_POSTERS -> 0.8f
            RowLayoutStyle.STANDARD, RowLayoutStyle.LANDSCAPE -> 1f
        }

        val effectiveSize = row.posterSize ?: layout.posterSize.toRowSize()
        val sizeMultiplier = when (effectiveSize) {
            RowPosterSize.SMALL -> 0.82f
            RowPosterSize.MEDIUM -> 1f
            RowPosterSize.LARGE -> 1.22f
        }

        val multiplier = styleMultiplier * sizeMultiplier
        return (baseWidth * multiplier) to (baseHeight * multiplier)
    }

    /** The gap below one row, in dp - Settings -> Home Screen -> Home Layout -> Home Density. */
    fun rowSpacingDp(density: HomeDensity): Float = when (density) {
        HomeDensity.COMPACT -> 16f
        HomeDensity.BALANCED -> 32f
        HomeDensity.SPACIOUS -> 48f
    }

    /** The gap between cards in one row, in dp. */
    fun gapDp(spacing: RowSpacing): Float = when (spacing) {
        RowSpacing.COMPACT -> 8f
        RowSpacing.NORMAL -> 12f
        RowSpacing.WIDE -> 20f
    }

    private fun PosterSizeOption.toRowSize(): RowPosterSize = when (this) {
        PosterSizeOption.SMALL -> RowPosterSize.SMALL
        PosterSizeOption.MEDIUM -> RowPosterSize.MEDIUM
        PosterSizeOption.LARGE -> RowPosterSize.LARGE
        // Custom without a per-row override falls back to Medium's proportions
        // for a row whose own layout style still applies a multiplier - only
        // the no-override, no-style-change case above uses the raw dp value.
        PosterSizeOption.CUSTOM -> RowPosterSize.MEDIUM
    }
}
