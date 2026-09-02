package tv.mango.app.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.databinding.ViewHeroBinding
import tv.mango.app.models.MediaItem
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
 */
class HeroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewHeroBinding.inflate(LayoutInflater.from(context), this)

    private var shown: MediaItem? = null

    var onPlay: ((MediaItem) -> Unit)? = null
    var onDetails: ((MediaItem) -> Unit)? = null

    init {
        binding.heroPlay.setOnClickListener { shown?.let { item -> onPlay?.invoke(item) } }
        binding.heroDetails.setOnClickListener { shown?.let { item -> onDetails?.invoke(item) } }
    }

    fun show(item: MediaItem) {
        // Re-binding the title already on screen would restart its crossfade
        // for no change; focus returning to the same card is common.
        if (shown?.id == item.id) return
        shown = item

        binding.heroTitle.text = item.title
        binding.heroMeta.text = Formatters.metadataLine(context, item)
        binding.heroSynopsis.text = item.synopsis
        binding.heroEyebrow.setText(
            if (item.isPartiallyWatched) R.string.label_continue_watching else R.string.label_featured,
        )
        binding.heroPlay.setText(
            if (item.isPartiallyWatched) R.string.action_continue else R.string.action_play,
        )

        ImageLoader.loadBackdrop(
            target = binding.heroBackdrop,
            key = item.images.backdrop,
            widthPx = BACKDROP_MAX_WIDTH_PX,
            heightPx = BACKDROP_MAX_HEIGHT_PX,
        )
    }

    /** Puts focus on the primary action, so the screen always has an entry point. */
    fun focusPrimaryAction(): Boolean = binding.heroPlay.requestFocus()

    override fun onDetachedFromWindow() {
        ImageLoader.clear(binding.heroBackdrop)
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
    }
}
