package tv.mango.app.addon.model

/**
 * One playable source, normalised.
 *
 * The player never learns which add-on produced a stream: it receives this.
 * That is the whole point of the type - the add-on ecosystem stops here, and
 * everything downstream deals in one shape.
 *
 * Protocol fields are preserved rather than discarded even where this
 * application does not yet use them, because throwing them away at the parser
 * is irreversible and they are what later ranking and playback will need.
 */
data class StreamResult(
    /** Identifies the stream within its add-on, where one was given. */
    val id: String? = null,

    /** The add-on's short label, usually naming the source or its quality. */
    val name: String? = null,

    /** The add-on's longer description, often several lines. */
    val title: String? = null,

    /**
     * Exactly one of these carries the actual source. A stream with none of
     * them is discarded by the parser as unplayable.
     */
    val url: String? = null,
    val infoHash: String? = null,
    val fileIndex: Int? = null,
    val externalUrl: String? = null,
    val youtubeId: String? = null,

    /** Parsed out of the add-on's labels, since the protocol has no field for it. */
    val quality: StreamQuality = StreamQuality.UNKNOWN,
    val codec: String? = null,
    val sizeBytes: Long? = null,
    val language: String? = null,
    val audio: String? = null,

    val behaviorHints: StreamBehaviorHints = StreamBehaviorHints(),

    /** Subtitles the stream carried with it, as opposed to a separate lookup. */
    val subtitles: List<SubtitleResult> = emptyList(),

    /** Which add-on supplied this, so the interface can say so. */
    val providerId: String = "",
    val providerName: String = "",
) {
    /** Whether this application can play it directly. */
    val isDirectlyPlayable: Boolean get() = !url.isNullOrBlank()

    /**
     * A peer-to-peer source. Recognised so it can be labelled honestly and
     * handled by policy, rather than silently presented as a direct stream.
     */
    val isPeerToPeer: Boolean get() = !infoHash.isNullOrBlank()
}

data class StreamBehaviorHints(
    /** The source will not survive being cached or shared. */
    val notWebReady: Boolean = false,
    val bingeGroup: String? = null,
    val countryWhitelist: List<String> = emptyList(),
    val proxyHeaders: Map<String, Map<String, String>> = emptyMap(),
    val videoSize: Long? = null,
    val filename: String? = null,
)

/**
 * Resolution, in the coarse buckets a viewer actually chooses between.
 *
 * An ordinal is carried explicitly rather than relying on declaration order, so
 * ranking cannot be changed by accident when a case is inserted.
 */
enum class StreamQuality(val label: String, val rank: Int) {
    UHD_4K("4K", 5),
    QHD_1440("1440p", 4),
    FHD_1080("1080p", 3),
    HD_720("720p", 2),
    SD_480("480p", 1),
    UNKNOWN("", 0),
}
