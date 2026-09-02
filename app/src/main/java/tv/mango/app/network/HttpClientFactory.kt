package tv.mango.app.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The application's one HTTP client.
 *
 * Shared deliberately. OkHttp's expensive parts are its connection pool and
 * thread pool, and a client per add-on would multiply both by however many the
 * user has installed - on a device where memory is the binding constraint.
 *
 * The limits below exist because a screen can fan out to every installed
 * add-on at once. Without a ceiling, ten add-ons mean ten simultaneous
 * connections competing for a stick's modest radio, and the slowest of them
 * decides how long everything takes.
 */
object HttpClientFactory {

    /**
     * Deliberately short. An add-on that has not answered in this long is one
     * the viewer is already waiting on, and the aggregators are built to return
     * partial results rather than block on the slowest member.
     */
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val READ_TIMEOUT_SECONDS = 12L
    private const val CALL_TIMEOUT_SECONDS = 15L

    /** Across all add-ons. */
    private const val MAX_CONCURRENT_REQUESTS = 8

    /** One slow add-on must not be able to occupy the whole budget. */
    private const val MAX_CONCURRENT_PER_HOST = 3

    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(MAX_CONCURRENT_REQUESTS, 5, TimeUnit.MINUTES))
        .dispatcher(
            Dispatcher().apply {
                maxRequests = MAX_CONCURRENT_REQUESTS
                maxRequestsPerHost = MAX_CONCURRENT_PER_HOST
            },
        )
        .build()
}
