package tv.mango.app.data.mock

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import tv.mango.app.data.DataResult
import tv.mango.app.data.FailureReason
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.models.ContentRow
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaImages
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.utilities.Logger

/**
 * The bundled catalogue, standing in for a content service.
 *
 * Titles are invented for this project. They exist so that focus, scrolling,
 * layout and image memory can be exercised against realistic volumes and
 * realistic string lengths before any network is involved - a long title has to
 * be seen wrapping in a real row, not imagined.
 *
 * The asset is read and parsed once, off the main thread, behind a mutex so a
 * burst of concurrent calls on a cold start cannot each start their own parse.
 * Callers only ever hold [CatalogProvider], so replacing this with a real
 * service changes nothing above it.
 */
class MockCatalogProvider(
    private val context: Context,
) : CatalogProvider {

    private val json = Json { ignoreUnknownKeys = true }
    private val loadLock = Mutex()

    @Volatile
    private var cached: Catalogue? = null

    override suspend fun homeRows(): DataResult<HomeContent> {
        val catalogue = load() ?: return DataResult.Failure(FailureReason.UNKNOWN)
        // Falls back to the first title in the first row rather than throwing:
        // a catalogue with nothing marked featured is still a usable screen.
        val featured = catalogue.featured.firstOrNull()
            ?: catalogue.rows.firstOrNull()?.items?.firstOrNull()
            ?: return DataResult.Failure(FailureReason.NOT_FOUND)
        return DataResult.Success(HomeContent(featured = featured, rows = catalogue.rows))
    }

    override suspend fun browse(
        type: MediaType,
        page: Int,
        pageSize: Int,
    ): DataResult<List<MediaItem>> {
        val catalogue = load() ?: return DataResult.Failure(FailureReason.UNKNOWN)
        val all = catalogue.byType(type)
        val from = page * pageSize
        if (from >= all.size) return DataResult.Success(emptyList())
        return DataResult.Success(all.subList(from, minOf(from + pageSize, all.size)))
    }

    override suspend fun title(id: MediaId): DataResult<MediaItem> {
        val catalogue = load() ?: return DataResult.Failure(FailureReason.UNKNOWN)
        return catalogue.byId[id]
            ?.let { DataResult.Success(it) }
            ?: DataResult.Failure(FailureReason.NOT_FOUND)
    }

    private suspend fun load(): Catalogue? {
        cached?.let { return it }
        return loadLock.withLock {
            // Checked again inside the lock: several screens can ask at once on
            // a cold start, and only the first should do the work.
            cached ?: parse()?.also { cached = it }
        }
    }

    private fun parse(): Catalogue? = try {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString(CatalogJson.serializer(), text)

        val byId = parsed.titles.mapNotNull { title ->
            // An unrecognised type costs this one title, the same way a bad row
            // reference costs one card.
            val type = MediaType.entries.firstOrNull { it.name == title.type }
                ?: return@mapNotNull null
            val id = MediaId(title.id)
            id to MediaItem(
                id = id,
                type = type,
                title = title.title,
                year = title.year,
                runtimeMinutes = title.runtimeMinutes,
                certification = title.certification,
                genres = title.genres,
                synopsis = title.synopsis,
                images = MediaImages(
                    poster = "poster_${title.id}",
                    backdrop = "backdrop_${title.id}",
                ),
                progress = title.progress,
            )
        }.toMap()

        Catalogue(
            byId = byId,
            rows = parsed.rows.map { row ->
                ContentRow(
                    id = row.id,
                    title = row.title,
                    // mapNotNull rather than a lookup that can throw: one bad
                    // reference in the data should cost that one card, not the
                    // whole screen.
                    items = row.titleIds.mapNotNull { byId[MediaId(it)] },
                )
            },
            featured = parsed.featured.mapNotNull { byId[MediaId(it)] },
        )
    } catch (error: Exception) {
        // A malformed bundled asset is a build problem, not something the
        // viewer can act on. It surfaces as an ordinary failure state.
        Logger.e("Could not read the bundled catalogue", error)
        null
    }

    private class Catalogue(
        val byId: Map<MediaId, MediaItem>,
        val rows: List<ContentRow>,
        val featured: List<MediaItem>,
    ) {
        private val ordered = byId.values.toList()

        fun byType(type: MediaType): List<MediaItem> = ordered.filter { it.type == type }
    }

    private companion object {
        const val ASSET_NAME = "mock_catalog.json"
    }
}
