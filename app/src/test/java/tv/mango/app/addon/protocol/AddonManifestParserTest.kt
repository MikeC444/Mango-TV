package tv.mango.app.addon.protocol

import tv.mango.app.addon.model.AddonResourceName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The manifest parser is the application's first contact with an untrusted
 * third-party service, so these cover the shapes real add-ons actually send
 * rather than only the shape the specification describes.
 */
class AddonManifestParserTest {

    private fun valid(body: String) =
        (AddonManifestParser.parse(body) as? AddonManifestParser.Result.Valid)?.manifest

    private fun invalidReason(body: String) =
        (AddonManifestParser.parse(body) as? AddonManifestParser.Result.Invalid)?.reason

    @Test
    fun `reads a complete manifest`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "org.example.addon",
                  "name": "Example",
                  "version": "1.2.0",
                  "description": "An example add-on",
                  "logo": "https://example.test/logo.png",
                  "background": "https://example.test/bg.jpg",
                  "types": ["movie", "series"],
                  "resources": ["catalog", "meta", "stream", "subtitles"],
                  "idPrefixes": ["tt"],
                  "catalogs": [
                    { "type": "movie", "id": "top", "name": "Top Movies" }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("org.example.addon", manifest.id)
        assertEquals("Example", manifest.name)
        assertEquals("1.2.0", manifest.version)
        assertEquals(listOf("movie", "series"), manifest.types)
        assertEquals(4, manifest.resources.size)
        assertEquals(1, manifest.catalogs.size)
        assertEquals("Top Movies", manifest.catalogs.first().name)
    }

    @Test
    fun `resources declared as bare strings inherit the manifest types and prefixes`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "idPrefixes": ["tt"],
                  "resources": ["stream"]
                }
                """.trimIndent(),
            ),
        )

        val stream = manifest.resources.single()
        assertEquals("stream", stream.name)
        assertEquals(listOf("movie"), stream.types)
        assertEquals(listOf("tt"), stream.idPrefixes)
    }

    @Test
    fun `resources declared as objects narrow the manifest declaration`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie", "series", "channel"],
                  "idPrefixes": ["tt", "kitsu"],
                  "resources": [
                    { "name": "stream", "types": ["series"], "idPrefixes": ["kitsu"] }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val stream = manifest.resources.single()
        assertEquals(listOf("series"), stream.types)
        assertEquals(listOf("kitsu"), stream.idPrefixes)
        assertTrue(manifest.supports(AddonResourceName.STREAM, "series"))
        assertFalse(manifest.supports(AddonResourceName.STREAM, "movie"))
    }

    @Test
    fun `a manifest may mix string and object resource declarations`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "resources": [
                    "catalog",
                    { "name": "stream", "types": ["movie"], "idPrefixes": ["tt"] }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("catalog", "stream"), manifest.resources.map { it.name })
    }

    @Test
    fun `id prefixes decide whether an add-on is asked about an identifier`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "idPrefixes": ["tt"],
                  "resources": ["meta"]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(manifest.handlesId(AddonResourceName.META, "tt0111161"))
        assertFalse(manifest.handlesId(AddonResourceName.META, "kitsu:42"))
    }

    @Test
    fun `an add-on declaring no prefixes is asked about anything`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "resources": ["meta"]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(manifest.handlesId(AddonResourceName.META, "anything-at-all"))
    }

    @Test
    fun `unknown fields are ignored rather than rejected`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "resources": ["catalog"],
                  "types": ["movie"],
                  "somethingFromANewerSpec": { "nested": [1, 2, 3] },
                  "anotherUnknown": true
                }
                """.trimIndent(),
            ),
        )

        assertEquals("a", manifest.id)
    }

    @Test
    fun `configuration fields and hints are read`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "resources": ["stream"],
                  "behaviorHints": { "configurable": true, "configurationRequired": true },
                  "config": [
                    { "key": "apiKey", "type": "text", "title": "API key", "required": true }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(manifest.behaviorHints.configurable)
        assertTrue(manifest.behaviorHints.configurationRequired)
        val field = manifest.config.single()
        assertEquals("apiKey", field.key)
        assertTrue(field.required)
    }

    @Test
    fun `catalog arguments are read from the superseded extraRequired form`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "resources": ["catalog"],
                  "catalogs": [
                    {
                      "type": "movie", "id": "search",
                      "extraRequired": ["search"],
                      "extraSupported": ["search", "skip"]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        val catalog = manifest.catalogs.single()
        assertTrue(catalog.supportsSearch)
        assertTrue(catalog.supportsPaging)
        // A catalogue that cannot be listed without a search term is not a row.
        assertTrue(catalog.requiresArgument)
    }

    @Test
    fun `a catalog needing only paging is listable as a row`() {
        val manifest = assertNotNull(
            valid(
                """
                {
                  "id": "a", "name": "A", "version": "1.0.0",
                  "types": ["movie"],
                  "resources": ["catalog"],
                  "catalogs": [
                    {
                      "type": "movie", "id": "top",
                      "extra": [{ "name": "skip", "isRequired": false }]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertFalse(manifest.catalogs.single().requiresArgument)
    }

    @Test
    fun `missing required fields are rejected`() {
        assertEquals(
            AddonManifestParser.InvalidReason.MISSING_REQUIRED_FIELDS,
            invalidReason("""{ "name": "No id", "version": "1.0.0", "resources": ["meta"] }"""),
        )
        assertEquals(
            AddonManifestParser.InvalidReason.MISSING_REQUIRED_FIELDS,
            invalidReason("""{ "id": "a", "version": "1.0.0", "resources": ["meta"] }"""),
        )
        assertEquals(
            AddonManifestParser.InvalidReason.MISSING_REQUIRED_FIELDS,
            invalidReason("""{ "id": "a", "name": "A", "resources": ["meta"] }"""),
        )
    }

    @Test
    fun `a manifest offering nothing usable is rejected`() {
        assertEquals(
            AddonManifestParser.InvalidReason.NO_USABLE_RESOURCES,
            invalidReason("""{ "id": "a", "name": "A", "version": "1.0.0", "resources": [] }"""),
        )
    }

    @Test
    fun `malformed input is reported rather than thrown`() {
        assertEquals(AddonManifestParser.InvalidReason.NOT_JSON, invalidReason("not json at all"))
        assertEquals(AddonManifestParser.InvalidReason.NOT_JSON, invalidReason(""))
        assertEquals(AddonManifestParser.InvalidReason.NOT_AN_OBJECT, invalidReason("""["a"]"""))
        assertNull(valid("{"))
    }
}
