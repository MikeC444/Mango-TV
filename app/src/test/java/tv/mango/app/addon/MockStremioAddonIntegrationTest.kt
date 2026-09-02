package tv.mango.app.addon

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The whole path against a real add-on: install, browse, open, play, read
 * subtitles - nothing here is asserted against hand-built JSON, it all comes
 * from [MockStremioAddon] answering real HTTP requests the same way an
 * external Stremio-compatible service would.
 */
class MockStremioAddonIntegrationTest {

    private lateinit var addon: MockStremioAddon
    private lateinit var store: FakeAddonStore
    private lateinit var manager: AddonManager
    private lateinit var client: StremioProtocolClient

    @Before
    fun setUp() {
        addon = MockStremioAddon().apply { start() }
        store = FakeAddonStore()
        client = StremioProtocolClient(OkHttpClient())
        manager = AddonManager(store, client)
    }

    @After
    fun tearDown() {
        addon.shutdown()
    }

    private suspend fun install() {
        val installer = AddonInstaller(client, store)
        val preview = assertIs<AddonInstaller.Preview.Ready>(installer.preview(addon.manifestUrl))
        installer.install(preview)
    }

    @Test
    fun `installing reads the manifest, including a resource declared as an object`() = runBlocking {
        val installer = AddonInstaller(client, store)

        val preview = assertIs<AddonInstaller.Preview.Ready>(installer.preview(addon.manifestUrl))

        assertEquals("tv.mango.mock", preview.manifest.id)
        assertTrue(preview.manifest.supports(AddonResourceName.CATALOG))
        // "meta" was declared as an object narrowing types and id prefixes -
        // the bare-string resources on the same manifest still work alongside it.
        assertTrue(preview.manifest.supports(AddonResourceName.META, "movie"))
        assertTrue(preview.manifest.handlesId(AddonResourceName.META, "mock-movie-1"))
    }

    @Test
    fun `browsing returns every movie and every series in the mock library`() = runBlocking {
        install()
        val catalogs = CatalogResolver(manager, client)

        val movies = catalogs.browse(MediaType.MOVIE, skip = 0, limit = 10)
        val series = catalogs.browse(MediaType.SERIES, skip = 0, limit = 10)

        assertEquals(3, movies.items.size)
        assertEquals(1, series.items.size)
        assertEquals("First Light", movies.items.first { it.id.value == MockStremioAddon.MOVIE_1_ID }.title)
    }

    @Test
    fun `opening a movie returns its full metadata`() = runBlocking {
        install()
        val metadata = MetadataResolver(manager, client)

        val detail = metadata.detail(MediaId(MockStremioAddon.MOVIE_1_ID), MediaType.MOVIE)

        requireNotNull(detail)
        assertEquals("First Light", detail.item.title)
        assertEquals(2019, detail.item.year)
        assertEquals(128, detail.item.runtimeMinutes)
        assertEquals(listOf("Drama"), detail.item.genres)
        assertEquals(1, detail.cast.size)
    }

    @Test
    fun `opening a series returns its seasons, and episodes load a season at a time`() = runBlocking {
        install()
        val metadata = MetadataResolver(manager, client)
        val seriesId = MediaId(MockStremioAddon.SERIES_1_ID)

        val detail = metadata.detail(seriesId, MediaType.SERIES)
        val seasonOne = metadata.episodes(seriesId, season = 1)
        val seasonTwo = metadata.episodes(seriesId, season = 2)

        requireNotNull(detail)
        assertEquals(listOf(1, 2), detail.seasons.map { it.number })
        assertEquals(2, seasonOne.size)
        assertEquals(1, seasonTwo.size)
        assertEquals("Entry", seasonOne.first { it.number == 1 }.title)
    }

    @Test
    fun `playing a movie queries streams and ranks the highest quality first`() = runBlocking {
        install()
        val streams = StreamResolver(manager, client)

        val result = streams.streams(MediaId(MockStremioAddon.MOVIE_1_ID), MediaType.MOVIE)

        assertEquals(3, result.items.size)
        assertEquals(StreamQuality.UHD_4K, result.items.first().quality)
        assertTrue(result.items.all { it.providerId == "tv.mango.mock" })
    }

    @Test
    fun `playing one episode of a series queries that episode's streams`() = runBlocking {
        install()
        val streams = StreamResolver(manager, client)
        val episodeId = MediaId("${MockStremioAddon.SERIES_1_ID}:1:1")

        val result = streams.streams(episodeId, MediaType.SERIES)

        assertEquals(1, result.items.size)
    }

    @Test
    fun `subtitles for a movie come back in every language the add-on offers`() = runBlocking {
        install()
        val subtitles = SubtitleResolver(manager, client)

        val result = subtitles.subtitles(MediaId(MockStremioAddon.MOVIE_1_ID), MediaType.MOVIE)

        assertEquals(setOf("en", "es"), result.items.map { it.language }.toSet())
    }
}
