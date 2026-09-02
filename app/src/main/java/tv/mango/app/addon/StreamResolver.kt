package tv.mango.app.addon

import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.model.StreamResult
import tv.mango.app.addon.protocol.AddonUrls
import tv.mango.app.addon.protocol.CachePolicy
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.addon.protocol.StremioResponseParser
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType

/**
 * Streams for one title or episode, from every add-on that can answer.
 *
 * Unlike metadata, a viewer wants to see everything anyone has rather than one
 * provider's opinion - so every capable add-on is asked at once, the fan-out
 * already built for catalogues and metadata handles the concurrency and the
 * failure isolation, and every stream that comes back is kept and ranked
 * rather than narrowed down to one.
 *
 * Stream responses are never read from or written to a cache: a source can
 * expire, and a cached one presented as current would be a dead link the
 * viewer discovers only by pressing Play.
 */
class StreamResolver(
    private val manager: AddonManager,
    private val client: StremioProtocolClient,
    private val ranker: StreamRanker = StreamRanker(),
) {

    /**
     * @param videoId what the protocol addresses a stream request by: the
     *   title's own id for a film, or `"<seriesId>:<season>:<episode>"` for one
     *   episode - the shape [tv.mango.app.addon.protocol.StremioResponseParser]
     *   already builds episode ids in, so a caller need only pass through
     *   whichever id a viewer selected.
     */
    suspend fun streams(
        videoId: MediaId,
        type: MediaType,
        preferences: StreamPreferences = StreamPreferences(),
    ): Aggregated<StreamResult> {
        val wireType = StremioResponseParser.wireTypeOf(type)
        val addons = manager.capableOf(AddonResourceName.STREAM, wireType, videoId.value)
        if (addons.isEmpty()) return Aggregated()

        val resolved = manager.fanOut(addons, AddonManager.STREAM_TIMEOUT_MILLIS) { addon ->
            val url = AddonUrls.resourceUrl(
                baseUrl = addon.baseUrl,
                resource = AddonResourceName.STREAM.wireName,
                type = wireType,
                id = videoId.value,
            )
            when (val outcome = client.fetch(url, CachePolicy.NONE)) {
                is StremioProtocolClient.Outcome.Success ->
                    AddonReply.Answered(StremioResponseParser.parseStreams(outcome.body, addon))
                is StremioProtocolClient.Outcome.Failure -> AddonReply.Failed(outcome.reason)
            }
        }

        return resolved.copy(items = ranker.rank(resolved.items, addons, preferences))
    }
}
