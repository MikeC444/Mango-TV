package tv.mango.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.mango.app.data.UiState
import tv.mango.app.models.ContentRow
import tv.mango.app.models.ContinueWatchingItem
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaId
import tv.mango.app.repository.CatalogRepository
import tv.mango.app.repository.HomeScreenConfigRepository
import tv.mango.app.repository.LibraryRepository
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.settings.home.RowsConfig

/**
 * Holds the home screen's content across view recreation, so returning to Home
 * never re-fetches what is already in hand.
 *
 * Rows arrive here exactly as the catalogue supplies them; what a viewer sees
 * is that plus three things layered on top, all driven by
 * [HomeScreenConfigRepository]:
 *
 *  - Continue Watching, synthesised from the library the same as before.
 *  - Every title's watched badge, from the library's own watched marks.
 *  - Settings -> Home Screen -> Catalog Rows' own visibility, order and
 *    renaming - the one place row *identity* changes; everything about how a
 *    visible row's cards are drawn (size, layout, captions...) is resolved
 *    later, per row, by [tv.mango.app.ui.core.ContentRowsAdapter].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CatalogRepository,
    private val library: LibraryRepository,
    private val homeScreenConfig: HomeScreenConfigRepository,
) : ViewModel() {

    /** Bumped to re-run the request when the viewer asks to retry. */
    private val attempts = MutableStateFlow(0)

    val state: Flow<UiState<HomeContent>> =
        combine(
            attempts.flatMapLatest { repository.home() },
            library.continueWatching(),
            library.watchedIds,
            homeScreenConfig.config,
            ::mergeState,
        ).stateIn(
            scope = viewModelScope,
            // Survives the brief unsubscribe of a screen change without
            // holding a request open for a screen nobody is looking at.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading,
        )

    fun retry() {
        attempts.value += 1
    }

    fun removeFromContinueWatching(id: MediaId) {
        viewModelScope.launch { library.removeFromContinueWatching(id) }
    }

    private fun mergeState(
        catalogState: UiState<HomeContent>,
        continueWatching: List<ContinueWatchingItem>,
        watchedIds: Set<MediaId>,
        config: HomeScreenConfig,
    ): UiState<HomeContent> {
        if (catalogState !is UiState.Content) return catalogState
        var rows = catalogState.value.rows

        if (watchedIds.isNotEmpty()) {
            rows = rows.map { row -> row.copy(items = row.items.map { it.copy(watched = it.id in watchedIds) }) }
        }

        // Puts Continue Watching first when there is one to show, ahead of
        // anything the catalogue itself offers - resuming something is a more
        // useful first row than discovering something new. Row-level settings
        // (visibility, order, renaming) still apply to it exactly like any
        // other row from here on, keyed by this same id.
        if (continueWatching.isNotEmpty()) {
            val row = ContentRow(
                id = CONTINUE_WATCHING_ROW_ID,
                title = "Continue Watching",
                items = continueWatching.map { it.toMediaItem() },
            )
            rows = listOf(row) + rows
        }

        rows = applyRowsConfig(rows, config.rows)

        return UiState.Content(catalogState.value.copy(rows = rows))
    }

    /**
     * A viewer's own visibility, order and naming for rows - never anything
     * about how a visible row's cards are drawn, which stays resolved at
     * render time so it can vary the moment a row scrolls back on screen
     * without this state needing to change at all.
     */
    private fun applyRowsConfig(rows: List<ContentRow>, rowsConfig: RowsConfig): List<ContentRow> {
        val byId = rows.associateBy { it.id }
        val ordered = rowsConfig.order.mapNotNull { byId[it] } +
            rows.filter { it.id !in rowsConfig.order }

        return ordered.mapNotNull { row ->
            val rowConfig = rowsConfig.configFor(row.id)
            if (!rowConfig.visible) return@mapNotNull null
            val title = rowConfig.customTitle?.takeIf { it.isNotBlank() } ?: row.title
            if (title == row.title) row else row.copy(title = title)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CONTINUE_WATCHING_ROW_ID = "continue_watching"
    }
}
