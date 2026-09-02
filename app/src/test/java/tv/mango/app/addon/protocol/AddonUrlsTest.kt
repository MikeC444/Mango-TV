package tv.mango.app.addon.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddonUrlsTest {

    @Test
    fun `a bare host becomes an https manifest url`() {
        assertEquals(
            "https://example.test/manifest.json",
            AddonUrls.manifestUrl("example.test"),
        )
    }

    @Test
    fun `an existing manifest url is left alone`() {
        assertEquals(
            "https://example.test/manifest.json",
            AddonUrls.manifestUrl("https://example.test/manifest.json"),
        )
    }

    @Test
    fun `a configured add-on keeps its configuration segment`() {
        // The configuration travels inside the path, which is what lets
        // configured add-ons need no special handling anywhere else.
        assertEquals(
            "https://example.test/abc123config/manifest.json",
            AddonUrls.manifestUrl("https://example.test/abc123config"),
        )
    }

    @Test
    fun `a stremio scheme link is rewritten to https`() {
        assertEquals(
            "https://example.test/manifest.json",
            AddonUrls.manifestUrl("stremio://example.test/manifest.json"),
        )
    }

    @Test
    fun `plain http is preserved rather than silently upgraded`() {
        // Local add-ons during development are commonly http.
        assertEquals(
            "http://localhost:7000/manifest.json",
            AddonUrls.manifestUrl("http://localhost:7000/manifest.json"),
        )
    }

    @Test
    fun `trailing slashes and fragments are trimmed`() {
        assertEquals(
            "https://example.test/manifest.json",
            AddonUrls.manifestUrl("  https://example.test/#fragment  "),
        )
    }

    @Test
    fun `nonsense is rejected`() {
        assertNull(AddonUrls.manifestUrl(""))
        assertNull(AddonUrls.manifestUrl("   "))
        assertNull(AddonUrls.manifestUrl("https://"))
    }

    @Test
    fun `resource urls follow the protocol's path shape`() {
        assertEquals(
            "https://example.test/catalog/movie/top.json",
            AddonUrls.resourceUrl("https://example.test", "catalog", "movie", "top"),
        )
    }

    @Test
    fun `extra arguments become a segment before the suffix`() {
        assertEquals(
            "https://example.test/catalog/movie/top/skip=100&genre=Drama.json",
            AddonUrls.resourceUrl(
                baseUrl = "https://example.test",
                resource = "catalog",
                type = "movie",
                id = "top",
                extra = listOf("skip" to "100", "genre" to "Drama"),
            ),
        )
    }

    @Test
    fun `episode identifiers keep their colons`() {
        // Escaping the colon produces an id no add-on recognises, so this is
        // the single most important thing the encoder has to get right.
        assertEquals(
            "https://example.test/stream/series/tt1234567:1:2.json",
            AddonUrls.resourceUrl("https://example.test", "stream", "series", "tt1234567:1:2"),
        )
    }

    @Test
    fun `spaces in arguments are percent-encoded rather than turned into plus`() {
        assertEquals(
            "https://example.test/catalog/movie/top/search=the%20matrix.json",
            AddonUrls.resourceUrl(
                baseUrl = "https://example.test",
                resource = "catalog",
                type = "movie",
                id = "top",
                extra = listOf("search" to "the matrix"),
            ),
        )
    }
}
