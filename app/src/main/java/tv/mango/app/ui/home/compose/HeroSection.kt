package tv.mango.app.ui.home.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.mango.app.models.MediaItem
import tv.mango.app.utilities.Formatters

/**
 * The cinematic backdrop-and-metadata panel NuvioTV opens every browse
 * session with. Shows whichever title [item] currently is - Home's own
 * focus-follows logic (debouncing, "stop touching it once scrolled away")
 * lives one level up, in [tv.mango.app.ui.home.compose.HomeScreenContent],
 * since that behaviour is about the screen's scroll position, not about how
 * one hero frame draws.
 */
@Composable
fun HeroSection(
    item: MediaItem,
    colors: HomeColors,
    heightDp: Int,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    playFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { 1280.dp.roundToPx() }
    val heightPx = with(density) { 720.dp.roundToPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
    ) {
        BackdropImage(
            artworkKey = item.images.backdrop,
            widthPx = widthPx,
            heightPx = heightPx,
            modifier = Modifier.fillMaxSize(),
        )

        // A cinematic gradient over the artwork - dark at the edge the text
        // sits against, clear over the artwork's own focal point on the
        // right, exactly the treatment the brief calls for instead of a
        // real-time blur this hardware cannot afford.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(colors.background, colors.background.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.background),
                        startY = 0.4f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = 640.dp)
                .padding(start = 48.dp, bottom = 40.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicText(
                text = if (item.isPartiallyWatched) "CONTINUE WATCHING" else "FEATURED",
                style = TextStyle(color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            BasicText(
                text = item.title,
                style = TextStyle(color = colors.primaryText, fontSize = 40.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = Formatters.metadataLine(context, item),
                style = TextStyle(color = colors.secondaryText, fontSize = 16.sp),
            )
            item.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                BasicText(
                    text = synopsis,
                    style = TextStyle(color = colors.secondaryText, fontSize = 15.sp, lineHeight = 20.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroButton(
                    label = if (item.isPartiallyWatched) "Continue" else "Play",
                    colors = colors,
                    primary = true,
                    onClick = onPlay,
                    modifier = Modifier.focusRequester(playFocusRequester),
                )
                HeroButton(label = "Details", colors = colors, primary = false, onClick = onDetails)
            }
        }
    }
}

@Composable
private fun HeroButton(
    label: String,
    colors: HomeColors,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        primary && focused -> colors.accent
        primary -> colors.accent.copy(alpha = 0.85f)
        focused -> colors.primaryText
        else -> colors.surface.copy(alpha = 0.7f)
    }
    val textColor = if (primary || focused) colors.background else colors.primaryText

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = colors.focusGlow,
                shape = RoundedCornerShape(8.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        BasicText(text = label, style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
    }
}
