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
 * Nothing in the model layer holds a bitmap, or a URL. Keys are resolved by
 * [tv.mango.app.cache.ArtworkSource] at the moment an image is actually
 * needed, at the size it is actually needed.
 */
data class MediaImages(
    val poster: String,
    val backdrop: String,
)

/**
 * A title, as the browsing interface needs it.
 *
 * Carries enough for a card and for the hero above it - the hero has to follow
 * the focused card without a round trip, so a short synopsis and the metadata
 * line travel with the list. It deliberately stops there: cast, seasons and
 * episode lists belong to the one title being looked at, not to the forty in a
 * row, and are fetched by the detail screen.
 */
data class MediaItem(
    val id: MediaId,
    val type: MediaType,
    val title: String,

    /**
     * Everything below the title is optional.
     *
     * A bundled record carries all of it, but a catalogue row from an add-on
     * routinely carries an identifier, a name and a poster and nothing else -
     * the rest arrives only when the title is opened and its metadata is
     * fetched. Requiring these would mean either inventing values at the parser
     * or refusing perfectly good content.
     */
    val year: Int? = null,
    val runtimeMinutes: Int? = null,
    val certification: String? = null,
    val genres: List<String> = emptyList(),
    /** An add-on's own rating for the title (e.g. an IMDb score), as text - never computed here. */
    val rating: String? = null,
    val synopsis: String? = null,
    val images: MediaImages,
    /**
     * How far through the viewer is, from 0 to 1. Anything above zero means
     * the title is partly watched and should offer to resume.
     */
    val progress: Float = 0f,

    /**
     * Set only for a card synthesised for the Continue Watching row - never
     * for anything read from a catalogue. Its presence is what tells a card
     * tap to resume playback directly instead of opening the detail screen,
     * and what tells the hero to describe the episode and time remaining
     * instead of a title's usual year and genres.
     */
    val resume: ContinueWatchingItem? = null,

    /**
     * Whether the viewer has explicitly marked this title watched - see
     * [tv.mango.app.repository.LibraryRepository.isWatched]. Never set by a
     * catalogue provider; only [tv.mango.app.ui.home.HomeViewModel] populates
     * it, by combining a row's items with the library's own watched marks, so
     * Home's optional watched badge (Settings -> Home Screen -> Catalog Rows)
     * has something real to show.
     */
    val watched: Boolean = false,
) {
    val isPartiallyWatched: Boolean get() = progress > 0f
}

/** A titled, horizontally scrolling group of titles. */
data class ContentRow(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
)

/** Everything the home screen needs, resolved in one pass. */
data class HomeContent(
    val featured: MediaItem,
    val rows: List<ContentRow>,
)
