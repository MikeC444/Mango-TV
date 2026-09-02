package tv.mango.app.data.mock

import kotlinx.coroutines.delay
import tv.mango.app.data.DataResult
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaImages
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType

/**
 * Bundled content, standing in for a real catalogue.
 *
 * Titles here are invented for this project. They exist so focus, navigation,
 * layout and memory behaviour can be exercised against realistic volumes and
 * realistic string lengths before any network is involved - a long title has to
 * be seen wrapping in a real row, not imagined.
 *
 * The catalogue moves into a bundled JSON asset with artwork in the next phase.
 * Callers see no difference: they only ever hold the interface.
 */
class MockCatalogProvider : CatalogProvider {

    override suspend fun homeRows(): DataResult<List<ContentRow>> {
        // A provider is asynchronous by contract. Answering instantly here
        // would let a screen accidentally depend on data being ready during
        // its first layout pass, and that assumption would break the day a
        // real network sits behind this.
        delay(SIMULATED_LATENCY_MS)
        return DataResult.Success(ROWS)
    }

    override suspend fun browse(
        type: MediaType,
        page: Int,
        pageSize: Int,
    ): DataResult<List<MediaItem>> {
        delay(SIMULATED_LATENCY_MS)
        val all = CATALOGUE.filter { it.type == type }
        val from = page * pageSize
        if (from >= all.size) return DataResult.Success(emptyList())
        return DataResult.Success(all.subList(from, minOf(from + pageSize, all.size)))
    }

    private companion object {

        const val SIMULATED_LATENCY_MS = 120L

        private fun movie(id: String, title: String, year: Int) = MediaItem(
            id = MediaId(id),
            type = MediaType.MOVIE,
            title = title,
            year = year,
            images = MediaImages(poster = "poster_$id", backdrop = "backdrop_$id"),
        )

        private fun series(id: String, title: String, year: Int) = MediaItem(
            id = MediaId(id),
            type = MediaType.SERIES,
            title = title,
            year = year,
            images = MediaImages(poster = "poster_$id", backdrop = "backdrop_$id"),
        )

        val CATALOGUE = listOf(
            movie("m01", "The Salt Road", 2024),
            movie("m02", "Northern Lights", 2023),
            movie("m03", "A Quiet Inheritance", 2025),
            movie("m04", "Ninety Miles of Water", 2022),
            movie("m05", "The Cartographer", 2024),
            movie("m06", "Slow Burning Season", 2021),
            movie("m07", "Harbour", 2025),
            movie("m08", "The Weight of Small Things", 2023),
            movie("m09", "Meridian", 2024),
            movie("m10", "Every Room Facing West", 2022),
            movie("m11", "Cold Open", 2025),
            movie("m12", "The Long Field", 2020),
            movie("m13", "Ash and Amber", 2024),
            movie("m14", "Signal Hill", 2023),
            movie("m15", "The Second Summer", 2025),
            movie("m16", "Undertow", 2021),
            series("s01", "The Glasshouse", 2024),
            series("s02", "Provenance", 2023),
            series("s03", "Low Country", 2025),
            series("s04", "The Understudy", 2022),
            series("s05", "Nightjar", 2024),
            series("s06", "Continental Drift", 2023),
            series("s07", "The Reading Room", 2025),
            series("s08", "Fathom", 2021),
            series("s09", "Quarter Light", 2024),
            series("s10", "The Border Trilogy", 2022),
            series("s11", "Estuary", 2025),
            series("s12", "Foundry", 2023),
        )

        private val movies = CATALOGUE.filter { it.type == MediaType.MOVIE }
        private val series = CATALOGUE.filter { it.type == MediaType.SERIES }

        val ROWS = listOf(
            ContentRow("continue", "Continue Watching", movies.take(5) + series.take(2)),
            ContentRow("trending", "Trending Now", CATALOGUE.shuffled(java.util.Random(7)).take(12)),
            ContentRow("movies", "Popular Movies", movies),
            ContentRow("series", "Popular Series", series),
            ContentRow("recent", "Recently Added", CATALOGUE.takeLast(10)),
            ContentRow("recommended", "Recommended For You", CATALOGUE.shuffled(java.util.Random(19)).take(12)),
        )
    }
}
