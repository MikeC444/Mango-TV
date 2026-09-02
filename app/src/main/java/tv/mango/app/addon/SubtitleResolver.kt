package tv.mango.app.addon

import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.model.SubtitleResult
import tv.mango.app.addon.protocol.AddonUrls
import tv.mango.app.addon.protocol.CachePolicy
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.addon.protocol.StremioResponseParser
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType

/**
 * Subtitles for one title or episode, from every add-on that advertises the
 * resource, folded together with whatever tracks the chosen stream already
 * carried.
 *
 * A stream can bring its own subtitles - see
 * [tv.mango.app.addon.model.StreamResult.subtitles] - and a separate
 * subtitles lookup is not guaranteed to exist at all, so the player is meant
 * to have one combined list regardless of which of those sources, or both,
 * actually had something.
 */
class SubtitleResolver(
    private val manager: AddonManager,
    private val client: StremioProtocolClient,
) {

    suspend fun subtitles(
        videoId: MediaId,
        type: MediaType,
        fromStream: List<SubtitleResult> = emptyList(),
    ): Aggregated<SubtitleResult> {
        val wireType = StremioResponseParser.wireTypeOf(type)
        val addons = manager.capableOf(AddonResourceName.SUBTITLES, wireType, videoId.value)

        val resolved = if (addons.isEmpty()) {
            Aggregated()
        } else {
            manager.fanOut(addons) { addon ->
                val url = AddonUrls.resourceUrl(
                    baseUrl = addon.baseUrl,
                    resource = AddonResourceName.SUBTITLES.wireName,
                    type = wireType,
                    id = videoId.value,
                )
                when (val outcome = client.fetch(url, CachePolicy.NONE)) {
                    is StremioProtocolClient.Outcome.Success ->
                        AddonReply.Answered(StremioResponseParser.parseSubtitles(outcome.body, addon))
                    is StremioProtocolClient.Outcome.Failure -> AddonReply.Failed(outcome.reason)
                }
            }
        }

        // Deduplicated by add-on, language and URL: a track offered both by
        // the subtitles resource and embedded on the winning stream should
        // not show the viewer the same language twice.
        val combined = (fromStream + resolved.items)
            .distinctBy { Triple(it.providerId, it.language, it.url) }
        return resolved.copy(items = combined)
    }
}
