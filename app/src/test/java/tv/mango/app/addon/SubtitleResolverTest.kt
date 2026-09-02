package tv.mango.app.addon

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import tv.mango.app.addon.model.SubtitleResult
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleResolverTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun resolver(store: AddonStore): SubtitleResolver {
        val client = StremioProtocolClient(OkHttpClient())
        return SubtitleResolver(AddonManager(store, client), client)
    }

    private fun baseUrl() = server.url("/").toString().trimEnd('/')

    @Test
    fun `subtitles from the resource are returned`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "subtitles": [{ "id": "1", "url": "https://sub.test/en.srt", "lang": "en" }] }""",
            ),
        )
        val store = FakeAddonStore(listOf(testAddon("subs", baseUrl())))

        val result = resolver(store).subtitles(MediaId("tt1"), MediaType.MOVIE)

        assertEquals(1, result.items.size)
        assertEquals("en", result.items.first().language)
    }

    @Test
    fun `subtitles carried on the chosen stream are folded in`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "subtitles": [{ "id": "1", "url": "https://sub.test/en.srt", "lang": "en" }] }""",
            ),
        )
        val store = FakeAddonStore(listOf(testAddon("subs", baseUrl())))
        val fromStream = listOf(
            SubtitleResult(
                id = "2",
                url = "https://sub.test/fr.srt",
                language = "fr",
                label = "French",
                providerId = "other-addon",
                providerName = "Other",
            ),
        )

        val result = resolver(store).subtitles(MediaId("tt1"), MediaType.MOVIE, fromStream = fromStream)

        assertEquals(setOf("en", "fr"), result.items.map { it.language }.toSet())
    }

    @Test
    fun `a duplicate track from both sources is not shown twice`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "subtitles": [{ "id": "1", "url": "https://sub.test/en.srt", "lang": "en" }] }""",
            ),
        )
        val store = FakeAddonStore(listOf(testAddon("subs", baseUrl())))
        val fromStream = listOf(
            SubtitleResult(
                id = "1",
                url = "https://sub.test/en.srt",
                language = "en",
                label = "English",
                providerId = "subs",
                providerName = "subs",
            ),
        )

        val result = resolver(store).subtitles(MediaId("tt1"), MediaType.MOVIE, fromStream = fromStream)

        assertEquals(1, result.items.size)
    }

    @Test
    fun `no subtitles anywhere is an empty list, not a failure`() = runBlocking {
        val store = FakeAddonStore(
            listOf(testAddon("no-subs", baseUrl(), resources = listOf("catalog"))),
        )

        val result = resolver(store).subtitles(MediaId("tt1"), MediaType.MOVIE)

        assertTrue(result.items.isEmpty())
        assertTrue(!result.isTotalFailure)
    }
}
