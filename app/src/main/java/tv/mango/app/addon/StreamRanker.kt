package tv.mango.app.addon

import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.addon.model.StreamResult

/**
 * What the viewer would choose, given a straight list of everything found.
 *
 * Every field is a preference rather than a filter: ranking orders what came
 * back, it never removes a source because it missed one. A source that
 * matches nothing here is still a source the viewer can play, just last.
 */
data class StreamPreferences(
    val preferredQuality: StreamQuality? = null,
    val preferredCodec: String? = null,
    val preferredLanguage: String? = null,
    /**
     * Which shape of source to prefer, most preferred first. Defaults to what
     * this application can actually play without further work: a direct URL
     * ahead of a torrent ahead of a hand-off to something external.
     */
    val kindOrder: List<StreamKind> = listOf(
        StreamKind.DIRECT,
        StreamKind.PEER_TO_PEER,
        StreamKind.YOUTUBE,
        StreamKind.EXTERNAL,
    ),
)

/** The broad shape of a source, since ranking and playback both care about it. */
enum class StreamKind {
    DIRECT,
    PEER_TO_PEER,
    YOUTUBE,
    EXTERNAL,
    UNKNOWN,
    ;

    companion object {
        fun of(stream: StreamResult): StreamKind = when {
            stream.isDirectlyPlayable -> DIRECT
            stream.isPeerToPeer -> PEER_TO_PEER
            !stream.youtubeId.isNullOrBlank() -> YOUTUBE
            !stream.externalUrl.isNullOrBlank() -> EXTERNAL
            else -> UNKNOWN
        }
    }
}

/**
 * Orders streams gathered from several add-ons into one list.
 *
 * Never assumes the first stream to arrive is the best one: an add-on that
 * answers quickly is not the same as an add-on that answers well. Ordering is
 * driven entirely by [StreamPreferences] and by which add-on the viewer has
 * placed higher in their priority list - never by which add-on happened to
 * respond first - so a viewer who cares about 4K is not offered a 1080p
 * source from their favourite add-on before a 4K source from another.
 *
 * A total order over everything found, not a filter: nothing here drops a
 * stream, so the interface can always fall back down the list rather than
 * being left with fewer options than actually exist.
 */
class StreamRanker {

    fun rank(
        streams: List<StreamResult>,
        addonPriorityOrder: List<Addon>,
        preferences: StreamPreferences = StreamPreferences(),
    ): List<StreamResult> {
        if (streams.isEmpty()) return streams
        val priority = addonPriorityOrder.withIndex().associate { (index, addon) -> addon.id to index }
        return streams.sortedWith(
            compareBy(
                { kindRank(it, preferences) },
                { if (matchesQuality(it, preferences)) 0 else 1 },
                { -it.quality.rank },
                { if (matchesCodec(it, preferences)) 0 else 1 },
                { if (matchesLanguage(it, preferences)) 0 else 1 },
                { priority[it.providerId] ?: Int.MAX_VALUE },
                { -(it.sizeBytes ?: 0L) },
            ),
        )
    }

    private fun kindRank(stream: StreamResult, preferences: StreamPreferences): Int {
        val index = preferences.kindOrder.indexOf(StreamKind.of(stream))
        return if (index >= 0) index else preferences.kindOrder.size
    }

    private fun matchesQuality(stream: StreamResult, preferences: StreamPreferences): Boolean =
        preferences.preferredQuality == null || stream.quality == preferences.preferredQuality

    private fun matchesCodec(stream: StreamResult, preferences: StreamPreferences): Boolean =
        preferences.preferredCodec == null ||
            stream.codec?.equals(preferences.preferredCodec, ignoreCase = true) == true

    private fun matchesLanguage(stream: StreamResult, preferences: StreamPreferences): Boolean =
        preferences.preferredLanguage == null ||
            stream.language?.equals(preferences.preferredLanguage, ignoreCase = true) == true
}
