package tv.mango.app.data.provider

import tv.mango.app.addon.Aggregated
import tv.mango.app.addon.CatalogResolver
import tv.mango.app.addon.MetadataResolver
import tv.mango.app.addon.StreamResolver
import tv.mango.app.addon.SubtitleResolver
import tv.mango.app.addon.model.StreamResult
import tv.mango.app.addon.model.SubtitleResult
import tv.mango.app.data.DataResult
import tv.mango.app.data.FailureReason
import tv.mango.app.models.ContentRow
import tv.mango.app.models.Episode
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.models.TitleDetail

/**
 * Add-ons, presented as an ordinary content source.
 *
 * This is where the add-on ecosystem stops. It implements the same interfaces
 * the bundled catalogue does, so every screen above it is unchanged and none of
 * them knows the protocol exists. Adding Stremio support required no new
 * content-provider architecture, because the seam for exactly this was already
 * the shape of the application.
 */
class AddonCatalogProvider(
    private val catalogs: CatalogResolver,
    private val metadata: MetadataResolver,
) : CatalogProvider {

    override suspend fun homeRows(): DataResult<HomeContent> {
        val resolved = catalogs.homeRows()
        val rows = resolved.items.map { it.row }
        if (rows.isEmpty()) return resolved.toFailure()

        // The featured title is the first card of the first row. Add-ons have
        // no notion of a featured title, and the top of the highest-priority
        // catalogue is the closest honest equivalent.
        val featured = rows.firstNotNullOfOrNull { it.items.firstOrNull() }
            ?: return DataResult.Failure(FailureReason.NOT_FOUND)

        return DataResult.Success(HomeContent(featured = featured, rows = rows))
    }

    override suspend fun browse(
        type: MediaType,
        page: Int,
        pageSize: Int,
    ): DataResult<List<MediaItem>> {
        val resolved = catalogs.browse(type, skip = page * pageSize, limit = pageSize)
        // A later page returning nothing is the end of the collection, not a
        // failure. Only an unanswered first page is worth reporting as one.
        if (resolved.items.isEmpty() && resolved.isTotalFailure && page == 0) {
            return resolved.toFailure()
        }
        return DataResult.Success(resolved.items.distinctBy { it.id.value })
    }

    override suspend fun title(id: MediaId): DataResult<MediaItem> {
        // Types are tried in turn because an identifier alone does not say
        // which it is, and the protocol addresses metadata by both.
        MediaType.entries.forEach { type ->
            metadata.detail(id, type)?.let { return DataResult.Success(it.item) }
        }
        return DataResult.Failure(FailureReason.NOT_FOUND)
    }
}

/** Detail for one title, from add-ons. */
class AddonDetailProvider(
    private val metadata: MetadataResolver,
) : MovieProvider, SeriesProvider {

    override suspend fun movie(id: MediaId): DataResult<TitleDetail> =
        metadata.detail(id, MediaType.MOVIE)
            ?.let { DataResult.Success(it) }
            ?: DataResult.Failure(FailureReason.NOT_FOUND)

    override suspend fun series(id: MediaId): DataResult<TitleDetail> =
        metadata.detail(id, MediaType.SERIES)
            ?.let { DataResult.Success(it) }
            ?: DataResult.Failure(FailureReason.NOT_FOUND)

    override suspend fun episodes(id: MediaId, season: Int): DataResult<List<Episode>> =
        DataResult.Success(metadata.episodes(id, season))
}

/**
 * Streams, from every enabled add-on that advertises the resource for this
 * content type - ranked, not narrowed to one, so the interface can offer the
 * viewer a choice or simply take the first.
 */
class AddonStreamProvider(
    private val resolver: StreamResolver,
) : StreamProvider {

    override suspend fun streams(item: MediaItem, episode: Episode?): DataResult<List<StreamResult>> {
        val videoId = MediaId(episode?.id ?: item.id.value)
        val resolved = resolver.streams(videoId, item.type)
        if (resolved.items.isEmpty()) return resolved.toFailure()
        return DataResult.Success(resolved.items)
    }
}

/**
 * Subtitles, from every enabled add-on that advertises the resource.
 *
 * No subtitles found is not a failure worth reporting the way no streams
 * found is: most titles have none available and a viewer expects to watch
 * without them far more often than they expect to fail to watch at all.
 */
class AddonSubtitleProvider(
    private val resolver: SubtitleResolver,
) : SubtitleProvider {

    override suspend fun subtitles(
        item: MediaItem,
        episode: Episode?,
        fromStream: List<SubtitleResult>,
    ): DataResult<List<SubtitleResult>> {
        val videoId = MediaId(episode?.id ?: item.id.value)
        return DataResult.Success(resolver.subtitles(videoId, item.type, fromStream).items)
    }
}

/**
 * Distinguishes nobody answering from nobody being asked.
 *
 * An unreachable add-on is a network failure the viewer can retry; having no
 * add-on able to answer is not an error at all, and offering a retry button for
 * it would be offering to do the same nothing again.
 */
private fun <T> Aggregated<T>.toFailure(): DataResult.Failure = when {
    isTotalFailure -> DataResult.Failure(FailureReason.NETWORK)
    else -> DataResult.Failure(FailureReason.NOT_FOUND)
}

/**
 * The application's content, from wherever it comes.
 *
 * Add-ons are preferred once the user has installed and enabled one; the
 * bundled catalogue stands in until then, so a fresh install is a working
 * application rather than an empty shell, and a configured one is not cluttered
 * with invented titles.
 *
 * Lookups by identifier consult every source in turn regardless, because a
 * title saved to the library before an add-on was installed must still resolve
 * afterwards. Sources decline identifiers they do not recognise by returning
 * NOT_FOUND, so this needs no scheme of its own to tell them apart.
 */
class CompositeCatalogProvider(
    private val addons: CatalogProvider,
    private val bundled: CatalogProvider,
    private val hasEnabledAddons: suspend () -> Boolean,
) : CatalogProvider {

    override suspend fun homeRows(): DataResult<HomeContent> = preferred { it.homeRows() }

    override suspend fun browse(
        type: MediaType,
        page: Int,
        pageSize: Int,
    ): DataResult<List<MediaItem>> = preferred { it.browse(type, page, pageSize) }

    override suspend fun title(id: MediaId): DataResult<MediaItem> {
        // Order matters only for speed here: the bundled catalogue answers
        // instantly and declines anything it does not hold.
        bundled.title(id).let { if (it is DataResult.Success) return it }
        return addons.title(id)
    }

    /**
     * Runs [request] against add-ons where there are any, and falls back to the
     * bundled catalogue when add-ons produce nothing usable - which covers a
     * fresh install, every add-on disabled, and every add-on being unreachable
     * at once. In each of those a viewer is better served by something than by
     * an error.
     */
    private suspend fun <T> preferred(
        request: suspend (CatalogProvider) -> DataResult<T>,
    ): DataResult<T> {
        if (!hasEnabledAddons()) return request(bundled)
        val fromAddons = request(addons)
        if (fromAddons is DataResult.Success) return fromAddons
        return request(bundled)
    }
}
