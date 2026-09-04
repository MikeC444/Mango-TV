package tv.mango.app.ui.home.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import tv.mango.app.models.MediaItem

/** Poster aspect ratio the whole application uses - see `card_poster_width`/`card_poster_height`. */
private const val POSTER_ASPECT = 130f / 195f

/**
 * The NuvioTV-style poster card: rounded artwork, a focus scale-and-glow (no
 * separate elevation/shadow animator - one `scale` on the whole card reads
 * identically at ten feet and costs one animated float), a watched check and
 * a Continue Watching progress bar. No title beneath the artwork - the
 * poster itself is the identity, the same as NuvioTV's own rows.
 *
 * The outer padding matters more than it looks: without it, a card at its
 * focused 1.08x scale grows past its own row slot and visibly overlaps the
 * next card mid-scroll, which reads as the whole row juddering as focus
 * moves. The padding gives the scale room to grow into that never belongs
 * to a neighbour.
 *
 * Deliberately its own implementation rather than reusing [tv.mango.app.ui.core.TvCardView]:
 * that class is tied to the View focus/animation system Browse, Search and
 * Detail still use, and Compose has no cheap way to host it without losing
 * everything actually being changed here (shape, glow colour, layout).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    item: MediaItem,
    colors: HomeColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int = 130,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(200),
        label = "cardScale",
    )
    val density = LocalDensity.current
    val widthPx = with(density) { widthDp.dp.roundToPx() }
    val heightPx = (widthPx / POSTER_ASPECT).toInt()

    Box(
        modifier = modifier.padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(widthDp.dp)
                .height((widthDp / POSTER_ASPECT).dp)
                .scale(scale)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(
                    width = if (focused) 3.dp else 0.dp,
                    color = if (focused) colors.focusGlow else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                )
                .onFocusChanged { focused = it.isFocused }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            PosterImage(
                artworkKey = item.images.poster,
                widthPx = widthPx,
                heightPx = heightPx,
                modifier = Modifier.fillMaxSize(),
            )

            if (item.watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colors.accent),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        val stroke = Stroke(width = size.minDimension * 0.22f)
                        drawLine(
                            color = Color.Black,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.55f),
                            end = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height),
                            strokeWidth = stroke.width,
                        )
                        drawLine(
                            color = Color.Black,
                            start = androidx.compose.ui.geometry.Offset(size.width * 0.4f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = stroke.width,
                        )
                    }
                }
            }

            if (item.isPartiallyWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .background(colors.accent),
                    )
                }
            }
        }
    }
}
