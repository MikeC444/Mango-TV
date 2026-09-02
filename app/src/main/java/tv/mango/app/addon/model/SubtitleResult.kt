package tv.mango.app.addon.model

/**
 * One subtitle track, normalised.
 *
 * Reached either from the subtitles resource or from a stream that carried its
 * own. Both arrive here identically, so the player has one thing to handle.
 */
data class SubtitleResult(
    val id: String,
    val url: String,
    /** ISO code where the add-on gave one; otherwise whatever it did give. */
    val language: String,
    /** What to show the viewer. Falls back to the language code. */
    val label: String,
    val format: SubtitleFormat = SubtitleFormat.UNKNOWN,
    val providerId: String = "",
    val providerName: String = "",
)

/**
 * Guessed from the URL, since the protocol does not state it. The player needs
 * to know before it can build a track.
 */
enum class SubtitleFormat(val mimeType: String?) {
    SRT("application/x-subrip"),
    VTT("text/vtt"),
    SSA("text/x-ssa"),
    UNKNOWN(null),
    ;

    companion object {
        fun fromUrl(url: String): SubtitleFormat {
            val path = url.substringBefore('?').substringBefore('#').lowercase()
            return when {
                path.endsWith(".srt") -> SRT
                path.endsWith(".vtt") -> VTT
                path.endsWith(".ssa") || path.endsWith(".ass") -> SSA
                else -> UNKNOWN
            }
        }
    }
}
