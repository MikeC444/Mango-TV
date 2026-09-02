package tv.mango.app.data.local

import kotlinx.serialization.Serializable

/**
 * How far through a title the viewer is, when they last were, and enough of a
 * snapshot to render a Continue Watching card without a second trip through
 * the catalogue.
 *
 * The snapshot exists because the identifier this is keyed by is a series or
 * a film's own id, but what actually needs to resume - and what the home
 * screen needs to show underneath "S3 E7" - is whichever episode was last
 * played. Re-deriving that from the catalogue on every home screen load
 * would mean either a second network round trip per row entry or an add-on
 * that can look episodes up by id in isolation, and this application assumes
 * neither.
 */
@Serializable
data class PlaybackPosition(
    val fraction: Float,
    val updatedAtMillis: Long,
    val title: String,
    /** [tv.mango.app.models.MediaType] as a string, so this file stays free of a UI-facing import. */
    val mediaType: String,
    val posterKey: String,
    val backdropKey: String,
    val runtimeMinutes: Int? = null,
    val episodeId: String? = null,
    val episodeSeason: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
)
