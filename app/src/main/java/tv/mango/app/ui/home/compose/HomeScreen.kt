package tv.mango.app.ui.home.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState
import tv.mango.app.data.refresh.Refreshable
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaItem

private const val HERO_HEIGHT_DP = 480
private const val HERO_DEBOUNCE_MS = 220L

/**
 * The NuvioTV-style Home surface: [HeroSection] over [CatalogRowSection]s in
 * one scrolling column, real data throughout - [tv.mango.app.ui.home.HomeViewModel]
 * is untouched, so Continue Watching, watched badges and Settings -> Home
 * Screen -> Catalog Rows' visibility/order/renaming all still apply exactly
 * as they did on the View-based screen this replaces.
 */
@Composable
fun HomeScreen(
    state: Refreshable<HomeContent>,
    colors: HomeColors,
    onCardClick: (MediaItem) -> Unit,
    onCardLongClick: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        when (val content = state.content) {
            is UiState.Loading -> Unit // A still, dark surface beats a spinner - see the old HomeFragment's own reasoning.

            is UiState.Content -> HomeScreenContent(
                content = content.value,
                colors = colors,
                onCardClick = onCardClick,
                onCardLongClick = onCardLongClick,
                onPlay = onPlay,
                onDetails = onDetails,
            )

            is UiState.Empty -> HomeMessage(title = "Nothing to show yet", colors = colors, onRetry = onRetry)

            is UiState.Error -> HomeMessage(
                title = if (content.reason == FailureReason.NETWORK) "Can't reach the network" else "Something went wrong",
                body = if (content.reason == FailureReason.NETWORK) "Check the connection and try again." else null,
                colors = colors,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    content: HomeContent,
    colors: HomeColors,
    onCardClick: (MediaItem) -> Unit,
    onCardLongClick: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val heroHeightPx = with(density) { HERO_HEIGHT_DP.dp.roundToPx() }

    val heroFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                0f
            } else {
                (1f - listState.firstVisibleItemScrollOffset.toFloat() / heroHeightPx).coerceIn(0f, 1f)
            }
        }
    }
    val heroVisible = heroFraction > 0.05f

    var featured by remember(content.featured.id) { mutableStateOf(content.featured) }
    var pendingFocused by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(pendingFocused, heroVisible) {
        val candidate = pendingFocused
        if (candidate != null && heroVisible && candidate.id != featured.id) {
            delay(HERO_DEBOUNCE_MS)
            featured = candidate
        }
    }

    val playFocusRequester = remember { FocusRequester() }
    var initialFocusClaimed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialFocusClaimed) {
            initialFocusClaimed = true
            runCatching { playFocusRequester.requestFocus() }
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "hero") {
            Box(modifier = Modifier.fillMaxWidth().height(HERO_HEIGHT_DP.dp)) {
                if (heroVisible) {
                    HeroSection(
                        item = featured,
                        colors = colors,
                        heightDp = HERO_HEIGHT_DP,
                        onPlay = { onPlay(featured) },
                        onDetails = { onDetails(featured) },
                        playFocusRequester = playFocusRequester,
                        modifier = Modifier.background(colors.background),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(colors.background))
                }
            }
        }

        items(items = content.rows, key = { it.id }) { row ->
            CatalogRowSection(
                row = row,
                colors = colors,
                onItemClick = onCardClick,
                onItemLongClick = onCardLongClick,
                onItemFocused = { pendingFocused = it },
                modifier = Modifier.padding(bottom = 28.dp),
            )
        }

        item(key = "bottom_safe_area") {
            Box(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun HomeMessage(
    title: String,
    colors: HomeColors,
    onRetry: () -> Unit,
    body: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 64.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(text = title, style = TextStyle(color = colors.primaryText, fontSize = 24.sp))
        body?.let {
            BasicText(
                text = it,
                style = TextStyle(color = colors.secondaryText, fontSize = 16.sp),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Box(modifier = Modifier.padding(top = 20.dp)) {
            RetryButton(colors = colors, onClick = onRetry)
        }
    }
}

@Composable
private fun RetryButton(colors: HomeColors, onClick: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        BasicText(
            text = "Retry",
            style = TextStyle(color = if (focused) colors.background else colors.primaryText, fontSize = 16.sp),
        )
    }
}
