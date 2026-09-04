package tv.mango.app.ui.home.compose

import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import tv.mango.app.cache.ImageLoader

/**
 * A poster or backdrop, loaded through the same [ImageLoader] every View-based
 * screen uses - the RGB_565 poster decoding, the capped backdrop size and the
 * disk cache all still apply here. Compose has no cheap way to reuse Glide's
 * bitmap pool directly, so this wraps a single plain [ImageView] rather than
 * pulling in a second image pipeline (Coil) that would cache the same
 * artwork twice.
 */
@Composable
fun PosterImage(
    artworkKey: String,
    widthPx: Int,
    heightPx: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        },
        update = { imageView -> ImageLoader.loadPoster(imageView, artworkKey, widthPx, heightPx) },
        onRelease = { imageView -> ImageLoader.clear(imageView) },
    )
}

@Composable
fun BackdropImage(
    artworkKey: String?,
    widthPx: Int,
    heightPx: Int,
    modifier: Modifier = Modifier,
) {
    if (artworkKey == null) return
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        },
        update = { imageView -> ImageLoader.loadBackdrop(imageView, artworkKey, widthPx, heightPx) },
        onRelease = { imageView -> ImageLoader.clear(imageView) },
    )
}
