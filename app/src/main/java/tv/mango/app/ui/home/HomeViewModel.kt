package tv.mango.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.mango.app.data.UiState
import tv.mango.app.data.refresh.RefreshFold
import tv.mango.app.data.refresh.Refreshable
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
 * Holds the home screen's content.
 *
 * Fetched once per launch and then kept. An earlier version published through
 * `WhileSubscribed(5_000)`, which meant that leaving the screen for more than
 * five seconds - opening a title, glancing at Movies, backgrounding the app -
 * cancelled the request and re-ran it on the way back. That was work nobody
 * asked for, and a network fan-out across every add-on each time.
 *
 * `Lazily` starts on first subscription and never restarts, so returning shows
 * what is already held. New content arrives only when [refresh] asks for it,
 * folded against what is already on screen via [RefreshFold] so a refresh
 * never blanks a working screen.
 *
 * On top of that catalogue data, three things are layered from
 * [HomeScreenConfigRepository]:
 *
 *  - Continue Watching, synthesised from the library - it stays live
 *    regardless of whether the catalogue itself is re-fetched, since it is
 *    combined in from its own flow.
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

    /** Bumped to re-run the request when the viewer asks to retry or refresh. */
    private val attempts = MutableStateFlow(0)

    val state: StateFlow<Refreshable<HomeContent>> =
        combine(
            attempts.flatMapLatest { repository.home() },
            library.continueWatching(),
            library.watchedIds,
            homeScreenConfig.config,
            ::mergeState,
        )
            // Folded against what is already on screen, so a refresh never
            // replaces a working screen with a blank one.
            .scan(RefreshFold.initial<HomeContent>()) { previous, incoming ->
                RefreshFold.next(previous, incoming)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = RefreshFold.initial(),
            )

    /** Asked for by the viewer. Keeps the current screen up while it runs. */
    fun refresh() {
        attempts.value += 1
    }

    /** Asked for from an error state, where there is nothing to keep up. */
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
        const val CONTINUE_WATCHING_ROW_ID = "continue_watching"
    }
}
