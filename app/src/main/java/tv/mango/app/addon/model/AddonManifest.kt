package tv.mango.app.addon.model

/**
 * A Stremio add-on's manifest, as this application understands it.
 *
 * Deliberately a plain Kotlin description rather than a mirror of the wire
 * format. Add-ons are third-party services of varying age and rigour: some
 * declare resources as bare strings, others as objects; some omit fields the
 * specification calls optional and some send fields it has never heard of. All
 * of that is absorbed by the parser, so everything above this file can rely on
 * one shape.
 *
 * Nothing here is nullable that the application would have to null-check on
 * every use. Absent lists become empty, absent flags become false.
 */
data class AddonManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val logo: String? = null,
    val background: String? = null,
    val contactEmail: String? = null,

    /** Content types the add-on deals in, across all its resources. */
    val types: List<String> = emptyList(),

    /** What the add-on can answer, and for which types and id prefixes. */
    val resources: List<AddonResource> = emptyList(),

    /**
     * Identifier namespaces the add-on recognises - "tt" for IMDb ids, for
     * instance. Empty means it has not said, which is treated as "will accept
     * anything" rather than as "accepts nothing".
     */
    val idPrefixes: List<String> = emptyList(),

    val catalogs: List<AddonCatalog> = emptyList(),
    val behaviorHints: ManifestBehaviorHints = ManifestBehaviorHints(),

    /** Fields the add-on asks the user to fill in before it will work. */
    val config: List<AddonConfigField> = emptyList(),
) {

    /** Whether this add-on can answer [resource] for [type]. */
    fun supports(resource: AddonResourceName, type: String? = null): Boolean {
        val declared = resources.firstOrNull { it.name == resource.wireName } ?: return false
        if (type == null) return true
        val supportedTypes = declared.types.ifEmpty { types }
        // An add-on that names no types is taken at its word rather than
        // assumed to support none: refusing to ask is worse than one wasted
        // request that returns nothing.
        return supportedTypes.isEmpty() || supportedTypes.contains(type)
    }

    /**
     * Whether an identifier looks like something this add-on will recognise.
     *
     * Used to avoid asking an IMDb-only add-on about a Kitsu id. An add-on that
     * declares no prefixes is asked anyway.
     */
    fun handlesId(resource: AddonResourceName, id: String): Boolean {
        val declared = resources.firstOrNull { it.name == resource.wireName } ?: return false
        val prefixes = declared.idPrefixes.ifEmpty { idPrefixes }
        return prefixes.isEmpty() || prefixes.any { id.startsWith(it) }
    }
}

/**
 * One capability declaration.
 *
 * The protocol permits either a bare string ("catalog") or an object naming the
 * types and id prefixes it applies to. Both arrive here as this, with the
 * string form inheriting the manifest's own types and prefixes.
 */
data class AddonResource(
    val name: String,
    val types: List<String> = emptyList(),
    val idPrefixes: List<String> = emptyList(),
)

/** The resources this application knows how to ask for. */
enum class AddonResourceName(val wireName: String) {
    CATALOG("catalog"),
    META("meta"),
    STREAM("stream"),
    SUBTITLES("subtitles"),
    ADDON_CATALOG("addon_catalog"),
    ;

    companion object {
        fun fromWire(value: String): AddonResourceName? =
            entries.firstOrNull { it.wireName == value }
    }
}

/** One browsable list an add-on offers. */
data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String? = null,
    val extra: List<CatalogExtra> = emptyList(),
) {
    /**
     * A catalogue that cannot be listed without an argument the home screen has
     * no way to supply - a search term, typically. Those are reachable through
     * search rather than as rows.
     */
    val requiresArgument: Boolean
        get() = extra.any { it.isRequired && it.name != EXTRA_SKIP }

    val supportsPaging: Boolean
        get() = extra.any { it.name == EXTRA_SKIP }

    val supportsSearch: Boolean
        get() = extra.any { it.name == EXTRA_SEARCH }

    companion object {
        const val EXTRA_SKIP = "skip"
        const val EXTRA_SEARCH = "search"
        const val EXTRA_GENRE = "genre"
    }
}

/** An argument a catalogue accepts or insists on. */
data class CatalogExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
    val optionsLimit: Int = 1,
)

data class ManifestBehaviorHints(
    val adult: Boolean = false,
    val p2p: Boolean = false,
    /** The add-on offers a configuration page. */
    val configurable: Boolean = false,
    /** The add-on will not work at all until it has been configured. */
    val configurationRequired: Boolean = false,
)

/** One field on an add-on's configuration form. */
data class AddonConfigField(
    val key: String,
    val type: String = "text",
    val title: String? = null,
    val options: List<String> = emptyList(),
    val required: Boolean = false,
    val default: String? = null,
)
