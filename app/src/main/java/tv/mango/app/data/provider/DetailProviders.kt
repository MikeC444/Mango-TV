package tv.mango.app.data.provider

import tv.mango.app.addon.model.StreamResult
import tv.mango.app.addon.model.SubtitleResult
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
 * Resolves a title, or one episode of it, to everything playable.
 *
 * Separate from every other provider because entitlement and playback are
 * usually a different system from metadata, and because a stream is often
 * short-lived where metadata is cacheable for days. Returns every stream any
 * enabled add-on offered, already ranked - the player chooses among them, or
 * offers the choice to the viewer, without ever learning which add-on any of
 * them came from beyond what [StreamResult.providerName] already carries for
 * the interface to show.
 */
interface StreamProvider {
    suspend fun streams(item: MediaItem, episode: Episode? = null): DataResult<List<StreamResult>>
}

/** Subtitles for a title, or one episode of it, from every add-on that has them. */
interface SubtitleProvider {
    suspend fun subtitles(item: MediaItem, episode: Episode? = null): DataResult<List<SubtitleResult>>
}
