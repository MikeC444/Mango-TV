package tv.mango.app.addon.protocol

import java.net.URLEncoder

/**
 * Builds add-on request URLs.
 *
 * The protocol addresses resources as
 *
 *     {base}/{resource}/{type}/{id}.json
 *
 * with optional arguments in a further segment before the suffix
 *
 *     {base}/catalog/movie/top/skip=100&genre=Drama.json
 *
 * The base is whatever precedes "manifest.json" in the URL the user installed.
 * A configured add-on carries its configuration inside that base as its own
 * path segment, which is why configuration needs no special handling here or
 * anywhere else: it travels with the URL.
 */
object AddonUrls {

    private const val JSON_SUFFIX = ".json"

    /** Normalises whatever the user pasted into a manifest URL. */
    fun manifestUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Add-ons are commonly shared as stremio:// links, which are the same
        // URL under a scheme this application cannot fetch.
        val withScheme = when {
            trimmed.startsWith("stremio://", ignoreCase = true) ->
                "https://" + trimmed.removePrefix("stremio://").removePrefix("stremio://")
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            // A bare host is the most common thing a viewer types.
            else -> "https://$trimmed"
        }

        val withoutFragment = withScheme.substringBefore('#')
        if (!isWellFormed(withoutFragment)) return null

        return if (withoutFragment.endsWith(JSON_SUFFIX, ignoreCase = true)) {
            withoutFragment
        } else {
            withoutFragment.trimEnd('/') + "/manifest.json"
        }
    }

    private fun isWellFormed(url: String): Boolean {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        val host = afterScheme.substringBefore('/').substringBefore('?')
        // A host with no dot and no port is a typo far more often than it is an
        // intranet name, but rejecting "localhost" would make the mock add-on
        // untestable on a device.
        return host.isNotBlank() &&
            !host.startsWith(":") &&
            (host.contains('.') || host.startsWith("localhost"))
    }

    /**
     * A resource request.
     *
     * @param extra arguments such as `skip` or `search`. Order is preserved so
     *   a cache key built from the URL is stable for the same request.
     */
    fun resourceUrl(
        baseUrl: String,
        resource: String,
        type: String,
        id: String,
        extra: List<Pair<String, String>> = emptyList(),
    ): String {
        val builder = StringBuilder(baseUrl.trimEnd('/'))
        builder.append('/').append(encodeSegment(resource))
        builder.append('/').append(encodeSegment(type))
        builder.append('/').append(encodeSegment(id))
        if (extra.isNotEmpty()) {
            builder.append('/').append(encodeExtra(extra))
        }
        builder.append(JSON_SUFFIX)
        return builder.toString()
    }

    private fun encodeExtra(extra: List<Pair<String, String>>): String =
        extra.joinToString("&") { (key, value) ->
            "${encodeSegment(key)}=${encodeSegment(value)}"
        }

    /**
     * Percent-encodes a path segment.
     *
     * [URLEncoder] targets form bodies rather than paths, so its output is
     * corrected here: a space in a path is %20 and not a plus sign, and the
     * characters below are legal in a segment and are left alone. The colon
     * matters in particular - episode identifiers look like "tt1234:1:1", and
     * escaping it produces an id no add-on recognises.
     */
    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
            .replace("+", "%20")
            .replace("%3A", ":")
            .replace("%2C", ",")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
            .replace("*", "%2A")
}
