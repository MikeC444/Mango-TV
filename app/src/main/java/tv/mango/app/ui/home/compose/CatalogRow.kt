package tv.mango.app.ui.home.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaItem

/**
 * One titled, horizontally scrolling row - the same [ContentRow] the old
 * `ContentRowsAdapter` rendered, already filtered, ordered and renamed by
 * [tv.mango.app.ui.home.HomeViewModel] per Settings -> Home Screen -> Catalog
 * Rows, so this composable only has to draw it.
 */
@Composable
fun CatalogRowSection(
    row: ContentRow,
    colors: HomeColors,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onItemFocused: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (row.title.isNotBlank()) {
            BasicText(
                text = row.title,
                style = TextStyle(
                    color = colors.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 48.dp),
        ) {
            items(items = row.items, key = { it.id.value }) { item ->
                MediaCard(
                    item = item,
                    colors = colors,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    // hasFocus, not isFocused: the actual focus target is the
                    // inner clickable surface inside MediaCard, a descendant
                    // of the node this modifier is attached to.
                    modifier = Modifier.onFocusChanged { state -> if (state.hasFocus) onItemFocused(item) },
                )
            }
        }
    }
}
