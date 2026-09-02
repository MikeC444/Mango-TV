package tv.mango.app.addon

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import tv.mango.app.addon.model.AddonCatalog
import tv.mango.app.addon.model.CatalogExtra
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.models.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Browsing and paging, against a real add-on.
 *
 * Uses [kotlinx.coroutines.runBlocking] rather than `runTest`: this exercises
 * [AddonManager.fanOut], which wraps every request in `withTimeoutOrNull`, and
 * that combination races `runTest`'s virtual clock against real network I/O -
 * see the equivalent note on StreamResolverTest.
 */
class CatalogResolverTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun resolver(store: AddonStore): CatalogResolver {
        val client = StremioProtocolClient(OkHttpClient())
        return CatalogResolver(AddonManager(store, client), client)
    }

    private fun baseUrl() = server.url("/").toString().trimEnd('/')

    private fun addonWithCatalog(catalog: AddonCatalog) = testAddon(
        id = "addon",
        baseUrl = baseUrl(),
        catalogs = listOf(catalog),
    )

    @Test
    fun `a later page is asked for when the catalogue advertises paging`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "metas": [{ "id": "tt2", "type": "movie", "name": "Page Two" }] }""",
            ),
        )
        val catalog = AddonCatalog(
            type = "movie",
            id = "top",
            extra = listOf(CatalogExtra(AddonCatalog.EXTRA_SKIP, isRequired = false)),
        )
        val store = FakeAddonStore(listOf(addonWithCatalog(catalog)))

        val result = resolver(store).browse(MediaType.MOVIE, skip = 20, limit = 10)

        assertEquals(1, result.items.size)
        assertEquals("Page Two", result.items.first().title)
    }

    @Test
    fun `a later page is not requested from a catalogue that cannot page`() = runBlocking {
        val catalog = AddonCatalog(type = "movie", id = "top", extra = emptyList())
        val store = FakeAddonStore(listOf(addonWithCatalog(catalog)))

        val result = resolver(store).browse(MediaType.MOVIE, skip = 20, limit = 10)

        assertTrue(result.items.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `an empty catalogue page is empty rather than an error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{ "metas": [] }"""))
        val store = FakeAddonStore(listOf(addonWithCatalog(AddonCatalog(type = "movie", id = "top"))))

        val result = resolver(store).browse(MediaType.MOVIE, skip = 0, limit = 10)

        assertTrue(result.items.isEmpty())
        assertTrue(!result.isTotalFailure)
    }

    @Test
    fun `a catalogue requiring an argument is skipped on the home screen`() = runBlocking {
        val requiresSearch = AddonCatalog(
            type = "movie",
            id = "search-only",
            extra = listOf(CatalogExtra("search", isRequired = true)),
        )
        val store = FakeAddonStore(listOf(addonWithCatalog(requiresSearch)))

        val result = resolver(store).homeRows()

        assertTrue(result.items.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `the home screen combines rows from every add-on's catalogues`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{ "metas": [{ "id": "tt1", "type": "movie", "name": "A" }] }""",
            ),
        )
        val store = FakeAddonStore(listOf(addonWithCatalog(AddonCatalog(type = "movie", id = "top"))))

        val result = resolver(store).homeRows()

        assertEquals(1, result.items.size)
        assertEquals(1, result.items.first().row.items.size)
    }
}
