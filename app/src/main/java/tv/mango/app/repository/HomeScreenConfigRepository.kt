package tv.mango.app.repository

import kotlinx.coroutines.flow.Flow
import tv.mango.app.data.local.HomeScreenConfigStore
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.settings.home.HomePreset
import tv.mango.app.settings.home.HomeScreenPresets
import tv.mango.app.settings.home.RowConfig

/**
 * What every Home Screen settings screen reads from and writes through, and
 * what [tv.mango.app.theme.RuntimeTheme] observes to keep the live application
 * in step with it.
 *
 * Every write here goes through [HomeScreenConfigStore.update], which is what
 * lets [update] be expressed as a small, targeted change - flip this one field
 * - without any caller needing to hold the rest of the configuration itself.
 */
class HomeScreenConfigRepository(
    private val store: HomeScreenConfigStore,
) {

    val config: Flow<HomeScreenConfig> = store.config

    suspend fun update(transform: (HomeScreenConfig) -> HomeScreenConfig) {
        // Any manual change moves the viewer off a named preset - "Custom" is
        // not something a viewer selects, it is what touching a control after
        // a preset always leaves behind.
        store.update { current -> transform(current).copy(preset = HomePreset.CUSTOM) }
    }

    suspend fun updateRow(rowId: String, transform: (RowConfig) -> RowConfig) {
        update { current ->
            val existing = current.rows.configFor(rowId)
            current.copy(rows = current.rows.copy(rows = current.rows.rows + (rowId to transform(existing))))
        }
    }

    suspend fun setRowOrder(order: List<String>) {
        update { current -> current.copy(rows = current.rows.copy(order = order)) }
    }

    suspend fun applyPreset(preset: HomePreset) {
        store.save(HomeScreenPresets.configFor(preset))
    }

    /**
     * Restores an exact, previously-read configuration - what Settings ->
     * Home Screen -> Preview's Cancel button does with the snapshot it took
     * when a viewer opened Preview, undoing anything changed since without
     * touching whatever it was that time was already.
     */
    suspend fun restore(snapshot: HomeScreenConfig) {
        store.save(snapshot)
    }

    /** Restores the default appearance. Never touches watch history, watchlist, add-ons or playback data. */
    suspend fun reset() {
        store.save(HomeScreenConfig.default())
    }
}
