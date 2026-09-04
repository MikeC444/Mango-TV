package tv.mango.app.utilities

import android.content.Context
import tv.mango.app.R
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import java.util.Locale

/**
 * How the application writes things down.
 *
 * Centralised so a runtime or a metadata line reads identically wherever it
 * appears, and so the separator and ordering can be changed in one place.
 */
object Formatters {

    /** "2h 21m", or "48m" for anything under an hour. */
    fun runtime(context: Context, minutes: Int): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return if (hours == 0) {
            context.getString(R.string.format_minutes, remainder)
        } else {
            context.getString(R.string.format_hours_minutes, hours, remainder)
        }
    }

    /**
     * The single metadata line beneath a title.
     *
     * Year, length, certification, genres - most to least useful, so the line
     * degrades sensibly when it has to be truncated on a narrow layout. For a
     * series the runtime describes a typical episode rather than the whole run,
     * which is the number a viewer is actually deciding on.
     */
    fun metadataLine(context: Context, item: MediaItem): String {
        // A Continue Watching card describes itself differently: which
        // episode, and how much of it is left, is what a viewer deciding
        // whether to resume actually wants - not the show's year or genres.
        val resume = item.resume
        if (resume != null) {
            return listOfNotNull(
                resume.episodeLabel,
                resume.remainingMinutes?.let { context.getString(R.string.format_minutes_remaining, it) },
            ).joinToString(SEPARATOR)
        }

        // Any of these may be absent on a title whose metadata has not been
        // fetched, or whose provider simply does not carry it. The line is
        // assembled from whatever is known and closes up around the rest, so a
        // sparse record reads as a shorter line rather than as a broken one.
        val length = item.runtimeMinutes?.let { minutes ->
            when (item.type) {
                MediaType.MOVIE -> runtime(context, minutes)
                MediaType.SERIES ->
                    context.getString(R.string.format_per_episode, runtime(context, minutes))
            }
        }
        return listOfNotNull(
            item.year?.toString(),
            item.rating?.let { context.getString(R.string.format_rating, it) },
            length,
            item.certification,
            item.genres.joinToString(", ").takeIf { it.isNotEmpty() },
        ).filter { it.isNotBlank() }.joinToString(SEPARATOR)
    }

    /**
     * The optional caption line under a card's artwork, built from exactly the
     * fields a row's own Information toggles ask for - see Settings -> Home
     * Screen -> Catalog Rows -> Edit Row. Never includes anything toggled off,
     * and returns `null` rather than an empty string when everything is.
     */
    fun cardCaptionLine(
        context: Context,
        item: MediaItem,
        showYear: Boolean,
        showRating: Boolean,
        showRuntime: Boolean,
    ): String? {
        val length = item.runtimeMinutes.takeIf { showRuntime }?.let { runtime(context, it) }
        val parts = listOfNotNull(
            item.year?.toString().takeIf { showYear },
            item.rating?.let { context.getString(R.string.format_rating, it) }.takeIf { showRating },
            length,
        ).filter { it.isNotBlank() }
        return parts.joinToString(SEPARATOR).takeIf { it.isNotEmpty() }
    }

    /**
     * "1.4 GB", "700 MB" - a stream's size, for the source picker.
     *
     * Formatted with a fixed locale rather than the device's own: this is a
     * decimal number in an English-only interface, not localised text, and
     * the device's locale would otherwise decide whether the separator is a
     * period or a comma.
     */
    fun fileSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) return String.format(Locale.US, "%.1f GB", gb)
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.0f MB", mb)
    }

    /** A middle dot with air around it; a slash reads as cramped at distance. */
    const val SEPARATOR = "  ·  "
}
