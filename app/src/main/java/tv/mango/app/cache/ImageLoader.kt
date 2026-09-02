package tv.mango.app.cache

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import tv.mango.app.R

/**
 * Every image request in the application goes through here.
 *
 * Image handling is the single easiest way to make a streaming app unusable on
 * a Fire Stick, so the rules are enforced in one place rather than trusted to
 * each call site:
 *
 *  - **Decode at the size actually drawn.** Every request states its target in
 *    pixels. Without this, a poster is decoded at its full source size and the
 *    device shrinks it every frame, holding several times the memory it needs.
 *  - **Posters decode as RGB_565.** Half the bytes per pixel of ARGB_8888. The
 *    artwork has no transparency and no gradients fine enough for the loss of
 *    colour depth to show at poster size, so this is free.
 *  - **The backdrop decodes at full depth.** It fills the screen, where 16-bit
 *    banding across a large smooth field would be visible.
 *  - **Rows do not cross-fade.** A fade on every card that scrolls into view is
 *    a continuous animation across the whole screen. The hero fades, because
 *    there is one of it and the change is meaningful.
 */
object ImageLoader {

    private val posterOptions = RequestOptions()
        .format(DecodeFormat.PREFER_RGB_565)
        // AUTOMATIC skips caching a copy of something already on local disk,
        // and caches properly once these become network requests.
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .dontAnimate()
        .placeholder(R.drawable.card_placeholder)

    private val backdropOptions = RequestOptions()
        .format(DecodeFormat.PREFER_ARGB_8888)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)

    private val source: ArtworkSource = BundledArtworkSource()

    fun loadPoster(target: ImageView, key: String, widthPx: Int, heightPx: Int) {
        Glide.with(target)
            .load(source.uriFor(key, widthPx, heightPx))
            .apply(posterOptions)
            .override(widthPx, heightPx)
            .into(target)
    }

    fun loadBackdrop(target: ImageView, key: String, widthPx: Int, heightPx: Int) {
        Glide.with(target)
            .load(source.uriFor(key, widthPx, heightPx))
            .apply(backdropOptions)
            .override(widthPx, heightPx)
            .transition(DrawableTransitionOptions.withCrossFade(CROSSFADE_MS))
            .into(target)
    }

    /**
     * Cancels an in-flight request and releases the bitmap reference.
     *
     * Called when a card is recycled. Without it, a request started for a card
     * that has since scrolled away still completes, and can deliver the wrong
     * artwork into a view that has been rebound to something else.
     */
    fun clear(target: ImageView) {
        Glide.with(target).clear(target)
        target.setImageDrawable(null)
    }

    private const val CROSSFADE_MS = 320
}
