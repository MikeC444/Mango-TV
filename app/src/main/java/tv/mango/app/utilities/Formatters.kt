package tv.mango.app.utilities

import android.content.Context
import tv.mango.app.R
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType

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
            length,
            item.certification,
            item.genres.joinToString(", ").takeIf { it.isNotEmpty() },
        ).filter { it.isNotBlank() }.joinToString(SEPARATOR)
    }

    /** A middle dot with air around it; a slash reads as cramped at distance. */
    private const val SEPARATOR = "  ·  "
}
