package tv.mango.app.addon

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Streams, gathered across add-ons and ranked.
 *
 * Two real servers rather than one, so each add-on's response is under this
 * test's own control instead of depending on the order concurrent requests
 * happen to arrive in.
 */
class StreamResolverTest {

    private lateinit var serverA: MockWebServer
    private lateinit var serverB: MockWebServer

    @Before
    fun setUp() {
        serverA = MockWebServer().apply { start() }
        serverB = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        serverA.shutdown()
        serverB.shutdown()
    }

    private fun resolver(store: AddonStore): StreamResolver {
        val client = StremioProtocolClient(OkHttpClient())
        return StreamResolver(AddonManager(store, client), client)
    }

    private fun baseUrl(server: MockWebServer) = server.url("/").toString().trimEnd('/')

    @Test
    fun `streams from every capable add-on are combined and ranked`() = runBlocking {
        serverA.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "streams": [{ "url": "https://a.test/720.mp4", "name": "720p" }] }""",
            ),
        )
        serverB.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "streams": [{ "url": "https://b.test/1080.mp4", "name": "1080p" }] }""",
            ),
        )
        val store = FakeAddonStore(
            listOf(
                testAddon("addon-a", baseUrl(serverA)),
                testAddon("addon-b", baseUrl(serverB)),
            ),
        )

        val result = resolver(store).streams(MediaId("tt1"), MediaType.MOVIE)

        assertEquals(2, result.items.size)
        // Ranked, not just concatenated: the higher-quality source leads.
        assertEquals(StreamQuality.FHD_1080, result.items.first().quality)
    }

    @Test
    fun `an unreachable add-on does not prevent another from returning streams`() = runBlocking {
        // Started and stopped so the port is real but nothing is listening on
        // it any more - a guaranteed connection refusal rather than a guess at
        // which unused port is safe to dial in a sandboxed CI runner.
        val dead = MockWebServer().apply { start() }
        val deadUrl = baseUrl(dead)
        dead.shutdown()

        serverB.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "streams": [{ "url": "https://b.test/1080.mp4" }] }""",
            ),
        )
        val store = FakeAddonStore(
            listOf(
                testAddon("addon-a", deadUrl),
                testAddon("addon-b", baseUrl(serverB)),
            ),
        )

        val result = resolver(store).streams(MediaId("tt1"), MediaType.MOVIE)

        assertEquals(1, result.items.size)
        assertTrue(result.isPartial)
    }

    @Test
    fun `an add-on that does not advertise streams is not asked`() = runBlocking {
        serverA.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "streams": [{ "url": "https://a.test/a.mp4" }] }""",
            ),
        )
        val store = FakeAddonStore(
            listOf(
                testAddon("streams", baseUrl(serverA), resources = listOf("stream")),
                testAddon("catalog-only", baseUrl(serverB), resources = listOf("catalog")),
            ),
        )

        resolver(store).streams(MediaId("tt1"), MediaType.MOVIE)

        assertEquals(0, serverB.requestCount)
    }

    @Test
    fun `no capable add-ons produces an empty result rather than an error`() = runBlocking {
        val store = FakeAddonStore(
            listOf(testAddon("catalog-only", baseUrl(serverA), resources = listOf("catalog"))),
        )

        val result = resolver(store).streams(MediaId("tt1"), MediaType.MOVIE)

        assertTrue(result.items.isEmpty())
        assertTrue(!result.isTotalFailure)
    }
}
