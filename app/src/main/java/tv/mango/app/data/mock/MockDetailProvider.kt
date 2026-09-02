package tv.mango.app.data.mock

import tv.mango.app.data.DataResult
import tv.mango.app.data.FailureReason
import tv.mango.app.data.provider.MovieProvider
import tv.mango.app.data.provider.SeriesProvider
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.Season
import tv.mango.app.models.TitleDetail

/**
 * Detail for one title, from the bundled payload.
 *
 * Implements both provider interfaces because one bundled asset happens to
 * serve both. A real deployment might well split them - metadata and episode
 * guides often come from different systems, and are cacheable for very
 * different lengths of time - and the interfaces stay separate so that it can.
 */
class MockDetailProvider(
    private val source: MockCatalogSource,
) : MovieProvider, SeriesProvider {

    override suspend fun movie(id: MediaId): DataResult<TitleDetail> = detail(id)

    override suspend fun series(id: MediaId): DataResult<TitleDetail> = detail(id)

    override suspend fun episodes(id: MediaId, season: Int): DataResult<List<Episode>> {
        val detail = source.detail(id) ?: return DataResult.Failure(FailureReason.NOT_FOUND)
        return DataResult.Success(detail.episodes.filter { it.season == season })
    }

    private suspend fun detail(id: MediaId): DataResult<TitleDetail> {
        val catalogue = source.catalogue() ?: return DataResult.Failure(FailureReason.UNKNOWN)
        val item = catalogue.byId[id] ?: return DataResult.Failure(FailureReason.NOT_FOUND)
        val extra = source.detail(id)

        // A title with no detail entry still opens: the screen shows what the
        // catalogue knows and simply omits the cast row.
        val seasons = extra?.episodes
            .orEmpty()
            .groupBy { it.season }
            .map { (number, episodes) -> Season(number = number, episodeCount = episodes.size) }
            .sortedBy { it.number }

        return DataResult.Success(
            TitleDetail(
                item = item,
                cast = extra?.cast.orEmpty(),
                seasons = seasons,
            ),
        )
    }
}
