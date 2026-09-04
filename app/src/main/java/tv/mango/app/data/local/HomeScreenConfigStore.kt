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
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.utilities.Logger
import java.io.IOException

private val Context.homeScreenConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mango_home_screen_config",
)

/**
 * Persists a viewer's whole Home Screen customisation as one JSON value.
 *
 * A single key rather than one per field, unlike [LibraryStore]: everything on
 * this page changes together under a preset or a reset, and a viewer editing
 * one screen still wants every other screen's settings read back exactly as
 * left. One document is also one place a future field can be added to
 * [HomeScreenConfig] without a migration - `ignoreUnknownKeys` and each field's
 * own default cover a viewer's store predating that field, and a store from a
 * build that is newer than this one reading an unrecognised key back.
 *
 * Kept entirely separate from every other DataStore file in the application:
 * a corrupt or reset appearance store can never take watch history, a
 * watchlist or add-ons with it.
 */
class HomeScreenConfigStore(
    context: Context,
) {

    private val store = context.applicationContext.homeScreenConfigDataStore
    private val json = Json { ignoreUnknownKeys = true }

    val config: Flow<HomeScreenConfig> = store.data
        .recoveringFromReadErrors()
        .map { preferences ->
            preferences[CONFIG]
                ?.let { runCatching { json.decodeFromString(HomeScreenConfig.serializer(), it) }.getOrNull() }
                ?: HomeScreenConfig.default()
        }

    suspend fun save(config: HomeScreenConfig) {
        store.edit { preferences ->
            preferences[CONFIG] = json.encodeToString(HomeScreenConfig.serializer(), config)
        }
    }

    /**
     * Reads, transforms and writes back within one DataStore transaction, so
     * two settings rows changed in quick succession can never race each other
     * into overwriting one field with a stale copy of the rest.
     */
    suspend fun update(transform: (HomeScreenConfig) -> HomeScreenConfig) {
        store.edit { preferences ->
            val current = preferences[CONFIG]
                ?.let { runCatching { json.decodeFromString(HomeScreenConfig.serializer(), it) }.getOrNull() }
                ?: HomeScreenConfig.default()
            preferences[CONFIG] = json.encodeToString(HomeScreenConfig.serializer(), transform(current))
        }
    }

    private fun Flow<Preferences>.recoveringFromReadErrors(): Flow<Preferences> =
        catch { error ->
            if (error is IOException) {
                Logger.e("Could not read the home screen appearance store", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }

    private companion object {
        val CONFIG = stringPreferencesKey("home_screen_config")
    }
}
