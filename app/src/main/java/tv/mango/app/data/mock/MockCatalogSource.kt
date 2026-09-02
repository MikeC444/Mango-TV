package tv.mango.app.data.mock

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import tv.mango.app.models.CastMember
import tv.mango.app.models.ContentRow
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaImages
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.utilities.Logger

/**
 * Reads and holds the bundled content.
 *
 * Shared by the catalogue and detail providers so the assets are parsed once
 * between them rather than once each.
 *
 * The two assets are loaded independently and on demand. The browse catalogue
 * is small and is needed the moment the application opens; the detail payload
 * is several times the size, is only needed once a viewer opens a title, and
 * many sessions will never touch it. Loading them together would put the whole
 * of it on the cold-start path for no reason - and a real content service would
 * impose the same split anyway, so the screens are built against the shape they
 * will actually have.
 */
class MockCatalogSource(
    private val context: Context,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val catalogLock = Mutex()
    private val detailLock = Mutex()

    @Volatile
    private var catalog: Catalogue? = null

    @Volatile
    private var details: Map<MediaId, TitleDetailData>? = null

    suspend fun catalogue(): Catalogue? {
        catalog?.let { return it }
        return catalogLock.withLock {
            // Checked again inside the lock: several screens can ask at once on
            // a cold start, and only the first should do the work.
            catalog ?: withContext(Dispatchers.IO) { parseCatalogue() }?.also { catalog = it }
        }
    }

    suspend fun detail(id: MediaId): TitleDetailData? {
        details?.let { return it[id] }
        val loaded = detailLock.withLock {
            details ?: withContext(Dispatchers.IO) { parseDetails() }?.also { details = it }
        }
        return loaded?.get(id)
    }

    private fun parseCatalogue(): Catalogue? = try {
        val parsed = json.decodeFromString(
            CatalogJson.serializer(),
            readAsset(CATALOG_ASSET),
        )

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

    private fun parseDetails(): Map<MediaId, TitleDetailData>? = try {
        val parsed = json.decodeFromString(
            DetailsJson.serializer(),
            readAsset(DETAILS_ASSET),
        )
        parsed.details.mapKeys { (id, _) -> MediaId(id) }
            .mapValues { (id, entry) ->
                TitleDetailData(
                    cast = entry.cast.map { CastMember(name = it.name, role = it.role) },
                    episodes = entry.episodes.map { episode ->
                        Episode(
                            id = episode.id,
                            seriesId = id,
                            season = episode.season,
                            number = episode.number,
                            title = episode.title,
                            synopsis = episode.synopsis,
                            runtimeMinutes = episode.runtimeMinutes,
                            // Stills would come from a real provider; the
                            // series' own backdrop stands in.
                            thumbnail = "backdrop_${id.value}",
                        )
                    },
                )
            }
    } catch (error: Exception) {
        Logger.e("Could not read the bundled detail payload", error)
        null
    }

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    class Catalogue(
        val byId: Map<MediaId, MediaItem>,
        val rows: List<ContentRow>,
        val featured: List<MediaItem>,
    ) {
        private val ordered = byId.values.toList()

        fun byType(type: MediaType): List<MediaItem> = ordered.filter { it.type == type }
    }

    class TitleDetailData(
        val cast: List<CastMember>,
        val episodes: List<Episode>,
    )

    private companion object {
        const val CATALOG_ASSET = "mock_catalog.json"
        const val DETAILS_ASSET = "mock_details.json"
    }
}
