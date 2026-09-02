package tv.mango.app.data.provider

import tv.mango.app.data.DataResult
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaItem

/**
 * Full detail for a single title.
 *
 * Kept apart from [CatalogProvider] because the two have genuinely different
 * shapes: a catalogue call returns many small projections, a detail call
 * returns one large record. A future provider may well serve them from
 * different endpoints, or one may be cached far longer than the other.
 *
 * The detail models these return arrive with the detail screens; the interfaces
 * are declared now so the seam exists before anything is built against it.
 */
interface MovieProvider {
    suspend fun movie(id: MediaId): DataResult<MediaItem>
}

interface SeriesProvider {
    suspend fun series(id: MediaId): DataResult<MediaItem>
}

interface SearchProvider {
    suspend fun search(query: String, page: Int): DataResult<List<MediaItem>>
}

/**
 * Resolves a title to something playable.
 *
 * Separate from every other provider because entitlement and playback are
 * usually a different system from metadata, and because a stream URL is often
 * short-lived while metadata is cacheable for days.
 */
interface StreamProvider {
    suspend fun stream(id: MediaId): DataResult<StreamSource>
}

data class StreamSource(
    val uri: String,
    val mimeType: String?,
)
