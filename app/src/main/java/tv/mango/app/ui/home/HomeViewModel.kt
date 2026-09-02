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
import tv.mango.app.repository.LibraryRepository

/**
 * Holds the home screen's content across view recreation, so returning to Home
 * never re-fetches what is already in hand.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    /** Bumped to re-run the request when the viewer asks to retry. */
    private val attempts = MutableStateFlow(0)

    val state: Flow<UiState<HomeContent>> =
        attempts
            .flatMapLatest { repository.home() }
            .combine(library.continueWatching(), ::withContinueWatching)
            .stateIn(
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
        const val STOP_TIMEOUT_MS = 5_000L
        const val CONTINUE_WATCHING_ROW_ID = "continue_watching"
    }
}
