package tv.mango.app.addon

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.protocol.AddonManifestParser
import tv.mango.app.utilities.Logger
import java.io.IOException

private val Context.addonDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mango_addons",
)

/**
 * The add-ons the user has installed.
 *
 * Each record keeps the manifest exactly as the add-on sent it, as text, and
 * re-parses it on load rather than storing a translated copy. Two reasons: the
 * application starts up knowing what every add-on can do without going to the
 * network first, and a manifest field this version does not model is not
 * silently destroyed by being stored - a later version reads it from the same
 * record.
 *
 * A record that can no longer be parsed is dropped on read rather than
 * propagated. That can only happen if a stored manifest was truncated, and a
 * half-understood add-on is worse than an absent one.
 */
class AddonRepository(
    context: Context,
) : AddonStore {

    private val store = context.applicationContext.addonDataStore
    private val json = Json { ignoreUnknownKeys = true }

    /** Installed add-ons in priority order, whether enabled or not. */
    val addons: Flow<List<Addon>> = store.data
        .catch { error ->
            if (error is IOException) {
                Logger.e("Could not read installed add-ons", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> decode(preferences[INSTALLED]) }

    override suspend fun installed(): List<Addon> = addons.first()

    /** Enabled add-ons only, in priority order. This is what queries use. */
    override suspend fun enabled(): List<Addon> = installed().filter { it.isEnabled }

    /**
     * Adds an add-on, or replaces one already installed under the same id.
     *
     * Reinstalling is how an add-on is reconfigured: the configuration lives in
     * the manifest URL, so a reconfigured add-on is the same add-on at a new
     * URL. Its position in the priority order is kept, because the user chose
     * it and did not ask for it to change.
     */
    override suspend fun install(addon: Addon) {
        edit { current ->
            val existing = current.firstOrNull { it.id == addon.id }
            val withoutExisting = current.filterNot { it.id == addon.id }
            val placed = addon.copy(
                priority = existing?.priority ?: nextPriority(withoutExisting),
                installedAtMillis = existing?.installedAtMillis ?: addon.installedAtMillis,
            )
            withoutExisting + placed
        }
    }

    suspend fun remove(addonId: String) {
        edit { current -> current.filterNot { it.id == addonId } }
    }

    suspend fun setEnabled(addonId: String, enabled: Boolean) {
        edit { current ->
            current.map { if (it.id == addonId) it.copy(isEnabled = enabled) else it }
        }
    }

    /**
     * Reorders the whole list.
     *
     * Takes the complete order rather than a move instruction, so the stored
     * priorities cannot drift out of step with what the user was looking at.
     */
    suspend fun reorder(addonIdsInOrder: List<String>) {
        edit { current ->
            val ranked = addonIdsInOrder.withIndex().associate { (index, id) -> id to index }
            current.map { addon ->
                addon.copy(priority = ranked[addon.id] ?: (ranked.size + addon.priority))
            }
        }
    }

    /** Replaces a stored manifest after a background refresh. */
    override suspend fun updateManifest(addonId: String, addon: Addon) {
        edit { current ->
            current.map {
                if (it.id == addonId) {
                    addon.copy(
                        isEnabled = it.isEnabled,
                        priority = it.priority,
                        installedAtMillis = it.installedAtMillis,
                    )
                } else {
                    it
                }
            }
        }
    }

    private suspend fun edit(transform: (List<Addon>) -> List<Addon>) {
        store.edit { preferences ->
            val updated = transform(decode(preferences[INSTALLED])).sortedBy { it.priority }
            preferences[INSTALLED] = encode(updated)
        }
    }

    private fun nextPriority(current: List<Addon>): Int =
        (current.maxOfOrNull { it.priority } ?: -1) + 1

    private fun encode(addons: List<Addon>): String = json.encodeToString(
        ListSerializer(StoredAddon.serializer()),
        addons.map { addon ->
            StoredAddon(
                manifestUrl = addon.manifestUrl,
                manifestJson = addon.manifestJson,
                isEnabled = addon.isEnabled,
                priority = addon.priority,
                installedAtMillis = addon.installedAtMillis,
                manifestRefreshedAtMillis = addon.manifestRefreshedAtMillis,
            )
        },
    )

    private fun decode(raw: String?): List<Addon> {
        if (raw.isNullOrBlank()) return emptyList()
        val stored = runCatching {
            json.decodeFromString(ListSerializer(StoredAddon.serializer()), raw)
        }.getOrElse {
            Logger.e("Installed add-ons could not be read; starting empty", it)
            return emptyList()
        }

        return stored.mapNotNull { record ->
            val parsed = AddonManifestParser.parse(record.manifestJson)
            val manifest = (parsed as? AddonManifestParser.Result.Valid)?.manifest
                ?: return@mapNotNull null
            Addon(
                manifestUrl = record.manifestUrl,
                manifest = manifest,
                manifestJson = record.manifestJson,
                isEnabled = record.isEnabled,
                priority = record.priority,
                installedAtMillis = record.installedAtMillis,
                manifestRefreshedAtMillis = record.manifestRefreshedAtMillis,
            )
        }.sortedBy { it.priority }
    }

    /**
     * The stored shape.
     *
     * Holds only what is needed to reconstruct the add-on. No credentials are
     * separated out and stored here: where an add-on requires a key, that key
     * is part of the manifest URL the user installed, and it stays there.
     */
    @Serializable
    private data class StoredAddon(
        val manifestUrl: String,
        val manifestJson: String,
        val isEnabled: Boolean,
        val priority: Int,
        val installedAtMillis: Long,
        val manifestRefreshedAtMillis: Long,
    )

    private companion object {
        val INSTALLED = stringPreferencesKey("installed_addons")
    }
}
