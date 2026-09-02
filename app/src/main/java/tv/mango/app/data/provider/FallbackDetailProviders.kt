package tv.mango.app.data.provider

import tv.mango.app.data.DataResult
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.TitleDetail

/**
 * Detail from whichever source recognises the title.
 *
 * The bundled catalogue is asked first because it answers from memory and
 * declines anything it does not hold, so a bundled title costs no network and
 * an add-on title costs one failed map lookup. A title saved to the library
 * before any add-on was installed still opens afterwards, and vice versa.
 */
class FallbackMovieProvider(
    private val addons: MovieProvider,
    private val bundled: MovieProvider,
) : MovieProvider {

    override suspend fun movie(id: MediaId): DataResult<TitleDetail> {
        bundled.movie(id).let { if (it is DataResult.Success) return it }
        return addons.movie(id)
    }
}

class FallbackSeriesProvider(
    private val addons: SeriesProvider,
    private val bundled: SeriesProvider,
) : SeriesProvider {

    override suspend fun series(id: MediaId): DataResult<TitleDetail> {
        bundled.series(id).let { if (it is DataResult.Success) return it }
        return addons.series(id)
    }

    override suspend fun episodes(id: MediaId, season: Int): DataResult<List<Episode>> {
        bundled.episodes(id, season).let { result ->
            if (result is DataResult.Success && result.value.isNotEmpty()) return result
        }
        return addons.episodes(id, season)
    }
}
