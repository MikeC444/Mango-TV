package tv.mango.app.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tv.mango.app.data.local.LibraryStore
import tv.mango.app.data.local.PlaybackPosition
import tv.mango.app.models.ContinueWatchingItem
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import tv.mango.app.models.ResumePoint

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

    /**
     * Every title with somewhere to resume, most recently watched first.
     *
     * Keyed by the series or film's own id, never by an episode's - picking
     * a different episode of a show already in progress moves the one card,
     * it never creates a second one, which is what actually lets a card be
     * pointed back at with "resume from exactly where you stopped" rather
     * than at whichever episode happened to be watched first.
     */
    fun continueWatching(): Flow<List<ContinueWatchingItem>> = store.progress.map { positions ->
        positions.entries
            .sortedByDescending { it.value.updatedAtMillis }
            .mapNotNull { (id, position) -> position.toContinueWatchingItem(id) }
    }

    suspend fun setInWatchlist(id: MediaId, saved: Boolean) =
        store.setInWatchlist(id.value, saved)

    suspend fun recordProgress(id: MediaId, fraction: Float, resumePoint: ResumePoint) = store.setProgress(
        id.value,
        PlaybackPosition(
            fraction = fraction,
            updatedAtMillis = System.currentTimeMillis(),
            title = resumePoint.title,
            mediaType = resumePoint.type.name,
            posterKey = resumePoint.posterKey,
            backdropKey = resumePoint.backdropKey,
            runtimeMinutes = resumePoint.runtimeMinutes,
            episodeId = resumePoint.episodeId,
            episodeSeason = resumePoint.episodeSeason,
            episodeNumber = resumePoint.episodeNumber,
            episodeTitle = resumePoint.episodeTitle,
        ),
    )

    suspend fun removeFromContinueWatching(id: MediaId) = store.removeProgress(id.value)

    private fun PlaybackPosition.toContinueWatchingItem(id: String): ContinueWatchingItem? {
        val type = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
        return ContinueWatchingItem(
            id = MediaId(id),
            type = type,
            title = title,
            posterKey = posterKey,
            backdropKey = backdropKey,
            fraction = fraction,
            episodeId = episodeId,
            episodeSeason = episodeSeason,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            remainingMinutes = runtimeMinutes?.let { total ->
                ((1f - fraction) * total).toInt().coerceAtLeast(1)
            },
        )
    }
}
