package tv.mango.app.addon

import kotlinx.serialization.json.JsonObject
import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.protocol.AddonUrls
import tv.mango.app.addon.protocol.CachePolicy
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.addon.protocol.StremioResponseParser
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import tv.mango.app.models.TitleDetail

/**
 * Full detail for one title, from whichever add-on can best supply it.
 *
 * Metadata is unlike streams: the viewer wants one description of a film, not
 * every description of it. So add-ons are asked in priority order and the first
 * usable answer is taken, rather than all of them being asked at once and all
 * but one discarded.
 *
 * Where the winning answer has gaps that matter, the next provider is asked to
 * fill them - a series whose metadata carries no episodes is a detail screen
 * with an empty episode list, which is worth a second request to avoid. The
 * gaps that do not matter are left alone.
 */
class MetadataResolver(
    private val manager: AddonManager,
    private val client: StremioProtocolClient,
) {

    suspend fun detail(id: MediaId, type: MediaType): TitleDetail? {
        val wireType = StremioResponseParser.wireTypeOf(type)
        val addons = manager.capableOf(AddonResourceName.META, wireType, id.value)
        if (addons.isEmpty()) return null

        val answer = manager.firstAnswer(addons) { addon -> fetchMeta(addon, wireType, id) }
            ?: return null

        val (winner, base) = answer
        return fillGaps(base, addons - winner, wireType, id)
    }

    /**
     * Episodes for one season.
     *
     * The protocol returns a series' whole run in its metadata response - there
     * is no per-season request - so this filters what the meta call already
     * returned rather than issuing a narrower one.
     */
    suspend fun episodes(id: MediaId, season: Int): List<Episode> {
        val addons = manager.capableOf(AddonResourceName.META, SERIES_TYPE, id.value)
        val answer = manager.firstAnswer(addons) { addon ->
            val body = fetchMetaBody(addon, SERIES_TYPE, id) ?: return@firstAnswer null
            val meta = body["meta"] as? JsonObject ?: return@firstAnswer null
            StremioResponseParser.parseEpisodes(meta, id).takeIf { it.isNotEmpty() }
        }
        return answer?.second.orEmpty().filter { it.season == season }
    }

    /**
     * Asks further providers only for what the first one did not supply.
     *
     * Deliberately narrow. Merging every field across every provider would mean
     * a request per add-on on every detail screen; this asks again only when
     * the result would otherwise be visibly incomplete, and stops as soon as it
     * is not.
     */
    private suspend fun fillGaps(
        base: TitleDetail,
        others: List<Addon>,
        wireType: String,
        id: MediaId,
    ): TitleDetail {
        if (!base.hasVisibleGaps()) return base
        if (others.isEmpty()) return base

        var merged = base
        for (addon in others) {
            if (!merged.hasVisibleGaps()) break
            val candidate = fetchMeta(addon, wireType, id) ?: continue
            merged = merged.mergedWith(candidate)
        }
        return merged
    }

    private suspend fun fetchMeta(addon: Addon, wireType: String, id: MediaId): TitleDetail? {
        val body = fetchMetaBody(addon, wireType, id) ?: return null
        return StremioResponseParser.parseMeta(body)
    }

    private suspend fun fetchMetaBody(
        addon: Addon,
        wireType: String,
        id: MediaId,
    ): JsonObject? {
        val url = AddonUrls.resourceUrl(
            baseUrl = addon.baseUrl,
            resource = AddonResourceName.META.wireName,
            type = wireType,
            id = id.value,
        )
        return when (val outcome = client.fetch(url, CachePolicy.METADATA)) {
            is StremioProtocolClient.Outcome.Success -> outcome.body
            is StremioProtocolClient.Outcome.Failure -> null
        }
    }

    /** Gaps a viewer would notice: no description, no cast, or a series with no episodes. */
    private fun TitleDetail.hasVisibleGaps(): Boolean =
        item.synopsis.isNullOrBlank() ||
            cast.isEmpty() ||
            (item.type == MediaType.SERIES && seasons.isEmpty())

    /** Takes from [other] only what this record does not already have. */
    private fun TitleDetail.mergedWith(other: TitleDetail): TitleDetail = copy(
        item = item.copy(
            year = item.year ?: other.item.year,
            runtimeMinutes = item.runtimeMinutes ?: other.item.runtimeMinutes,
            certification = item.certification ?: other.item.certification,
            genres = item.genres.ifEmpty { other.item.genres },
            rating = item.rating ?: other.item.rating,
            synopsis = item.synopsis?.takeIf { it.isNotBlank() } ?: other.item.synopsis,
            images = item.images.copy(
                poster = item.images.poster.ifBlank { other.item.images.poster },
                backdrop = item.images.backdrop.ifBlank { other.item.images.backdrop },
            ),
        ),
        cast = cast.ifEmpty { other.cast },
        seasons = seasons.ifEmpty { other.seasons },
    )

    private companion object {
        const val SERIES_TYPE = "series"
    }
}
