package tv.mango.app.addon

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

/**
 * A small, real Stremio-compatible add-on.
 *
 * Not a fake with canned method calls - an actual [MockWebServer] that speaks
 * the wire protocol, so tests built against it exercise the real manifest
 * parsing, URL building and response parsing end to end rather than a
 * hand-typed shortcut through them. It carries three movies and one series
 * with two seasons, across every resource this application understands, which
 * is enough to test the whole path from installing an add-on to playing
 * something from it without depending on any external service.
 *
 * The manifest deliberately mixes both ways the protocol allows declaring a
 * resource - a bare string and an object narrowing it to certain types and id
 * prefixes - because a real add-on ecosystem contains both and this
 * application has to read either.
 */
class MockStremioAddon {

    val server: MockWebServer = MockWebServer()

    fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                return when {
                    path == "/manifest.json" -> json(MANIFEST)
                    path.startsWith("/catalog/movie/top") -> json(MOVIE_CATALOG)
                    path.startsWith("/catalog/series/top") -> json(SERIES_CATALOG)
                    path.startsWith("/meta/movie/$MOVIE_1_ID") -> json(MOVIE_META)
                    path.startsWith("/meta/series/$SERIES_1_ID") -> json(SERIES_META)
                    path.startsWith("/stream/movie/$MOVIE_1_ID") -> json(MOVIE_STREAMS)
                    path.startsWith("/stream/series/$SERIES_1_ID:1:1") -> json(EPISODE_STREAMS)
                    path.startsWith("/subtitles/movie/$MOVIE_1_ID") -> json(MOVIE_SUBTITLES)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    fun shutdown() = server.shutdown()

    val manifestUrl: String get() = server.url("/manifest.json").toString()

    private fun json(body: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(body)

    companion object {
        const val MOVIE_1_ID = "mock-movie-1"
        const val SERIES_1_ID = "mock-series-1"

        private val MANIFEST = """
            {
              "id": "tv.mango.mock",
              "name": "Mock Library",
              "description": "A small, self-contained catalogue for testing.",
              "version": "1.0.0",
              "logo": "https://mock.test/logo.png",
              "types": ["movie", "series"],
              "idPrefixes": ["mock-"],
              "resources": [
                "catalog",
                { "name": "meta", "types": ["movie", "series"], "idPrefixes": ["mock-"] },
                "stream",
                "subtitles"
              ],
              "catalogs": [
                { "type": "movie", "id": "top", "name": "Mock Movies",
                  "extra": [{ "name": "skip", "isRequired": false }] },
                { "type": "series", "id": "top", "name": "Mock Series" }
              ]
            }
        """.trimIndent()

        private val MOVIE_CATALOG = """
            {
              "metas": [
                { "id": "$MOVIE_1_ID", "type": "movie", "name": "First Light",
                  "poster": "https://mock.test/p1.jpg" },
                { "id": "mock-movie-2", "type": "movie", "name": "Harbor Season",
                  "poster": "https://mock.test/p2.jpg" },
                { "id": "mock-movie-3", "type": "movie", "name": "Glass Meridian",
                  "poster": "https://mock.test/p3.jpg" }
              ]
            }
        """.trimIndent()

        private val SERIES_CATALOG = """
            {
              "metas": [
                { "id": "$SERIES_1_ID", "type": "series", "name": "The Long Descent",
                  "poster": "https://mock.test/s1.jpg" }
              ]
            }
        """.trimIndent()

        private val MOVIE_META = """
            {
              "meta": {
                "id": "$MOVIE_1_ID", "type": "movie", "name": "First Light",
                "poster": "https://mock.test/p1.jpg", "background": "https://mock.test/b1.jpg",
                "description": "A quiet arrival, and what it costs.",
                "releaseInfo": "2019", "genres": ["Drama"], "runtime": "128 min",
                "credits_cast": [{ "name": "Amara Voss", "character": "Rell" }]
              }
            }
        """.trimIndent()

        private val SERIES_META = """
            {
              "meta": {
                "id": "$SERIES_1_ID", "type": "series", "name": "The Long Descent",
                "poster": "https://mock.test/s1.jpg", "background": "https://mock.test/sb1.jpg",
                "description": "Two seasons underground.", "genres": ["Thriller"],
                "videos": [
                  { "id": "$SERIES_1_ID:1:1", "season": 1, "episode": 1, "name": "Entry" },
                  { "id": "$SERIES_1_ID:1:2", "season": 1, "episode": 2, "name": "Depth" },
                  { "id": "$SERIES_1_ID:2:1", "season": 2, "episode": 1, "name": "Surface" }
                ]
              }
            }
        """.trimIndent()

        private val MOVIE_STREAMS = """
            {
              "streams": [
                { "name": "MockSource 2160p", "title": "First Light 2160p HEVC",
                  "url": "https://mock.test/stream/first-light-2160p.mp4" },
                { "name": "MockSource 1080p", "title": "First Light 1080p x264 1.4GB",
                  "url": "https://mock.test/stream/first-light-1080p.mp4" },
                { "name": "MockSource 720p", "title": "First Light 720p",
                  "url": "https://mock.test/stream/first-light-720p.mp4" }
              ]
            }
        """.trimIndent()

        private val EPISODE_STREAMS = """
            {
              "streams": [
                { "name": "MockSource 1080p", "url": "https://mock.test/stream/entry-1080p.mp4" }
              ]
            }
        """.trimIndent()

        private val MOVIE_SUBTITLES = """
            {
              "subtitles": [
                { "id": "en", "url": "https://mock.test/subs/first-light-en.srt", "lang": "en" },
                { "id": "es", "url": "https://mock.test/subs/first-light-es.vtt", "lang": "es" }
              ]
            }
        """.trimIndent()
    }
}
