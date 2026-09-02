package tv.mango.app.addon.model

/**
 * An add-on the user has installed.
 *
 * The manifest is stored alongside the URL it came from, so the application
 * starts up knowing what every add-on can do without going to the network
 * first. It is refreshed in the background, not on the cold-start path.
 */
data class Addon(
    /** The URL the user supplied. Carries the add-on's configuration, if any. */
    val manifestUrl: String,
    val manifest: AddonManifest,

    /**
     * The manifest exactly as the add-on sent it.
     *
     * Kept so the record can be stored verbatim and re-parsed, which means a
     * field this version does not model survives being installed by it.
     */
    val manifestJson: String = "",

    val isEnabled: Boolean = true,
    /**
     * Lower sorts first. Ties are broken by installation order, so a list the
     * user has never reordered stays in the order they built it.
     */
    val priority: Int = 0,
    val installedAtMillis: Long = 0L,
    val manifestRefreshedAtMillis: Long = 0L,
) {
    val id: String get() = manifest.id
    val name: String get() = manifest.name

    /**
     * Where resource requests are addressed.
     *
     * Everything before "/manifest.json". For a configured add-on the
     * configuration is already a path segment inside this, which is why
     * configuration needs no special handling anywhere else.
     */
    val baseUrl: String get() = manifestUrl.removeSuffix(MANIFEST_SUFFIX).trimEnd('/')

    companion object {
        const val MANIFEST_SUFFIX = "manifest.json"
    }
}
