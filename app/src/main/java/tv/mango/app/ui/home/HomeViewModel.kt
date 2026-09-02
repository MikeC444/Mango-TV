package tv.mango.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tv.mango.app.data.UiState
import tv.mango.app.models.ContentRow
import tv.mango.app.repository.CatalogRepository

/**
 * Holds the home screen's content across configuration changes and rotations of
 * the view, so returning to Home never re-fetches what is already in hand.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {

    /** Bumped to re-run the request when the viewer asks to retry. */
    private val attempts = MutableStateFlow(0)

    val state: Flow<UiState<List<ContentRow>>> =
        attempts
            .flatMapLatest { repository.homeRows() }
            .stateIn(
                scope = viewModelScope,
                // Survives the brief unsubscribe of a screen change without
                // holding the request open for a screen nobody is looking at.
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading,
            )

    fun retry() {
        attempts.value += 1
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
