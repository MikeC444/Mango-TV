package tv.mango.app.ui.addon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tv.mango.app.addon.AddonRepository
import tv.mango.app.addon.model.Addon

/**
 * One installed add-on, live from storage - enabling, reordering and removing
 * all read back through the same [addon] stream rather than local state, so
 * the screen can never show something storage disagrees with.
 */
class AddonDetailViewModel(
    private val addonId: String,
    private val repository: AddonRepository,
) : ViewModel() {

    val addon: StateFlow<Addon?> = repository.addons
        .map { installed -> installed.firstOrNull { it.id == addonId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(addonId, enabled) }
    }

    fun moveUp() = reorder(delta = -1)

    fun moveDown() = reorder(delta = 1)

    fun remove() {
        viewModelScope.launch { repository.remove(addonId) }
    }

    private fun reorder(delta: Int) {
        viewModelScope.launch {
            val order = repository.installed().map { it.id }.toMutableList()
            val index = order.indexOf(addonId)
            val target = index + delta
            if (index < 0 || target !in order.indices) return@launch
            order.removeAt(index)
            order.add(target, addonId)
            repository.reorder(order)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
