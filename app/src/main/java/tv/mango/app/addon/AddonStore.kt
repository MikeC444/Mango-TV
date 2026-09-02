package tv.mango.app.addon

import tv.mango.app.addon.model.Addon

/**
 * The stored add-ons, as the protocol layer needs them.
 *
 * Narrow on purpose. The manager and the installer need to read the installed
 * list and write to it; they have no business knowing that it lives in
 * DataStore, and depending on the concrete repository would tie the whole
 * protocol layer to Android for the sake of two methods.
 *
 * Keeping this seam also means the layer can be exercised against an in-memory
 * store in tests - which is how the fan-out's failure isolation is verified
 * without a device.
 */
interface AddonStore {

    /** Every installed add-on, in priority order. */
    suspend fun installed(): List<Addon>

    /** Enabled add-ons only, in priority order. This is what queries use. */
    suspend fun enabled(): List<Addon>

    suspend fun install(addon: Addon)

    suspend fun updateManifest(addonId: String, addon: Addon)
}
