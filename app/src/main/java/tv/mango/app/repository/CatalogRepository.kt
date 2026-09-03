package tv.mango.app.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import tv.mango.app.data.DataResult
import tv.mango.app.data.UiState
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.data.provider.MovieProvider
import tv.mango.app.data.provider.SeriesProvider
import tv.mango.app.models.Episode
import tv.mango.app.models.HomeContent
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.models.TitleDetail

/**
 * Turns provider results into the states a screen can render.
 *
 * The whole flow runs on [Dispatchers.IO] and emits Loading first, so no screen
 * has to arrange either for itself and none of this work can land on the main
 * thread. Collection is lifecycle-scoped by the caller, so a request is
 * cancelled when the screen that wanted it goes away.
 */
class CatalogRepository(
    private val catalog: CatalogProvider,
    private val movies: MovieProvider,
    private val series: SeriesProvider,
) {

    fun home(): Flow<UiState<HomeContent>> = flow {
        emit(UiState.Loading)
        emit(catalog.homeRows().toUiState { it.rows.isEmpty() })
    }.flowOn(Dispatchers.IO)

    /**
     * One page of a collection.
     *
     * A suspending call rather than a flow, because paging is driven by the
     * viewer reaching the end of what is loaded, not by anything the data layer
     * can observe. The screen that asked owns the accumulated list; emitting a
     * fresh flow per page would only push that bookkeeping down here.
     */
    suspend fun browsePage(
        type: MediaType,
        page: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): DataResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        catalog.browse(type, page, pageSize)
    }

    /** A single title, for a detail screen reached by identifier. */
    suspend fun title(id: MediaId): DataResult<MediaItem> = withContext(Dispatchers.IO) {
        catalog.title(id)
    }

    /**
     * Titles whose name contains [query], across both films and series.
     *
     * Composed entirely from [CatalogProvider.browse] rather than a dedicated
     * search resource: the provider seam has no search method of its own, and
     * giving it one - for an add-on-backed implementation that would mean
     * protocol support this application does not otherwise use - is a larger
     * change than a search screen needs. A bounded number of pages of each
     * type is fetched and matched by title instead, which is exact for the
     * bundled catalogue and still useful against a real one.
     */
    suspend fun search(query: String): DataResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        val needle = query.trim()
        if (needle.isEmpty()) return@withContext DataResult.Success(emptyList())
        scanByType { it.title.contains(needle, ignoreCase = true) }
    }

    /**
     * Other titles sharing at least one genre with [item], across both films
     * and series - the closest this catalogue can honestly offer to "similar
     * titles". There is no recommendation data or add-on capability behind a
     * real one, so this leans on the one signal every title already carries.
     */
    suspend fun similarTo(item: MediaItem): DataResult<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (item.genres.isEmpty()) return@withContext DataResult.Success(emptyList())
        scanByType { it.id != item.id && it.genres.any { genre -> genre in item.genres } }
    }

    /** Every title of every type matching [matches], scanned a bounded number of pages at a time. */
    private suspend fun scanByType(matches: (MediaItem) -> Boolean): DataResult<List<MediaItem>> {
        val found = mutableListOf<MediaItem>()
        for (type in MediaType.entries) {
            when (val result = pagedMatches(type, matches)) {
                is DataResult.Success -> found += result.value
                is DataResult.Failure -> return result
            }
        }
        return DataResult.Success(found)
    }

    /** Fails only when the very first page of [type] fails outright. */
    private suspend fun pagedMatches(
        type: MediaType,
        matches: (MediaItem) -> Boolean,
    ): DataResult<List<MediaItem>> {
        val found = mutableListOf<MediaItem>()
        for (page in 0 until SEARCH_PAGE_LIMIT) {
            val items = when (val result = catalog.browse(type, page, DEFAULT_PAGE_SIZE)) {
                is DataResult.Success -> result.value
                is DataResult.Failure -> if (page == 0) return result else break
            }
            if (items.isEmpty()) break
            found += items.filter(matches)
            if (items.size < DEFAULT_PAGE_SIZE) break
        }
        return DataResult.Success(found)
    }

    /**
     * Full detail for one title.
     *
     * Routed by type so films and series can be served by different systems
     * later without the detail screen needing to know that they are.
     */
    suspend fun detail(id: MediaId, type: MediaType): DataResult<TitleDetail> =
        withContext(Dispatchers.IO) {
            when (type) {
                MediaType.MOVIE -> movies.movie(id)
                MediaType.SERIES -> series.series(id)
            }
        }

    /** One season's episodes, loaded when that season is selected. */
    suspend fun episodes(id: MediaId, season: Int): DataResult<List<Episode>> =
        withContext(Dispatchers.IO) { series.episodes(id, season) }

    private inline fun <T> DataResult<T>.toUiState(isEmpty: (T) -> Boolean): UiState<T> =
        when (this) {
            is DataResult.Success -> if (isEmpty(value)) UiState.Empty else UiState.Content(value)
            is DataResult.Failure -> UiState.Error(reason)
        }

    companion object {
        /**
         * Large enough that a viewer holding right does not outrun it, small
         * enough that a page's artwork fits comfortably in the image cache.
         */
        const val DEFAULT_PAGE_SIZE = 24

        /** Pages of each type search will look through before giving up. */
        private const val SEARCH_PAGE_LIMIT = 4
    }
}
