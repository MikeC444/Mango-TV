package tv.mango.app.ui.addon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.mango.app.addon.AddonInstaller

/**
 * Everything on screen while installing one add-on.
 *
 * [Ready] and [NeedsConfiguration] carry the [AddonInstaller.Preview] the
 * add-on answered with; nothing is written to storage until [AddAddonViewModel.install]
 * is called, so an add-on is never on the device because a URL was pasted -
 * the viewer sees exactly what the add-on says about itself first.
 */
sealed interface AddAddonState {
    data object Idle : AddAddonState
    data object Loading : AddAddonState
    data class Ready(val preview: AddonInstaller.Preview.Ready) : AddAddonState
    data class NeedsConfiguration(val preview: AddonInstaller.Preview.NeedsConfiguration) : AddAddonState
    data class Failed(val reason: AddonInstaller.Preview.Reason) : AddAddonState
    data object Installing : AddAddonState
    data object Installed : AddAddonState
}

class AddAddonViewModel(
    private val installer: AddonInstaller,
) : ViewModel() {

    private val _state = MutableStateFlow<AddAddonState>(AddAddonState.Idle)
    val state: StateFlow<AddAddonState> = _state.asStateFlow()

    fun preview(url: String) {
        if (url.isBlank()) return
        _state.value = AddAddonState.Loading
        viewModelScope.launch {
            _state.value = when (val result = installer.preview(url)) {
                is AddonInstaller.Preview.Ready -> AddAddonState.Ready(result)
                is AddonInstaller.Preview.NeedsConfiguration -> AddAddonState.NeedsConfiguration(result)
                is AddonInstaller.Preview.Failed -> AddAddonState.Failed(result.reason)
            }
        }
    }

    /** Only this writes anything - everything before it was only ever a look. */
    fun install() {
        val ready = (_state.value as? AddAddonState.Ready)?.preview ?: return
        _state.value = AddAddonState.Installing
        viewModelScope.launch {
            installer.install(ready)
            _state.value = AddAddonState.Installed
        }
    }

    /** Back to the URL field, whether the viewer cancelled or is trying a different address. */
    fun reset() {
        _state.value = AddAddonState.Idle
    }
}
