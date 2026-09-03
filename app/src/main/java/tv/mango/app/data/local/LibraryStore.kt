package tv.mango.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import tv.mango.app.utilities.Logger
import java.io.IOException

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mango_library",
)

/**
 * What the viewer has saved and how far through they are.
 *
 * Two pieces of state, both small: a set of saved identifiers, and a map of
 * identifier to a position and a timestamp. Everything is exposed as a [Flow],
 * so a screen showing a save button and a screen showing the library update
 * together without either knowing about the other.
 *
 * Reads and writes happen on DataStore's own dispatcher, never on the main
 * thread. Read failures are recovered rather than thrown: a corrupt or missing
 * store should leave a viewer with an empty library, not a crash on launch.
 */
class LibraryStore(
    context: Context,
) {

    private val store = context.applicationContext.libraryDataStore
    private val json = Json { ignoreUnknownKeys = true }

    val watchlist: Flow<Set<String>> = store.data
        .recoveringFromReadErrors()
        .map { preferences ->
            preferences[WATCHLIST]
                ?.let { runCatching { json.decodeFromString(SET_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
        }

    /**
     * Marked watched by the viewer, independent of playback progress - a
     * title can be marked without ever being played in this app (seen
     * elsewhere), and a title finished here is not implicitly marked, since
     * finishing already removes it from Continue Watching on its own.
     */
    val watched: Flow<Set<String>> = store.data
        .recoveringFromReadErrors()
        .map { preferences ->
            preferences[WATCHED]
                ?.let { runCatching { json.decodeFromString(SET_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
        }

    val progress: Flow<Map<String, PlaybackPosition>> = store.data
        .recoveringFromReadErrors()
        .map { preferences ->
            preferences[PROGRESS]
                ?.let { runCatching { json.decodeFromString(PROGRESS_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
        }

    suspend fun setInWatchlist(id: String, saved: Boolean) {
        store.edit { preferences ->
            val current = preferences[WATCHLIST]
                ?.let { runCatching { json.decodeFromString(SET_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
            val updated = if (saved) current + id else current - id
            preferences[WATCHLIST] = json.encodeToString(SET_SERIALIZER, updated)
        }
    }

    suspend fun setWatched(id: String, watched: Boolean) {
        store.edit { preferences ->
            val current = preferences[WATCHED]
                ?.let { runCatching { json.decodeFromString(SET_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
            val updated = if (watched) current + id else current - id
            preferences[WATCHED] = json.encodeToString(SET_SERIALIZER, updated)
        }
    }

    /**
     * Records how far through a title the viewer is.
     *
     * A position at or beyond [COMPLETE_THRESHOLD] is treated as finished and
     * removed, so a title watched to the end stops offering to resume and
     * leaves Continue Watching on its own.
     */
    suspend fun setProgress(id: String, position: PlaybackPosition) {
        store.edit { preferences ->
            val current = preferences[PROGRESS]
                ?.let { runCatching { json.decodeFromString(PROGRESS_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
            val updated = if (position.fraction >= COMPLETE_THRESHOLD || position.fraction <= 0f) {
                current - id
            } else {
                current + (id to position)
            }
            preferences[PROGRESS] = json.encodeToString(PROGRESS_SERIALIZER, updated)
        }
    }

    /** Explicit removal, for "Remove from Continue Watching" - not just a position update. */
    suspend fun removeProgress(id: String) {
        store.edit { preferences ->
            val current = preferences[PROGRESS]
                ?.let { runCatching { json.decodeFromString(PROGRESS_SERIALIZER, it) }.getOrNull() }
                .orEmpty()
            preferences[PROGRESS] = json.encodeToString(PROGRESS_SERIALIZER, current - id)
        }
    }

    /**
     * DataStore surfaces read failures into the flow. Recovering here means a
     * damaged store costs the viewer their saved list, not the application.
     */
    private fun Flow<Preferences>.recoveringFromReadErrors(): Flow<Preferences> =
        catch { error ->
            if (error is IOException) {
                Logger.e("Could not read the library store", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }

    private companion object {
        val WATCHLIST = stringPreferencesKey("watchlist")
        val WATCHED = stringPreferencesKey("watched")
        val PROGRESS = stringPreferencesKey("progress")

        val SET_SERIALIZER = SetSerializer(String.serializer())
        val PROGRESS_SERIALIZER = MapSerializer(String.serializer(), PlaybackPosition.serializer())

        /** Past this, a title counts as watched rather than in progress. */
        const val COMPLETE_THRESHOLD = 0.95f
    }
}
