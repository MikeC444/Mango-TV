package tv.mango.app.addon.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import tv.mango.app.addon.model.AddonCatalog
import tv.mango.app.addon.model.AddonConfigField
import tv.mango.app.addon.model.AddonManifest
import tv.mango.app.addon.model.AddonResource
import tv.mango.app.addon.model.CatalogExtra
import tv.mango.app.addon.model.ManifestBehaviorHints

/**
 * Reads an add-on manifest.
 *
 * The only hard requirements are an id, a name and a version - without those
 * there is nothing to install or to show the user. Everything else is optional
 * and absent fields become empty rather than failing the parse, because a
 * manifest that omits a field the specification calls optional is a valid
 * manifest, and a manifest carrying fields this application has never heard of
 * is a manifest from a newer specification than this one.
 */
object AddonManifestParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    sealed interface Result {
        data class Valid(val manifest: AddonManifest) : Result
        data class Invalid(val reason: InvalidReason) : Result
    }

    enum class InvalidReason {
        /** The body was not JSON at all. */
        NOT_JSON,

        /** Valid JSON, but not an object. */
        NOT_AN_OBJECT,

        /** No id, name or version. */
        MISSING_REQUIRED_FIELDS,

        /** Nothing this application knows how to ask for. */
        NO_USABLE_RESOURCES,
    }

    fun parse(body: String): Result {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: return Result.Invalid(InvalidReason.NOT_JSON)
        val manifest = root as? JsonObject
            ?: return Result.Invalid(InvalidReason.NOT_AN_OBJECT)
        return parse(manifest)
    }

    fun parse(root: JsonObject): Result {
        val id = root.str("id") ?: return Result.Invalid(InvalidReason.MISSING_REQUIRED_FIELDS)
        val name = root.str("name") ?: return Result.Invalid(InvalidReason.MISSING_REQUIRED_FIELDS)
        val version = root.str("version")
            ?: return Result.Invalid(InvalidReason.MISSING_REQUIRED_FIELDS)

        val types = root.strList("types")
        val idPrefixes = root.firstStrList("idPrefixes", "idPrefix")
        val resources = parseResources(root, types, idPrefixes)

        if (resources.isEmpty()) return Result.Invalid(InvalidReason.NO_USABLE_RESOURCES)

        return Result.Valid(
            AddonManifest(
                id = id,
                name = name,
                version = version,
                description = root.str("description"),
                logo = root.str("logo"),
                background = root.str("background"),
                contactEmail = root.str("contactEmail"),
                types = types,
                resources = resources,
                idPrefixes = idPrefixes,
                catalogs = parseCatalogs(root),
                behaviorHints = parseBehaviorHints(root),
                config = parseConfig(root),
            ),
        )
    }

    /**
     * Resources come in two shapes and both are current.
     *
     * A bare string names a capability and inherits the manifest's own types
     * and id prefixes. An object narrows it to particular ones. Older add-ons
     * use the first, newer and more selective ones the second, and a single
     * manifest may mix them in one array.
     */
    private fun parseResources(
        root: JsonObject,
        manifestTypes: List<String>,
        manifestIdPrefixes: List<String>,
    ): List<AddonResource> = root.array("resources").mapNotNull { element ->
        element.asStringOrNull()?.let { name ->
            return@mapNotNull AddonResource(
                name = name,
                types = manifestTypes,
                idPrefixes = manifestIdPrefixes,
            )
        }

        val declared = element.asObjectOrNull() ?: return@mapNotNull null
        val name = declared.str("name") ?: return@mapNotNull null
        AddonResource(
            name = name,
            // An object that narrows nothing falls back to the manifest, so the
            // two shapes describe the same add-on identically.
            types = declared.strList("types").ifEmpty { manifestTypes },
            idPrefixes = declared.firstStrList("idPrefixes", "idPrefix")
                .ifEmpty { manifestIdPrefixes },
        )
    }.distinctBy { it.name }

    private fun parseCatalogs(root: JsonObject): List<AddonCatalog> =
        root.objectList("catalogs").mapNotNull { entry ->
            val type = entry.str("type") ?: return@mapNotNull null
            val id = entry.str("id") ?: return@mapNotNull null
            AddonCatalog(
                type = type,
                id = id,
                name = entry.str("name"),
                extra = parseCatalogExtra(entry),
            )
        }

    /**
     * Catalogue arguments, from either the current or the superseded form.
     *
     * The current form is a single `extra` array of objects. Before it, the
     * same information was two arrays of bare names, `extraRequired` and
     * `extraSupported`. Both are still in the wild.
     */
    private fun parseCatalogExtra(catalog: JsonObject): List<CatalogExtra> {
        val modern = catalog.objectList("extra").mapNotNull { extra ->
            val name = extra.str("name") ?: return@mapNotNull null
            CatalogExtra(
                name = name,
                isRequired = extra.bool("isRequired"),
                options = extra.strList("options"),
                optionsLimit = extra.int("optionsLimit") ?: 1,
            )
        }
        if (modern.isNotEmpty()) return modern

        val required = catalog.strList("extraRequired")
        val supported = catalog.strList("extraSupported")
        return (required + supported).distinct().map { name ->
            CatalogExtra(name = name, isRequired = name in required)
        }
    }

    private fun parseBehaviorHints(root: JsonObject): ManifestBehaviorHints {
        val hints = root.child("behaviorHints") ?: return ManifestBehaviorHints()
        return ManifestBehaviorHints(
            adult = hints.bool("adult"),
            p2p = hints.bool("p2p"),
            configurable = hints.bool("configurable"),
            configurationRequired = hints.bool("configurationRequired"),
        )
    }

    private fun parseConfig(root: JsonObject): List<AddonConfigField> =
        root.objectList("config").mapNotNull { field ->
            val key = field.str("key") ?: return@mapNotNull null
            AddonConfigField(
                key = key,
                type = field.str("type") ?: "text",
                title = field.str("title"),
                options = field.strList("options"),
                required = field.bool("required"),
                default = field.str("default"),
            )
        }
}
