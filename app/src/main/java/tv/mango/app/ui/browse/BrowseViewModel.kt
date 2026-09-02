package tv.mango.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.mango.app.data.DataResult
import tv.mango.app.data.UiState
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.repository.CatalogRepository

/**
 * Paging state for one browse screen.
 *
 * The accumulated list lives here rather than in the fragment, so scrolling
 * position and loaded pages survive the view being destroyed and rebuilt -
 * coming back from a detail screen should not mean loading the catalogue again
 * and losing the viewer's place.
 *
 * Requests are guarded so a burst of scroll events near the end of the list
 * cannot start the same page several times over.
 */
class BrowseViewModel(
    private val repository: CatalogRepository,
    private val type: MediaType,
) : ViewModel() {

    private val loaded = mutableListOf<MediaItem>()

    private val _state = MutableStateFlow<UiState<List<MediaItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<MediaItem>>> = _state.asStateFlow()

    private var nextPage = 0
    private var loading = false
    private var reachedEnd = false

    init {
        loadNextPage()
    }

    /**
     * Called as the viewer approaches the end of what is loaded. Safe to call
     * repeatedly; it does nothing while a page is in flight or once the
     * collection is exhausted.
     */
    fun loadNextPage() {
        if (loading || reachedEnd) return
        loading = true

        viewModelScope.launch {
            when (val result = repository.browsePage(type, nextPage)) {
                is DataResult.Success -> {
                    if (result.value.isEmpty()) {
                        reachedEnd = true
                        // An empty first page means the collection is empty;
                        // an empty later page just means we have it all.
                        if (loaded.isEmpty()) _state.value = UiState.Empty
                    } else {
                        loaded += result.value
                        nextPage += 1
                        _state.value = UiState.Content(loaded.toList())
                    }
                }

                is DataResult.Failure -> {
                    // A page that fails part-way through leaves what is already
                    // loaded on screen; only a failure with nothing to show
                    // becomes an error state.
                    if (loaded.isEmpty()) _state.value = UiState.Error(result.reason)
                }
            }
            loading = false
        }
    }

    fun retry() {
        reachedEnd = false
        _state.value = UiState.Loading
        loadNextPage()
    }
}
