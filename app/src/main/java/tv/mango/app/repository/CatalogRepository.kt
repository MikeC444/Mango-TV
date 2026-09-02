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
    }
}
