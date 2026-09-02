package tv.mango.app.addon

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.protocol.ProtocolFailure
import tv.mango.app.addon.protocol.StremioProtocolClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Routing and fan-out.
 *
 * The behaviour being pinned here is the one the whole design rests on: that a
 * single broken add-on costs its own result and nothing else. Everything else
 * in the application is built on the assumption that this holds.
 */
class AddonManagerTest {

    private fun manager(store: AddonStore) =
        AddonManager(store, StremioProtocolClient(OkHttpClient()))

    // --------------------------------------------------------------- routing

    @Test
    fun `only add-ons declaring the resource are asked`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("streams-only", "https://a.test", resources = listOf("stream")),
                testAddon("meta-only", "https://b.test", resources = listOf("meta")),
            ),
        )

        val capable = manager(store).capableOf(AddonResourceName.STREAM)

        assertEquals(listOf("streams-only"), capable.map { it.id })
    }

    @Test
    fun `add-ons that do not serve the content type are not asked`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("movies", "https://a.test", types = listOf("movie")),
                testAddon("series", "https://b.test", types = listOf("series")),
            ),
        )

        assertEquals(
            listOf("series"),
            manager(store).capableOf(AddonResourceName.STREAM, "series").map { it.id },
        )
    }

    @Test
    fun `add-ons that do not recognise the identifier are not asked`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("imdb", "https://a.test", idPrefixes = listOf("tt")),
                testAddon("kitsu", "https://b.test", idPrefixes = listOf("kitsu")),
                testAddon("anything", "https://c.test", idPrefixes = emptyList()),
            ),
        )

        val capable = manager(store)
            .capableOf(AddonResourceName.META, "movie", "tt0111161")

        // The one declaring no prefixes is asked: refusing to ask is worse than
        // one wasted request.
        assertEquals(listOf("imdb", "anything"), capable.map { it.id })
    }

    @Test
    fun `disabled add-ons are never asked`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("on", "https://a.test"),
                testAddon("off", "https://b.test", enabled = false),
            ),
        )

        assertEquals(listOf("on"), manager(store).capableOf(AddonResourceName.CATALOG).map { it.id })
    }

    @Test
    fun `add-ons are returned in priority order`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("third", "https://c.test", priority = 2),
                testAddon("first", "https://a.test", priority = 0),
                testAddon("second", "https://b.test", priority = 1),
            ),
        )

        assertEquals(
            listOf("first", "second", "third"),
            manager(store).capableOf(AddonResourceName.CATALOG).map { it.id },
        )
    }

    // --------------------------------------------------------------- fan-out

    @Test
    fun `one failing add-on does not affect the others`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("good-a", "https://a.test"),
                testAddon("broken", "https://b.test"),
                testAddon("good-b", "https://c.test"),
            ),
        )
        val addons = manager(store).capableOf(AddonResourceName.STREAM)

        val result = manager(store).fanOut(addons) { addon ->
            if (addon.id == "broken") {
                AddonReply.Failed(ProtocolFailure.SERVER_ERROR)
            } else {
                AddonReply.Answered(listOf("${addon.id}-item"))
            }
        }

        assertEquals(listOf("good-a-item", "good-b-item"), result.items.sorted())
        assertEquals(listOf("broken"), result.failed.map { it.id })
        assertTrue(result.isPartial)
        assertFalse(result.isTotalFailure)
    }

    @Test
    fun `an add-on that throws is contained rather than propagated`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("good", "https://a.test"),
                testAddon("throws", "https://b.test"),
            ),
        )
        val addons = manager(store).capableOf(AddonResourceName.STREAM)

        // An add-on can return anything at all, including something that makes
        // a parser throw. That must not escape into the calling screen.
        val result = manager(store).fanOut(addons) { addon ->
            if (addon.id == "throws") error("this add-on is broken")
            AddonReply.Answered(listOf("ok"))
        }

        assertEquals(listOf("ok"), result.items)
        assertEquals(listOf("throws"), result.failed.map { it.id })
    }

    @Test
    fun `an add-on that hangs is timed out without holding up the rest`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("fast", "https://a.test"),
                testAddon("hangs", "https://b.test"),
            ),
        )
        val addons = manager(store).capableOf(AddonResourceName.STREAM)

        val result = manager(store).fanOut(addons, timeoutMillis = 50) { addon ->
            if (addon.id == "hangs") delay(10_000)
            AddonReply.Answered(listOf(addon.id))
        }

        assertEquals(listOf("fast"), result.items)
        assertEquals(listOf("hangs"), result.failed.map { it.id })
    }

    @Test
    fun `everything failing is reported as a total failure`() = runTest {
        val store = FakeAddonStore(listOf(testAddon("a", "https://a.test")))
        val addons = manager(store).capableOf(AddonResourceName.STREAM)

        val result = manager(store).fanOut(addons) {
            AddonReply.Failed(ProtocolFailure.UNREACHABLE)
        }

        assertTrue(result.isTotalFailure)
        assertFalse(result.isPartial)
    }

    @Test
    fun `asking nobody is not a failure`() = runTest {
        val result = manager(FakeAddonStore()).fanOut(emptyList<tv.mango.app.addon.model.Addon>()) {
            AddonReply.Answered(listOf("unreachable"))
        }

        assertTrue(result.items.isEmpty())
        assertFalse(result.isTotalFailure)
    }

    // ---------------------------------------------------------- first answer

    @Test
    fun `the first add-on to answer wins and the rest are not asked`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("first", "https://a.test", priority = 0),
                testAddon("second", "https://b.test", priority = 1),
            ),
        )
        val addons = manager(store).capableOf(AddonResourceName.META)
        val asked = mutableListOf<String>()

        val answer = manager(store).firstAnswer(addons) { addon ->
            asked += addon.id
            "answer-from-${addon.id}"
        }

        assertEquals("answer-from-first", answer?.second)
        assertEquals(listOf("first"), asked)
    }

    @Test
    fun `a silent add-on falls through to the next`() = runTest {
        val store = FakeAddonStore(
            listOf(
                testAddon("silent", "https://a.test", priority = 0),
                testAddon("answers", "https://b.test", priority = 1),
            ),
        )
        val addons = manager(store).capableOf(AddonResourceName.META)

        val answer = manager(store).firstAnswer(addons) { addon ->
            if (addon.id == "silent") null else "found"
        }

        assertEquals("found", answer?.second)
        assertEquals("answers", answer?.first?.id)
    }
}
