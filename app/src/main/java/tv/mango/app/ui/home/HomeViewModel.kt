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
import tv.mango.app.repository.LibraryRepository

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
 * what is already held. New content arrives only when [refresh] asks for it.
 * Continue Watching stays live regardless: it is combined in from its own flow,
 * which keeps emitting whether or not the catalogue is re-fetched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    /** Bumped to re-run the request when the viewer asks to retry. */
    private val attempts = MutableStateFlow(0)

    val state: StateFlow<Refreshable<HomeContent>> =
        attempts
            .flatMapLatest { repository.home() }
            .combine(library.continueWatching(), ::withContinueWatching)
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

    /**
     * Puts Continue Watching first when there is one to show, ahead of
     * anything the catalogue itself offers - resuming something is a more
     * useful first row than discovering something new.
     */
    private fun withContinueWatching(
        catalogState: UiState<HomeContent>,
        continueWatching: List<ContinueWatchingItem>,
    ): UiState<HomeContent> {
        if (catalogState !is UiState.Content || continueWatching.isEmpty()) return catalogState
        val row = ContentRow(
            id = CONTINUE_WATCHING_ROW_ID,
            // Every other row's title already comes from the catalogue as a
            // plain string rather than a resource - this one is no different,
            // it just happens to be supplied here instead of by a provider.
            title = "Continue Watching",
            items = continueWatching.map { it.toMediaItem() },
        )
        return UiState.Content(catalogState.value.copy(rows = listOf(row) + catalogState.value.rows))
    }

    private companion object {
        const val CONTINUE_WATCHING_ROW_ID = "continue_watching"
    }
}
