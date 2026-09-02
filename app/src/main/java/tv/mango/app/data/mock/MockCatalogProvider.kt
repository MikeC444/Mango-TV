package tv.mango.app.data.mock

import tv.mango.app.data.DataResult
import tv.mango.app.data.FailureReason
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType

/**
 * The bundled catalogue, standing in for a content service.
 *
 * Titles are invented for this project. They exist so that focus, scrolling,
 * layout and image memory can be exercised against realistic volumes and
 * realistic string lengths before any network is involved - a long title has to
 * be seen wrapping in a real row, not imagined.
 *
 * Callers only ever hold [CatalogProvider], so replacing this with a real
 * service changes nothing above it.
 */
class MockCatalogProvider(
    private val source: MockCatalogSource,
) : CatalogProvider {

    override suspend fun homeRows(): DataResult<HomeContent> {
        val catalogue = source.catalogue() ?: return DataResult.Failure(FailureReason.UNKNOWN)
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
        val catalogue = source.catalogue() ?: return DataResult.Failure(FailureReason.UNKNOWN)
        val all = catalogue.byType(type)
        val from = page * pageSize
        if (from >= all.size) return DataResult.Success(emptyList())
        return DataResult.Success(all.subList(from, minOf(from + pageSize, all.size)))
    }

    override suspend fun title(id: MediaId): DataResult<MediaItem> {
        val catalogue = source.catalogue() ?: return DataResult.Failure(FailureReason.UNKNOWN)
        return catalogue.byId[id]
            ?.let { DataResult.Success(it) }
            ?: DataResult.Failure(FailureReason.NOT_FOUND)
    }
}
