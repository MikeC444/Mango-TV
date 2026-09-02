package tv.mango.app.addon

import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.addon.model.StreamResult
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ordering, not filtering: every test here checks that nothing found is
 * dropped, only reordered by preference.
 */
class StreamRankerTest {

    private val ranker = StreamRanker()

    private fun stream(
        providerId: String,
        url: String? = "https://source.test/a.mp4",
        infoHash: String? = null,
        quality: StreamQuality = StreamQuality.UNKNOWN,
        codec: String? = null,
        language: String? = null,
        sizeBytes: Long? = null,
    ) = StreamResult(
        url = url,
        infoHash = infoHash,
        quality = quality,
        codec = codec,
        language = language,
        sizeBytes = sizeBytes,
        providerId = providerId,
        providerName = providerId,
    )

    @Test
    fun `higher quality is preferred with no other preference set`() {
        val low = stream("a", quality = StreamQuality.SD_480)
        val high = stream("b", quality = StreamQuality.UHD_4K)

        val ranked = ranker.rank(listOf(low, high), addonPriorityOrder = emptyList())

        assertEquals(listOf(high, low), ranked)
    }

    @Test
    fun `a direct source is preferred over a torrent by default`() {
        val direct = stream("a", url = "https://source.test/a.mp4")
        val p2p = stream("b", url = null, infoHash = "abc123")

        val ranked = ranker.rank(listOf(p2p, direct), addonPriorityOrder = emptyList())

        assertEquals(listOf(direct, p2p), ranked)
    }

    @Test
    fun `a preferred quality wins even over a higher raw quality`() {
        val fourK = stream("a", quality = StreamQuality.UHD_4K)
        val matching = stream("b", quality = StreamQuality.FHD_1080)

        val ranked = ranker.rank(
            listOf(fourK, matching),
            addonPriorityOrder = emptyList(),
            preferences = StreamPreferences(preferredQuality = StreamQuality.FHD_1080),
        )

        assertEquals(listOf(matching, fourK), ranked)
    }

    @Test
    fun `equal quality falls back to add-on priority order`() {
        val fromLowPriority = stream("second-choice")
        val fromHighPriority = stream("first-choice")
        val addons = listOf(
            testAddon("first-choice", "https://a.test"),
            testAddon("second-choice", "https://b.test"),
        )

        val ranked = ranker.rank(listOf(fromLowPriority, fromHighPriority), addonPriorityOrder = addons)

        assertEquals(listOf(fromHighPriority, fromLowPriority), ranked)
    }

    @Test
    fun `nothing is ever dropped, regardless of how little a stream matches`() {
        val streams = listOf(
            stream("a", url = null, infoHash = null), // no source at all - still ranked, never filtered
            stream("b", quality = StreamQuality.UNKNOWN),
        )

        val ranked = ranker.rank(streams, addonPriorityOrder = emptyList())

        assertEquals(streams.size, ranked.size)
    }

    @Test
    fun `an empty list ranks to an empty list`() {
        assertEquals(emptyList(), ranker.rank(emptyList(), addonPriorityOrder = emptyList()))
    }
}
