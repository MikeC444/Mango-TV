package tv.mango.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import tv.mango.app.data.DataResult
import tv.mango.app.data.FailureReason
import tv.mango.app.models.MediaItem
import tv.mango.app.repository.CatalogRepository

/** What the search screen shows, once something has actually been searched for. */
sealed interface SearchState {
    /** Nothing searched yet this visit - the prompt, not an empty result. */
    data object Idle : SearchState
    data object Loading : SearchState
    data class Content(val results: List<MediaItem>) : SearchState
    data class Empty(val query: String) : SearchState
    data class Error(val reason: FailureReason) : SearchState
}

/**
 * Search by title across films and series, live as the viewer types.
 *
 * Recent searches live here rather than on disk: a session-scoped list is
 * useful the moment a viewer backs out and returns to try a different title,
 * and persisting it would mean a new store for a screen that otherwise reuses
 * [CatalogRepository] entirely. Popular searches are not personal at all -
 * a fixed, editorial list, the same shortcut a viewer with an empty history
 * still benefits from.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {

    /** Every keystroke lands here; [state] only actually searches once typing pauses. */
    private val query = MutableStateFlow("")

    /**
     * An explicit "Find Similar" request, from a card's long-press menu -
     * distinct from [query] because it should never wait out the typing
     * debounce, and matches by genre rather than by title.
     */
    private val similarTrigger = MutableStateFlow<MediaItem?>(null)

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    val popularSearches: List<String> = POPULAR_SEARCHES

    val state: StateFlow<SearchState> = merge(
        query.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged().map<String, SearchTrigger> { SearchTrigger.Typed(it) },
        similarTrigger.filterNotNull().map<MediaItem, SearchTrigger> { SearchTrigger.Similar(it) },
    )
        .flatMapLatest<SearchTrigger, SearchState> { trigger ->
            when (trigger) {
                is SearchTrigger.Typed -> {
                    val trimmed = trigger.text.trim()
                    if (trimmed.isEmpty()) {
                        flowOf(SearchState.Idle)
                    } else {
                        flow {
                            emit(SearchState.Loading)
                            emit(runSearch(trimmed))
                        }
                    }
                }

                is SearchTrigger.Similar -> flow {
                    emit(SearchState.Loading)
                    emit(runSimilar(trigger.item))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SearchState.Idle,
        )

    /** Drives live, as-you-type results. Debounced above, so every keystroke is cheap to call. */
    fun onQueryChanged(text: String) {
        query.value = text
    }

    /** An explicit search - Enter, or picking a recent/popular chip - worth remembering. */
    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        remember(trimmed)
        query.value = trimmed
    }

    /** Runs immediately, bypassing the typing debounce - a long press is already an explicit choice. */
    fun findSimilar(item: MediaItem) {
        similarTrigger.value = item
    }

    private suspend fun runSearch(trimmed: String): SearchState = when (val result = repository.search(trimmed)) {
        is DataResult.Success ->
            if (result.value.isEmpty()) SearchState.Empty(trimmed) else SearchState.Content(result.value)
        is DataResult.Failure -> SearchState.Error(result.reason)
    }

    private suspend fun runSimilar(item: MediaItem): SearchState = when (val result = repository.similarTo(item)) {
        is DataResult.Success ->
            if (result.value.isEmpty()) SearchState.Empty(item.title) else SearchState.Content(result.value)
        is DataResult.Failure -> SearchState.Error(result.reason)
    }

    private sealed interface SearchTrigger {
        data class Typed(val text: String) : SearchTrigger
        data class Similar(val item: MediaItem) : SearchTrigger
    }

    private fun remember(query: String) {
        val deduped = _recentSearches.value.filterNot { it.equals(query, ignoreCase = true) }
        _recentSearches.value = (listOf(query) + deduped).take(MAX_RECENT_SEARCHES)
    }

    private companion object {
        const val MAX_RECENT_SEARCHES = 8
        const val SEARCH_DEBOUNCE_MS = 350L
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * A fixed editorial list rather than derived from anything - there is
         * no search-frequency telemetry in this application to base a real
         * "trending" list on, and a list that quietly did nothing would be
         * worse than an honest, static one.
         */
        val POPULAR_SEARCHES = listOf(
            "The Salt Road",
            "A Quiet Inheritance",
            "Northern Lights",
            "The Cartographer",
        )
    }
}
