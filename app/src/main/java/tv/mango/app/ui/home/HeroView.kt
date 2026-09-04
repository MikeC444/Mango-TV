package tv.mango.app.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.databinding.ViewHeroBinding
import tv.mango.app.models.MediaItem
import tv.mango.app.settings.home.GlassEffectLevel
import tv.mango.app.settings.home.HeroConfig
import tv.mango.app.settings.home.HeroArtworkMode
import tv.mango.app.settings.home.HeroOverlay
import tv.mango.app.settings.home.HeroTransition
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.ThemeDrawables
import tv.mango.app.theme.ThemeDrawables.panelRadiusDp
import tv.mango.app.theme.TypographyScale
import tv.mango.app.ui.core.MotionSpec
import tv.mango.app.utilities.Formatters

/**
 * The cinematic panel at the top of the home screen.
 *
 * Shows whichever title currently holds focus, so moving along a row is also
 * moving through the catalogue's descriptions - the card is the object, the
 * hero is what it is about.
 *
 * That only works if it is cheap. Two things make it so:
 *
 *  - The backdrop is decoded at a fixed cap, not at the size of the screen.
 *    A television is 1920 pixels wide and often 3840; decoding artwork to fill
 *    either is the fastest way to exhaust the heap on this hardware.
 *  - Updates are debounced by the caller. Holding a direction on the remote
 *    moves through a dozen cards a second, and starting a dozen image loads and
 *    then cancelling eleven of them is work for nothing.
 *
 * Style and behaviour - the glass panel, the overlay strength, which pieces of
 * information show at all - come from [HeroConfig], applied by [applyConfig]
 * once per screen build, same as every other shared view in the package.
 */
class HeroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewHeroBinding.inflate(LayoutInflater.from(context), this)

    private var shown: MediaItem? = null
    private var config: HeroConfig = HeroConfig()

    var onPlay: ((MediaItem) -> Unit)? = null
    var onDetails: ((MediaItem) -> Unit)? = null

    init {
        binding.heroPlay.setOnClickListener { shown?.let { item -> onPlay?.invoke(item) } }
        binding.heroDetails.setOnClickListener { shown?.let { item -> onDetails?.invoke(item) } }
        applyConfig(HeroConfig())
    }

    /** Applies a viewer's Hero settings - see Settings -> Home Screen -> Hero. Called once per screen build. */
    fun applyConfig(config: HeroConfig) {
        this.config = config
        val colors = RuntimeTheme.colors
        val glass = RuntimeTheme.config.value.glass

        val typography = RuntimeTheme.config.value.typography
        val accessibility = RuntimeTheme.config.value.accessibility
        TypographyScale.apply(binding.heroTitle, TITLE_BASE_SP, TypographyScale.titleScale(typography, accessibility))
        TypographyScale.apply(binding.heroSynopsis, BODY_BASE_SP, TypographyScale.textScale(typography, accessibility))
        TypographyScale.apply(binding.heroMeta, META_BASE_SP, TypographyScale.metadataScale(typography, accessibility))

        binding.heroEyebrow.setTextColor(colors.accent)
        // A flat "no glass" panel would paint an opaque box over the backdrop;
        // the two scrims behind this panel already carry the legibility work,
        // so with glass off the title floats directly on them instead - see
        // Settings -> Home Screen -> Liquid Glass and the Streamer preset.
        binding.heroPanel.background = if (glass.effect == GlassEffectLevel.OFF) {
            null
        } else {
            ThemeDrawables.glassPanel(
                colors,
                glass,
                glass.cornerRadius.panelRadiusDp() * resources.displayMetrics.density,
            )
        }

        val heightRes = when (config.size) {
            tv.mango.app.settings.home.HeroSize.COMPACT -> R.dimen.hero_height_compact
            tv.mango.app.settings.home.HeroSize.NORMAL -> R.dimen.hero_height
            tv.mango.app.settings.home.HeroSize.LARGE -> R.dimen.hero_height_large
        }
        // Null until this view has actually been attached by its parent - true
        // the first time this runs, from this class's own `init`, before the
        // inflater handing HeroView its XML-declared size even exists yet.
        layoutParams?.let { params ->
            layoutParams = params.apply { height = resources.getDimensionPixelSize(heightRes) }
        }

        binding.heroPanel.visibility = if (config.artworkMode == HeroArtworkMode.BACKDROP_ONLY) INVISIBLE else VISIBLE

        val overlayAlpha = when (config.overlay) {
            HeroOverlay.LIGHT -> 0.6f
            HeroOverlay.MEDIUM -> 0.85f
            HeroOverlay.STRONG -> 1f
        }
        binding.heroScrim.alpha = overlayAlpha
        binding.heroBottomFade.alpha = overlayAlpha
    }

    fun show(item: MediaItem) {
        // Re-binding the title already on screen would restart its crossfade
        // for no change; focus returning to the same card is common.
        if (shown?.id == item.id) return
        val previous = shown
        shown = item

        binding.heroEyebrow.setText(
            if (item.isPartiallyWatched) R.string.label_continue_watching else R.string.label_featured,
        )
        binding.heroPlay.setText(
            if (item.isPartiallyWatched) R.string.action_continue else R.string.action_play,
        )

        binding.heroTitle.visibility = if (config.showTitle) View.VISIBLE else View.GONE
        binding.heroMeta.visibility = if (config.showMetadata) View.VISIBLE else View.GONE
        binding.heroPlay.visibility = if (config.showPlayButton) View.VISIBLE else View.GONE
        binding.heroDetails.visibility = if (config.showSecondaryActions) View.VISIBLE else View.GONE

        applyTextTransition(previous != null) {
            binding.heroTitle.text = item.title
            binding.heroMeta.text = Formatters.metadataLine(context, item)
            binding.heroSynopsis.text = item.synopsis.orEmpty()
            binding.heroSynopsis.visibility =
                if (!config.showDescription || item.synopsis.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        if (config.artworkMode != HeroArtworkMode.STATIC || previous == null) {
            ImageLoader.loadBackdrop(
                target = binding.heroBackdrop,
                key = item.images.backdrop,
                widthPx = BACKDROP_MAX_WIDTH_PX,
                heightPx = BACKDROP_MAX_HEIGHT_PX,
            )
        }
    }

    /**
     * Applies [HeroConfig.transition] to the text panel's content change.
     * [HeroTransition.NONE] and a first, un-animated show both apply the
     * change immediately; every other option gives it a brief, cheap motion -
     * this is still just crossfading a handful of `TextView`s, nothing
     * approaching the cost of a real scene transition.
     */
    private fun applyTextTransition(animate: Boolean, apply: () -> Unit) {
        binding.heroPanel.animate().cancel()
        if (!animate || config.transition == HeroTransition.NONE) {
            apply()
            binding.heroPanel.alpha = 1f
            binding.heroPanel.translationX = 0f
            return
        }

        if (config.transition == HeroTransition.SLIDE) {
            binding.heroPanel.translationX = -SLIDE_DISTANCE_DP * resources.displayMetrics.density
            apply()
            binding.heroPanel.animate()
                .translationX(0f)
                .setDuration(MotionSpec.DURATION_EMPHASIZED)
                .setInterpolator(MotionSpec.emphasized)
                .start()
        } else {
            // Fade and Crossfade read the same way at this scale: text has no
            // second image to crossfade against, so both are one alpha animation.
            binding.heroPanel.alpha = 0f
            apply()
            binding.heroPanel.animate()
                .alpha(1f)
                .setDuration(MotionSpec.DURATION_IMAGE)
                .setInterpolator(MotionSpec.standard)
                .start()
        }
    }

    /** Applies Settings -> Home Screen -> Home Layout's Content Width to the text panel's own maximum width. */
    fun applyContentWidth(maxWidthPx: Int) {
        val params = binding.heroPanel.layoutParams as? ConstraintLayout.LayoutParams ?: return
        if (params.matchConstraintMaxWidth == maxWidthPx) return
        params.matchConstraintMaxWidth = maxWidthPx
        binding.heroPanel.layoutParams = params
    }

    /** Puts focus on the primary action, so the screen always has an entry point. */
    fun focusPrimaryAction(): Boolean = binding.heroPlay.requestFocus()

    override fun onDetachedFromWindow() {
        ImageLoader.clear(binding.heroBackdrop)
        binding.heroPanel.animate().cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        /**
         * Capped well below any television's resolution. The artwork carries no
         * detail that survives being scaled down anyway, and a 4K backdrop held
         * as a bitmap is several times the budget of an entire row of posters.
         */
        const val BACKDROP_MAX_WIDTH_PX = 1280
        const val BACKDROP_MAX_HEIGHT_PX = 720

        const val SLIDE_DISTANCE_DP = 60f

        /** Match TextAppearance.Mango.Display / Body / Label's own sp - the baseline TypographyScale scales from. */
        const val TITLE_BASE_SP = 44f
        const val BODY_BASE_SP = 18f
        const val META_BASE_SP = 15f
    }
}
