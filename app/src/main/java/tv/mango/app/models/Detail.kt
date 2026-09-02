package tv.mango.app.models

/** A performer and the part they play. */
data class CastMember(
    val name: String,
    val role: String,
)

/** One season of a series, and how many episodes it holds. */
data class Season(
    val number: Int,
    val episodeCount: Int,
)

data class Episode(
    val id: String,
    val seriesId: MediaId,
    val season: Int,
    val number: Int,
    val title: String,
    /** Absent on providers that give an episode list but no descriptions. */
    val synopsis: String? = null,
    val runtimeMinutes: Int? = null,
    /** A still where the provider has one; otherwise the series' own artwork. */
    val thumbnail: String,
    val airedIso: String? = null,
    val progress: Float = 0f,
)

/**
 * Everything a detail screen shows.
 *
 * Loaded only for the one title being looked at. Keeping it apart from
 * [MediaItem] is what lets a row of forty cards stay small: a card needs a
 * title and a poster, not a cast list and two seasons of episodes.
 */
data class TitleDetail(
    val item: MediaItem,
    val cast: List<CastMember>,
    /** Empty for a film. */
    val seasons: List<Season>,
)
