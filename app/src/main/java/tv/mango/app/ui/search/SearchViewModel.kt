package tv.mango.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * Search by title across films and series.
 *
 * Recent searches live here rather than on disk: a session-scoped list is
 * useful the moment a viewer backs out and returns to try a different title,
 * and persisting it would mean a new store for a screen that otherwise reuses
 * [CatalogRepository] entirely.
 */
class SearchViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        remember(trimmed)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = SearchState.Loading
            _state.value = when (val result = repository.search(trimmed)) {
                is DataResult.Success ->
                    if (result.value.isEmpty()) SearchState.Empty(trimmed) else SearchState.Content(result.value)
                is DataResult.Failure -> SearchState.Error(result.reason)
            }
        }
    }

    private fun remember(query: String) {
        val deduped = _recentSearches.value.filterNot { it.equals(query, ignoreCase = true) }
        _recentSearches.value = (listOf(query) + deduped).take(MAX_RECENT_SEARCHES)
    }

    private companion object {
        const val MAX_RECENT_SEARCHES = 8
    }
}
