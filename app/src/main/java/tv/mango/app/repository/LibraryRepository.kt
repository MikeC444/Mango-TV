package tv.mango.app.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tv.mango.app.data.local.LibraryStore
import tv.mango.app.models.MediaId

/**
 * What the viewer has saved, and how far through things they are.
 *
 * Sits between the screens and the store so screens deal in [MediaId] rather
 * than in raw strings, and so the storage underneath can be replaced without
 * any screen noticing.
 */
class LibraryRepository(
    private val store: LibraryStore,
) {

    fun isInWatchlist(id: MediaId): Flow<Boolean> =
        store.watchlist.map { id.value in it }

    val watchlist: Flow<Set<MediaId>> =
        store.watchlist.map { ids -> ids.mapTo(mutableSetOf(), ::MediaId) }

    fun progressOf(id: MediaId): Flow<Float> =
        store.progress.map { it[id.value]?.fraction ?: 0f }

    suspend fun setInWatchlist(id: MediaId, saved: Boolean) =
        store.setInWatchlist(id.value, saved)

    suspend fun recordProgress(id: MediaId, fraction: Float) =
        store.setProgress(id.value, fraction, System.currentTimeMillis())
}
