package tv.mango.app.addon

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Metadata resolution: one winning answer, and falling back to a second
 * provider only where the first left a gap a viewer would notice.
 *
 * `runBlocking`, not `runTest` - see the note on StreamResolverTest for why.
 */
class MetadataResolverTest {

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

    private fun resolver(store: AddonStore): MetadataResolver {
        val client = StremioProtocolClient(OkHttpClient())
        return MetadataResolver(AddonManager(store, client), client)
    }

    private fun baseUrl(server: MockWebServer) = server.url("/").toString().trimEnd('/')

    @Test
    fun `a complete answer from the first provider is not supplemented`() = runBlocking {
        serverA.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "meta": { "id": "tt1", "type": "movie", "name": "A",
                    "description": "Complete.",
                    "credits_cast": [{ "name": "Someone", "character": "Lead" }] } }""",
            ),
        )
        val store = FakeAddonStore(
            listOf(
                testAddon("first", baseUrl(serverA), priority = 0),
                testAddon("second", baseUrl(serverB), priority = 1),
            ),
        )

        val detail = resolver(store).detail(MediaId("tt1"), MediaType.MOVIE)

        requireNotNull(detail)
        assertEquals("Complete.", detail.item.synopsis)
        assertEquals(0, serverB.requestCount)
    }

    @Test
    fun `a gap in the first provider's answer is filled from the second`() = runBlocking {
        serverA.enqueue(
            MockResponse().setResponseCode(200).setBody(
                // No description and no cast: a gap a viewer would notice.
                """{ "meta": { "id": "tt1", "type": "movie", "name": "A" } }""",
            ),
        )
        serverB.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "meta": { "id": "tt1", "type": "movie", "name": "A",
                    "description": "From the second provider.",
                    "credits_cast": [{ "name": "Someone", "character": "Lead" }] } }""",
            ),
        )
        val store = FakeAddonStore(
            listOf(
                testAddon("first", baseUrl(serverA), priority = 0),
                testAddon("second", baseUrl(serverB), priority = 1),
            ),
        )

        val detail = resolver(store).detail(MediaId("tt1"), MediaType.MOVIE)

        requireNotNull(detail)
        // The name came from the first, higher-priority provider...
        assertEquals("A", detail.item.title)
        // ...but the gaps it left were filled from the second.
        assertEquals("From the second provider.", detail.item.synopsis)
        assertEquals(1, detail.cast.size)
    }

    @Test
    fun `a silent provider falls through to the next`() = runBlocking {
        // "first" is capable but never answers - a dead port stays refused.
        val dead = MockWebServer().apply { start() }
        val deadUrl = baseUrl(dead)
        dead.shutdown()

        serverB.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "meta": { "id": "tt1", "type": "movie", "name": "From the second" } }""",
            ),
        )
        val store = FakeAddonStore(
            listOf(
                testAddon("first", deadUrl, priority = 0),
                testAddon("second", baseUrl(serverB), priority = 1),
            ),
        )

        val detail = resolver(store).detail(MediaId("tt1"), MediaType.MOVIE)

        requireNotNull(detail)
        assertEquals("From the second", detail.item.title)
    }

    @Test
    fun `nobody able to answer is a null detail, not an error`() = runBlocking {
        val store = FakeAddonStore(
            listOf(testAddon("catalog-only", baseUrl(serverA), resources = listOf("catalog"))),
        )

        val detail = resolver(store).detail(MediaId("tt1"), MediaType.MOVIE)

        assertNull(detail)
    }

    @Test
    fun `episodes are read a season at a time from the same series metadata`() = runBlocking {
        serverA.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{ "meta": { "id": "s1", "type": "series", "name": "A Series",
                        "videos": [
                            { "id": "s1:1:1", "season": 1, "episode": 1, "name": "One" },
                            { "id": "s1:1:2", "season": 1, "episode": 2, "name": "Two" },
                            { "id": "s1:2:1", "season": 2, "episode": 1, "name": "Three" }
                        ] } }""",
                )
        }
        val store = FakeAddonStore(listOf(testAddon("addon", baseUrl(serverA))))

        val seasonOne = resolver(store).episodes(MediaId("s1"), season = 1)
        val seasonTwo = resolver(store).episodes(MediaId("s1"), season = 2)

        assertEquals(2, seasonOne.size)
        assertEquals(1, seasonTwo.size)
        assertTrue(seasonOne.all { it.season == 1 })
    }
}
