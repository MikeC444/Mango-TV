package tv.mango.app.addon.protocol

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The transport, against a real HTTP server.
 *
 * Every one of these is a failure an add-on will eventually produce in the
 * field. What is being pinned is that each becomes a value the caller can act
 * on rather than an exception thrown into whatever screen was waiting - one
 * broken add-on must never take a screen down with it.
 */
class StremioProtocolClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: StremioProtocolClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = StremioProtocolClient(
            OkHttpClient.Builder()
                .callTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun url(path: String) = server.url(path).toString()

    @Test
    fun `a well formed response is parsed`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{ "metas": [{ "id": "tt1", "type": "movie", "name": "A" }] }"""),
        )

        val outcome = client.fetch(url("/catalog/movie/top.json"))

        val success = assertIs<StremioProtocolClient.Outcome.Success>(outcome)
        assertEquals(1, StremioResponseParser.parseCatalog(success.body).size)
    }

    @Test
    fun `the request asks for json`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.fetch(url("/manifest.json"))

        assertEquals("application/json", server.takeRequest().getHeader("Accept"))
    }

    @Test
    fun `a 404 is reported as not found`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val outcome = client.fetch(url("/meta/movie/tt1.json"))

        val failure = assertIs<StremioProtocolClient.Outcome.Failure>(outcome)
        assertEquals(ProtocolFailure.NOT_FOUND, failure.reason)
        assertEquals(404, failure.status)
        // Not worth retrying: the add-on answered, and its answer was no.
        assertTrue(!failure.reason.isTransient)
    }

    @Test
    fun `a 500 is reported as a server error and is worth retrying`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val outcome = client.fetch(url("/meta/movie/tt1.json"))

        val failure = assertIs<StremioProtocolClient.Outcome.Failure>(outcome)
        assertEquals(ProtocolFailure.SERVER_ERROR, failure.reason)
        assertTrue(failure.reason.isTransient)
    }

    @Test
    fun `other unsuccessful statuses are reported as refused`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val outcome = client.fetch(url("/meta/movie/tt1.json"))

        assertEquals(
            ProtocolFailure.REFUSED,
            assertIs<StremioProtocolClient.Outcome.Failure>(outcome).reason,
        )
    }

    @Test
    fun `invalid json is reported as malformed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("this is not json"))

        val outcome = client.fetch(url("/manifest.json"))

        assertEquals(
            ProtocolFailure.MALFORMED,
            assertIs<StremioProtocolClient.Outcome.Failure>(outcome).reason,
        )
    }

    @Test
    fun `json that is not an object is reported as malformed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""["not", "an", "object"]"""))

        val outcome = client.fetch(url("/manifest.json"))

        assertEquals(
            ProtocolFailure.MALFORMED,
            assertIs<StremioProtocolClient.Outcome.Failure>(outcome).reason,
        )
    }

    @Test
    fun `an empty body is reported rather than parsed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val outcome = client.fetch(url("/manifest.json"))

        assertEquals(
            ProtocolFailure.EMPTY_BODY,
            assertIs<StremioProtocolClient.Outcome.Failure>(outcome).reason,
        )
    }

    @Test
    fun `a body that stalls mid-read is transient, not an empty response`() = runTest {
        // The headers arrive, then the body stalls. Reporting this as an empty
        // body would mark a passing network problem permanent and stop the
        // caller retrying something that would have worked.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(5, TimeUnit.SECONDS),
        )

        val outcome = client.fetch(url("/manifest.json"))

        val failure = assertIs<StremioProtocolClient.Outcome.Failure>(outcome)
        assertEquals(ProtocolFailure.UNREACHABLE, failure.reason)
        assertTrue(failure.reason.isTransient)
    }

    @Test
    fun `a timeout is reported as unreachable and is worth retrying`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .setBodyDelay(5, TimeUnit.SECONDS),
        )

        val outcome = client.fetch(url("/manifest.json"))

        val failure = assertIs<StremioProtocolClient.Outcome.Failure>(outcome)
        assertEquals(ProtocolFailure.UNREACHABLE, failure.reason)
        assertTrue(failure.reason.isTransient)
    }

    @Test
    fun `a dropped connection is reported rather than thrown`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = client.fetch(url("/manifest.json"))

        assertEquals(
            ProtocolFailure.UNREACHABLE,
            assertIs<StremioProtocolClient.Outcome.Failure>(outcome).reason,
        )
    }

    @Test
    fun `an unreachable host is reported rather than thrown`() = runTest {
        // Nothing is listening here; the port was closed when the server shut down.
        val dead = url("/manifest.json")
        server.shutdown()

        val outcome = client.fetch(dead)

        assertEquals(
            ProtocolFailure.UNREACHABLE,
            assertIs<StremioProtocolClient.Outcome.Failure>(outcome).reason,
        )
    }
}
