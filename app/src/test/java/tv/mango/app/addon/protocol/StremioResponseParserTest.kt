package tv.mango.app.addon.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.AddonManifest
import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StremioResponseParserTest {

    private fun body(json: String): JsonObject =
        Json.parseToJsonElement(json) as JsonObject

    private val provider = Addon(
        manifestUrl = "https://example.test/manifest.json",
        manifest = AddonManifest(id = "org.example", name = "Example", version = "1.0.0"),
    )

    // ---------------------------------------------------------------- catalog

    @Test
    fun `a movie catalog is read into card projections`() {
        val items = StremioResponseParser.parseCatalog(
            body(
                """
                {
                  "metas": [
                    {
                      "id": "tt0111161", "type": "movie", "name": "A Film",
                      "poster": "https://example.test/p.jpg",
                      "background": "https://example.test/b.jpg",
                      "releaseInfo": "1994",
                      "genres": ["Drama"],
                      "imdbRating": "9.3"
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val item = items.single()
        assertEquals(MediaId("tt0111161"), item.id)
        assertEquals(MediaType.MOVIE, item.type)
        assertEquals("A Film", item.title)
        assertEquals(1994, item.year)
        assertEquals(listOf("Drama"), item.genres)
        assertEquals("9.3", item.rating)
        assertEquals("https://example.test/p.jpg", item.images.poster)
    }

    @Test
    fun `a catalog row carrying only the essentials is still usable`() {
        // This is the common case: a catalogue gives an id, a type, a name and
        // a poster, and the rest arrives only when the title is opened.
        val items = StremioResponseParser.parseCatalog(
            body("""{ "metas": [{ "id": "tt1", "type": "series", "name": "A Show" }] }"""),
        )

        val item = items.single()
        assertEquals("A Show", item.title)
        assertNull(item.year)
        assertNull(item.runtimeMinutes)
        assertNull(item.synopsis)
        assertNull(item.rating)
        assertTrue(item.genres.isEmpty())
    }

    @Test
    fun `malformed entries are skipped and the rest of the catalog survives`() {
        val items = StremioResponseParser.parseCatalog(
            body(
                """
                {
                  "metas": [
                    { "id": "tt1", "type": "movie", "name": "Good" },
                    { "type": "movie", "name": "No id" },
                    { "id": "tt2", "name": "No type" },
                    { "id": "tt3", "type": "channel", "name": "Unsupported type" },
                    "not even an object",
                    { "id": "tt4", "type": "movie", "name": "Also good" }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("Good", "Also good"), items.map { it.title })
    }

    @Test
    fun `an empty catalog is empty rather than an error`() {
        assertTrue(StremioResponseParser.parseCatalog(body("""{ "metas": [] }""")).isEmpty())
        assertTrue(StremioResponseParser.parseCatalog(body("{}")).isEmpty())
    }

    // ------------------------------------------------------------------- meta

    @Test
    fun `series metadata yields seasons and episodes`() {
        val detail = assertNotNull(
            StremioResponseParser.parseMeta(
                body(
                    """
                    {
                      "meta": {
                        "id": "tt1", "type": "series", "name": "A Show",
                        "description": "About something.",
                        "runtime": "48 min",
                        "cast": ["Someone Notable", "Another Person"],
                        "videos": [
                          { "id": "tt1:1:1", "season": 1, "episode": 1, "name": "Pilot" },
                          { "id": "tt1:1:2", "season": 1, "episode": 2, "name": "Second" },
                          { "id": "tt1:2:1", "season": 2, "episode": 1, "name": "Return" }
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals("A Show", detail.item.title)
        assertEquals(48, detail.item.runtimeMinutes)
        assertEquals(2, detail.cast.size)
        assertEquals(listOf(1, 2), detail.seasons.map { it.number })
        assertEquals(2, detail.seasons.first().episodeCount)
    }

    @Test
    fun `season zero is hidden rather than listed first`() {
        val detail = assertNotNull(
            StremioResponseParser.parseMeta(
                body(
                    """
                    {
                      "meta": {
                        "id": "tt1", "type": "series", "name": "A Show",
                        "videos": [
                          { "id": "tt1:0:1", "season": 0, "episode": 1, "name": "A special" },
                          { "id": "tt1:1:1", "season": 1, "episode": 1, "name": "Pilot" }
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(listOf(1), detail.seasons.map { it.number })
    }

    @Test
    fun `cast roles are read from the credits form when present`() {
        val detail = assertNotNull(
            StremioResponseParser.parseMeta(
                body(
                    """
                    {
                      "meta": {
                        "id": "tt1", "type": "movie", "name": "A Film",
                        "credits_cast": [
                          { "name": "Someone Notable", "character": "The Lead" }
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals("Someone Notable", detail.cast.single().name)
        assertEquals("The Lead", detail.cast.single().role)
    }

    @Test
    fun `episodes without a season or number are skipped`() {
        val episodes = StremioResponseParser.parseEpisodes(
            body(
                """
                {
                  "videos": [
                    { "id": "a", "season": 1, "episode": 1, "name": "Fine" },
                    { "id": "b", "episode": 2, "name": "No season" },
                    { "id": "c", "season": 1, "name": "No number" }
                  ]
                }
                """.trimIndent(),
            ),
            MediaId("tt1"),
        )

        assertEquals(listOf("Fine"), episodes.map { it.title })
    }

    @Test
    fun `a meta response missing its meta object is rejected`() {
        assertNull(StremioResponseParser.parseMeta(body("{}")))
        assertNull(StremioResponseParser.parseMeta(body("""{ "meta": {} }""")))
    }

    @Test
    fun `runtime is read from the several forms providers use`() {
        assertEquals(142, StremioResponseParser.parseRuntimeMinutes("142 min"))
        assertEquals(142, StremioResponseParser.parseRuntimeMinutes("142"))
        assertEquals(142, StremioResponseParser.parseRuntimeMinutes("2h 22min"))
        assertEquals(120, StremioResponseParser.parseRuntimeMinutes("2h"))
        assertNull(StremioResponseParser.parseRuntimeMinutes(null))
        assertNull(StremioResponseParser.parseRuntimeMinutes(""))
        assertNull(StremioResponseParser.parseRuntimeMinutes("who knows"))
    }

    // ----------------------------------------------------------------- stream

    @Test
    fun `streams are normalised and attributed to their provider`() {
        val streams = StremioResponseParser.parseStreams(
            body(
                """
                {
                  "streams": [
                    {
                      "name": "Example 1080p",
                      "title": "A Film 1080p x264\n1.4 GB",
                      "url": "https://example.test/a.mp4"
                    },
                    {
                      "name": "Example 4K",
                      "title": "A Film 2160p HEVC",
                      "url": "https://example.test/b.mp4",
                      "behaviorHints": { "videoSize": 12345, "filename": "b.mp4" }
                    }
                  ]
                }
                """.trimIndent(),
            ),
            provider,
        )

        assertEquals(2, streams.size)
        assertEquals(StreamQuality.FHD_1080, streams[0].quality)
        assertEquals("H.264", streams[0].codec)
        assertEquals(1503238553L, streams[0].sizeBytes)
        assertEquals(StreamQuality.UHD_4K, streams[1].quality)
        assertEquals("HEVC", streams[1].codec)
        assertEquals(12345L, streams[1].sizeBytes)
        assertTrue(streams.all { it.providerId == "org.example" })
        assertTrue(streams.all { it.providerName == "Example" })
        assertTrue(streams.all { it.isDirectlyPlayable })
    }

    @Test
    fun `a stream with no source at all is dropped`() {
        val streams = StremioResponseParser.parseStreams(
            body(
                """
                {
                  "streams": [
                    { "name": "Playable", "url": "https://example.test/a.mp4" },
                    { "name": "Nothing to play" },
                    { "name": "Peer to peer", "infoHash": "abc123" }
                  ]
                }
                """.trimIndent(),
            ),
            provider,
        )

        assertEquals(listOf("Playable", "Peer to peer"), streams.map { it.name })
        assertTrue(streams[1].isPeerToPeer)
        // A peer-to-peer source is recognised but is not directly playable.
        assertTrue(!streams[1].isDirectlyPlayable)
    }

    @Test
    fun `an invalid stream response yields nothing rather than throwing`() {
        assertTrue(StremioResponseParser.parseStreams(body("{}"), provider).isEmpty())
        assertTrue(
            StremioResponseParser.parseStreams(body("""{ "streams": "wrong" }"""), provider)
                .isEmpty(),
        )
    }

    // -------------------------------------------------------------- subtitles

    @Test
    fun `subtitles are normalised with their format inferred from the url`() {
        val subtitles = StremioResponseParser.parseSubtitles(
            body(
                """
                {
                  "subtitles": [
                    { "id": "1", "url": "https://example.test/en.srt", "lang": "eng" },
                    { "id": "2", "url": "https://example.test/fr.vtt", "lang": "fra",
                      "title": "French (forced)" },
                    { "id": "3", "lang": "deu" }
                  ]
                }
                """.trimIndent(),
            ),
            provider,
        )

        assertEquals(2, subtitles.size)
        assertEquals("eng", subtitles[0].language)
        assertEquals("eng", subtitles[0].label)
        assertEquals("French (forced)", subtitles[1].label)
        assertEquals("text/vtt", subtitles[1].format.mimeType)
    }

    @Test
    fun `a missing subtitles array is empty rather than an error`() {
        assertTrue(StremioResponseParser.parseSubtitles(body("{}"), provider).isEmpty())
    }
}
