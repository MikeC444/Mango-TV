package tv.mango.app.addon

import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.AddonCatalog
import tv.mango.app.addon.model.AddonManifest
import tv.mango.app.addon.model.AddonResource

/** An in-memory [AddonStore], so the protocol layer can be tested off-device. */
class FakeAddonStore(initial: List<Addon> = emptyList()) : AddonStore {

    val addons = initial.toMutableList()

    override suspend fun installed(): List<Addon> = addons.sortedBy { it.priority }

    override suspend fun enabled(): List<Addon> = installed().filter { it.isEnabled }

    override suspend fun install(addon: Addon) {
        addons.removeAll { it.id == addon.id }
        addons += addon
    }

    override suspend fun updateManifest(addonId: String, addon: Addon) {
        val index = addons.indexOfFirst { it.id == addonId }
        if (index >= 0) addons[index] = addon
    }
}

/** Builds an add-on with just enough manifest to route a request to it. */
fun testAddon(
    id: String,
    baseUrl: String,
    resources: List<String> = listOf("catalog", "meta", "stream", "subtitles"),
    types: List<String> = listOf("movie", "series"),
    idPrefixes: List<String> = emptyList(),
    catalogs: List<AddonCatalog> = emptyList(),
    priority: Int = 0,
    enabled: Boolean = true,
): Addon = Addon(
    manifestUrl = "$baseUrl/manifest.json",
    manifest = AddonManifest(
        id = id,
        name = id,
        version = "1.0.0",
        types = types,
        idPrefixes = idPrefixes,
        resources = resources.map { AddonResource(it, types, idPrefixes) },
        catalogs = catalogs,
    ),
    isEnabled = enabled,
    priority = priority,
)
