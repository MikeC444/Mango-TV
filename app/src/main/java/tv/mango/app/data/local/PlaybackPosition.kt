package tv.mango.app.data.local

import kotlinx.serialization.Serializable

/**
 * How far through a title the viewer is, and when they last were.
 *
 * The timestamp is what lets a library order titles by how recently they were
 * watched, which is the ordering that actually matters for resuming.
 */
@Serializable
data class PlaybackPosition(
    val fraction: Float,
    val updatedAtMillis: Long,
)
