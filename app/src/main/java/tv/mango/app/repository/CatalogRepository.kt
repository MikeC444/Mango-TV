package tv.mango.app.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import tv.mango.app.data.DataResult
import tv.mango.app.data.UiState
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType

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
) {

    fun homeRows(): Flow<UiState<List<ContentRow>>> = flow {
        emit(UiState.Loading)
        emit(catalog.homeRows().toUiState { it.isEmpty() })
    }.flowOn(Dispatchers.IO)

    fun browse(type: MediaType, page: Int, pageSize: Int = DEFAULT_PAGE_SIZE): Flow<UiState<List<MediaItem>>> = flow {
        emit(UiState.Loading)
        emit(catalog.browse(type, page, pageSize).toUiState { it.isEmpty() })
    }.flowOn(Dispatchers.IO)

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
