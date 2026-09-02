package tv.mango.app.models

/**
 * Identity for anything in the catalogue.
 *
 * A value class rather than a bare String, so a media id can never be passed
 * where a title or an artwork key was meant. It costs nothing at runtime - the
 * compiler erases it back to a String.
 */
@JvmInline
value class MediaId(val value: String)

enum class MediaType { MOVIE, SERIES }

/**
 * Artwork references, held as keys rather than as anything already loaded.
 *
 * Mock content resolves these against bundled artwork; a real provider resolves
 * them to URLs. Nothing above this layer knows the difference, and nothing here
 * holds a bitmap.
 */
data class MediaImages(
    val poster: String,
    val backdrop: String,
)

/**
 * The projection a card needs, and nothing more.
 *
 * A row of forty cards holds forty of these, so it deliberately excludes
 * descriptions, cast and episode lists. Those are loaded by the detail screen
 * for the one title being looked at.
 */
data class MediaItem(
    val id: MediaId,
    val type: MediaType,
    val title: String,
    val year: Int,
    val images: MediaImages,
)

/** A titled, horizontally scrolling group of titles on the home screen. */
data class ContentRow(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
)
