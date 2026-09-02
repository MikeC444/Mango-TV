package tv.mango.app.addon.protocol

import kotlinx.serialization.json.JsonObject
import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.StreamBehaviorHints
import tv.mango.app.addon.model.StreamQuality
import tv.mango.app.addon.model.StreamResult
import tv.mango.app.addon.model.SubtitleFormat
import tv.mango.app.addon.model.SubtitleResult
import tv.mango.app.models.CastMember
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaImages
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.models.Season
import tv.mango.app.models.TitleDetail

/**
 * Turns add-on responses into the application's own models.
 *
 * This is the boundary. Above it nothing knows the protocol exists: the home
 * screen renders a MediaItem whether it came from a bundled asset or from a
 * catalogue service, and the detail screen renders a TitleDetail without
 * knowing which of several add-ons answered.
 *
 * Every method skips what it cannot read rather than failing. A catalogue of
 * fifty titles where three entries are malformed is a catalogue of forty-seven,
 * not an error - the viewer would rather have most of a screen than none of it.
 */
object StremioResponseParser {

    /** Content types this application can present. Others are skipped. */
    private fun mediaTypeOf(wire: String?): MediaType? = when (wire?.lowercase()) {
        "movie" -> MediaType.MOVIE
        "series" -> MediaType.SERIES
        else -> null
    }

    fun wireTypeOf(type: MediaType): String = when (type) {
        MediaType.MOVIE -> "movie"
        MediaType.SERIES -> "series"
    }

    // ---------------------------------------------------------------- catalog

    /** Reads a `metas` array into card-sized projections. */
    fun parseCatalog(body: JsonObject): List<MediaItem> =
        body.objectList("metas").mapNotNull(::parseMediaItem)

    /**
     * One catalogue entry.
     *
     * Only an id, a type and a name are genuinely required; a row without them
     * cannot be shown or opened. Everything else is filled where present.
     */
    fun parseMediaItem(meta: JsonObject): MediaItem? {
        val id = meta.firstStr("id", "imdb_id") ?: return null
        val type = mediaTypeOf(meta.str("type")) ?: return null
        val name = meta.firstStr("name", "title") ?: return null

        return MediaItem(
            id = MediaId(id),
            type = type,
            title = name,
            year = parseYear(meta),
            runtimeMinutes = parseRuntimeMinutes(meta.str("runtime")),
            certification = meta.str("certification"),
            genres = meta.firstStrList("genres", "genre"),
            synopsis = meta.firstStr("description", "overview"),
            images = MediaImages(
                // Artwork travels as absolute URLs, which the image pipeline
                // accepts alongside bundled keys.
                poster = meta.str("poster").orEmpty(),
                backdrop = meta.firstStr("background", "poster").orEmpty(),
            ),
        )
    }

    // ------------------------------------------------------------------- meta

    /** Reads a `meta` object into a full detail record. */
    fun parseMeta(body: JsonObject): TitleDetail? {
        val meta = body.child("meta") ?: return null
        val item = parseMediaItem(meta) ?: return null
        val videos = parseEpisodes(meta, MediaId(item.id.value))

        return TitleDetail(
            item = item,
            cast = parseCast(meta),
            seasons = videos
                // Season zero is where providers put specials and extras. It is
                // hidden rather than presented first, which is where sorting by
                // number would put it.
                .filter { it.season > 0 }
                .groupBy { it.season }
                .map { (number, episodes) -> Season(number, episodes.size) }
                .sortedBy { it.number },
        )
    }

    /**
     * Cast, from either the modern credits array or the older flat list.
     *
     * `credits_cast` carries roles; `cast` is names only. Where only names are
     * available the role is left blank rather than invented.
     */
    private fun parseCast(meta: JsonObject): List<CastMember> {
        val credited = meta.objectList("credits_cast").mapNotNull { person ->
            val name = person.str("name") ?: return@mapNotNull null
            CastMember(name = name, role = person.str("character").orEmpty())
        }
        if (credited.isNotEmpty()) return credited
        return meta.strList("cast").map { CastMember(name = it, role = "") }
    }

    /**
     * Episodes, from a series' `videos`.
     *
     * The whole run arrives in one response; the detail screen asks for a
     * season at a time and filters here, because the protocol has no way to
     * request one.
     */
    fun parseEpisodes(meta: JsonObject, seriesId: MediaId): List<Episode> {
        val fallbackArtwork = meta.firstStr("background", "poster").orEmpty()
        return meta.objectList("videos").mapNotNull { video ->
            val season = video.firstInt("season", "seasonNum") ?: return@mapNotNull null
            val number = video.firstInt("episode", "number", "episodeNum")
                ?: return@mapNotNull null
            // Episode identifiers are what a stream request is keyed on, so a
            // video without one is unplayable and not worth listing.
            val id = video.str("id") ?: "${seriesId.value}:$season:$number"

            Episode(
                id = id,
                seriesId = seriesId,
                season = season,
                number = number,
                title = video.firstStr("name", "title") ?: "",
                synopsis = video.firstStr("overview", "description"),
                runtimeMinutes = parseRuntimeMinutes(video.str("runtime")),
                thumbnail = video.str("thumbnail") ?: fallbackArtwork,
                airedIso = video.firstStr("released", "firstAired"),
            )
        }
    }

