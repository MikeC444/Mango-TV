package tv.mango.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.mango.app.data.DataResult
import tv.mango.app.data.UiState
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import tv.mango.app.models.TitleDetail
import tv.mango.app.repository.CatalogRepository
import tv.mango.app.repository.LibraryRepository

/**
 * State for one detail screen.
 *
 * The title's detail, the selected season's episodes and whether the title is
 * saved are three separate streams rather than one combined object, because
 * they change independently: saving a title should not re-render the episode
 * list, and changing season should not re-fetch the cast.
 */
class DetailViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val id: MediaId,
    private val type: MediaType,
) : ViewModel() {

    private val _detail = MutableStateFlow<UiState<TitleDetail>>(UiState.Loading)
    val detail: StateFlow<UiState<TitleDetail>> = _detail.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

    private val _selectedSeason = MutableStateFlow(FIRST_SEASON)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    val inLibrary: StateFlow<Boolean> = library.isInWatchlist(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    init {
        load()
    }

    fun load() {
        _detail.value = UiState.Loading
        viewModelScope.launch {
            when (val result = catalog.detail(id, type)) {
                is DataResult.Success -> {
                    _detail.value = UiState.Content(result.value)
                    result.value.seasons.firstOrNull()?.let { selectSeason(it.number) }
                }

                is DataResult.Failure -> _detail.value = UiState.Error(result.reason)
            }
        }
    }

    fun selectSeason(number: Int) {
        _selectedSeason.value = number
        viewModelScope.launch {
            // A season that fails to load leaves the list empty rather than
            // replacing the whole screen with an error: the title's own detail
            // is still perfectly readable.
            _episodes.value = when (val result = catalog.episodes(id, number)) {
                is DataResult.Success -> result.value
                is DataResult.Failure -> emptyList()
            }
        }
    }

    fun toggleLibrary() {
        viewModelScope.launch {
            library.setInWatchlist(id, saved = !inLibrary.value)
        }
    }

    private companion object {
        const val FIRST_SEASON = 1
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
