package tv.mango.app.data.provider

import tv.mango.app.data.DataResult
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaItem
import tv.mango.app.models.TitleDetail

/**
 * Full detail for a single title.
 *
 * Kept apart from [CatalogProvider] because the two have genuinely different
 * shapes: a catalogue call returns many small projections, a detail call
 * returns one large record. A real deployment may well serve them from
 * different systems, and cache one far longer than the other.
 */
interface MovieProvider {
    suspend fun movie(id: MediaId): DataResult<TitleDetail>
}

interface SeriesProvider {
    suspend fun series(id: MediaId): DataResult<TitleDetail>

    /**
     * Episodes for one season, not for the whole run.
     *
     * A long-running series is thousands of records; loading a season at a time
     * is the difference between a detail screen that opens immediately and one
     * that stalls on a title nobody asked to see all of.
     */
    suspend fun episodes(id: MediaId, season: Int): DataResult<List<Episode>>
}

interface SearchProvider {
    suspend fun search(query: String, page: Int): DataResult<List<MediaItem>>
}

/**
 * Resolves a title to something playable.
 *
 * Separate from every other provider because entitlement and playback are
 * usually a different system from metadata, and because a stream URL is often
 * short-lived where metadata is cacheable for days.
 */
interface StreamProvider {
    suspend fun stream(id: MediaId): DataResult<StreamSource>
}

data class StreamSource(
    val uri: String,
    val mimeType: String?,
)
