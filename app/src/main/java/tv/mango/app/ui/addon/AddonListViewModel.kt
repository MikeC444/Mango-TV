package tv.mango.app.ui.addon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import tv.mango.app.addon.AddonRepository
import tv.mango.app.addon.model.Addon

/** The installed add-ons, in priority order, live from storage. */
class AddonListViewModel(
    repository: AddonRepository,
) : ViewModel() {

    val addons: StateFlow<List<Addon>> = repository.addons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
