package tv.mango.app.addon.protocol

import tv.mango.app.addon.model.StreamQuality

/**
 * Reads resolution, codec, size and language out of an add-on's stream labels.
 *
 * The protocol has no field for any of these. By convention add-ons write them
 * into the stream's name and title, in whatever house style they happen to use,
 * and every client that offers a sorted list of sources is reading them back
 * out again.
 *
 * That makes this unavoidably a heuristic, so it is written to fail quietly:
 * anything it cannot recognise becomes unknown, which sorts last but is still
 * offered. Guessing wrongly would be worse than not guessing - a source
 * mislabelled 4K is a worse outcome than one labelled nothing at all.
 */
internal object StreamDescriptors {

    fun qualityOf(text: String): StreamQuality {
        val normalised = text.lowercase()
        return when {
            // 2160p, 4K and UHD all name the same thing.
            normalised.containsAny("2160", "4k", "uhd") -> StreamQuality.UHD_4K
            normalised.containsAny("1440", "2k", "qhd") -> StreamQuality.QHD_1440
            normalised.containsAny("1080", "fullhd", "full hd", "fhd") -> StreamQuality.FHD_1080
            normalised.containsAny("720", "hd ") -> StreamQuality.HD_720
            normalised.containsAny("480", "360", "240", "sd ") -> StreamQuality.SD_480
            else -> StreamQuality.UNKNOWN
        }
    }

    fun codecOf(text: String): String? {
        val normalised = text.lowercase()
        return when {
            normalised.containsAny("av1") -> "AV1"
            normalised.containsAny("hevc", "h265", "h.265", "x265") -> "HEVC"
            normalised.containsAny("avc", "h264", "h.264", "x264") -> "H.264"
            normalised.containsAny("vp9") -> "VP9"
            else -> null
        }
    }

    /** "1.4 GB", "700MB". Returned in bytes so it can be compared. */
    fun sizeOf(text: String): Long? {
        val match = SIZE.find(text) ?: return null
        val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].lowercase()) {
            "gb", "gib" -> 1024L * 1024L * 1024L
            "mb", "mib" -> 1024L * 1024L
            "kb", "kib" -> 1024L
            else -> return null
        }
        return (amount * multiplier).toLong()
    }

    /**
     * A language, only where an add-on has stated one unambiguously.
     *
     * Deliberately narrow. Inferring a language from a title is how a source
     * ends up mislabelled, and a wrong label is worse than none.
     */
    fun languageOf(text: String): String? {
        val match = LANGUAGE.find(text) ?: return null
        return match.groupValues[1].takeIf { it.isNotBlank() }
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { contains(it) }

    private val SIZE = Regex(
        """(\d+(?:[.,]\d+)?)\s*(GiB|GB|MiB|MB|KiB|KB)""",
        RegexOption.IGNORE_CASE,
    )

    /** Matches an explicit "Language: X" or a flag-prefixed label. */
    private val LANGUAGE = Regex(
        """(?:language|lang|audio)\s*[:\-]\s*([A-Za-z]{2,20})""",
        RegexOption.IGNORE_CASE,
    )
}