    // ----------------------------------------------------------------- stream

    /**
     * Reads a `streams` array.
     *
     * A stream with no source of any kind is dropped: it cannot be played and
     * offering it would be offering the viewer a dead end.
     */
    fun parseStreams(body: JsonObject, provider: Addon): List<StreamResult> =
        body.objectList("streams").mapNotNull { stream -> parseStream(stream, provider) }

    fun parseStream(stream: JsonObject, provider: Addon): StreamResult? {
        val url = stream.str("url")
        val infoHash = stream.str("infoHash")
        val externalUrl = stream.firstStr("externalUrl", "external_url")
        val youtubeId = stream.firstStr("ytId", "youtubeId")
        if (url == null && infoHash == null && externalUrl == null && youtubeId == null) {
            return null
        }

        val name = stream.str("name")
        val title = stream.firstStr("title", "description")
        val hints = stream.child("behaviorHints")

        // The protocol has no field for resolution or codec: both are
        // conventionally written into the labels, so they are read back out of
        // them. A guess here is better than presenting every source as
        // indistinguishable.
        val describing = listOfNotNull(name, title, hints?.str("filename")).joinToString(" ")

        return StreamResult(
            id = stream.str("id"),
            name = name,
            title = title,
            url = url,
            infoHash = infoHash,
            fileIndex = stream.int("fileIdx"),
            externalUrl = externalUrl,
            youtubeId = youtubeId,
            quality = StreamDescriptors.qualityOf(describing),
            codec = StreamDescriptors.codecOf(describing),
            sizeBytes = hints?.long("videoSize") ?: StreamDescriptors.sizeOf(describing),
            language = StreamDescriptors.languageOf(describing),
            audio = StreamDescriptors.audioOf(describing),
            behaviorHints = parseStreamBehaviorHints(hints),
            subtitles = parseSubtitles(stream, provider),
            providerId = provider.id,
            providerName = provider.name,
        )
    }

    private fun parseStreamBehaviorHints(hints: JsonObject?): StreamBehaviorHints {
        if (hints == null) return StreamBehaviorHints()
        return StreamBehaviorHints(
            notWebReady = hints.bool("notWebReady"),
            bingeGroup = hints.str("bingeGroup"),
            countryWhitelist = hints.strList("countryWhitelist"),
            videoSize = hints.long("videoSize"),
            filename = hints.str("filename"),
        )
    }

    // -------------------------------------------------------------- subtitles

    /** Reads a `subtitles` array, from either the resource or inside a stream. */
    fun parseSubtitles(body: JsonObject, provider: Addon): List<SubtitleResult> =
        body.objectList("subtitles").mapNotNull { subtitle ->
            val url = subtitle.str("url") ?: return@mapNotNull null
            val language = subtitle.firstStr("lang", "language") ?: UNKNOWN_LANGUAGE
            SubtitleResult(
                id = subtitle.str("id") ?: url,
                url = url,
                language = language,
                label = subtitle.firstStr("title", "name") ?: language,
                format = SubtitleFormat.fromUrl(url),
                providerId = provider.id,
                providerName = provider.name,
            )
        }

    // ---------------------------------------------------------------- helpers

    /** Release year, from whichever of several shapes the provider used. */
    private fun parseYear(meta: JsonObject): Int? {
        meta.int("year")?.let { if (it in PLAUSIBLE_YEARS) return it }
        // "releaseInfo" is a string that may be a year, a range ("2009-2015"),
        // or an open range ("2019-").
        val fromRelease = meta.firstStr("releaseInfo", "released", "releaseDate")
        val digits = fromRelease?.take(4)?.toIntOrNull()
        return digits?.takeIf { it in PLAUSIBLE_YEARS }
    }

    /** "142 min", "142", "2h 22min" - all seen in the wild. */
    internal fun parseRuntimeMinutes(runtime: String?): Int? {
        if (runtime.isNullOrBlank()) return null
        val hours = HOURS.find(runtime)?.groupValues?.get(1)?.toIntOrNull()
        val minutes = MINUTES.find(runtime)?.groupValues?.get(1)?.toIntOrNull()
        if (hours != null) return hours * 60 + (minutes ?: 0)
        if (minutes != null) return minutes
        return runtime.trim().toIntOrNull()?.takeIf { it > 0 }
    }

    private fun JsonObject.firstInt(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { int(it) }

    private const val UNKNOWN_LANGUAGE = "und"
    private val PLAUSIBLE_YEARS = 1880..2200
    private val HOURS = Regex("""(\d+)\s*h""", RegexOption.IGNORE_CASE)
    private val MINUTES = Regex("""(\d+)\s*m""", RegexOption.IGNORE_CASE)
}
