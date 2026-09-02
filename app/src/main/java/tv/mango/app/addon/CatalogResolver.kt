package tv.mango.app.addon

import tv.mango.app.addon.model.Addon
import tv.mango.app.addon.model.AddonCatalog
import tv.mango.app.addon.model.AddonResourceName
import tv.mango.app.addon.protocol.AddonUrls
import tv.mango.app.addon.protocol.CachePolicy
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.addon.protocol.StremioResponseParser
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType

/**
 * Builds the browsing surfaces out of add-on catalogues.
 *
 * An add-on may publish many catalogues and a user may install many add-ons, so
 * the number of possible rows grows as the product of the two. The home screen
 * takes a bounded slice of that in priority order rather than all of it: a
 * viewer cannot see thirty rows, and asking for thirty on a Fire Stick would be
 * thirty requests and thirty sets of artwork for a screen that shows four.
 */
class CatalogResolver(
    private val manager: AddonManager,
    private val client: StremioProtocolClient,
) {

    /** One add-on catalogue, resolved into a row of cards. */
    data class ResolvedRow(val row: ContentRow, val addon: Addon)

    /**
     * The rows for the home screen.
     *
     * Catalogues that cannot be listed without an argument - a search term -
     * are skipped, since the home screen has nothing to supply. They are
     * reachable through search instead.
     */
    suspend fun homeRows(limit: Int = MAX_HOME_ROWS): Aggregated<ResolvedRow> {
        val addons = manager.capableOf(AddonResourceName.CATALOG)
        if (addons.isEmpty()) return Aggregated()

        // Flattened in priority order and then capped, so a high-priority
        // add-on's catalogues all appear before a low-priority one's rather
        // than each add-on contributing its first before anyone contributes a
        // second.
        val requests = addons.flatMap { addon ->
            addon.manifest.catalogs
                .filterNot { it.requiresArgument }
                .mapNotNull { catalog ->
                    if (mediaTypeOf(catalog.type) == null) null else addon to catalog
                }
        }.take(limit)

        if (requests.isEmpty()) return Aggregated()

        // Fanned out over the catalogues themselves rather than over add-ons.
        // An add-on publishing four rows fetches all four at once; keying the
        // fan-out on the add-on would collapse them into one.
        return manager.fanOutOver(
            requests = requests,
            addonOf = { it.first },
            timeoutMillis = AddonManager.CATALOG_TIMEOUT_MILLIS,
        ) { (addon, catalog) -> fetchRow(addon, catalog) }
    }

    /** One page of a type's catalogue, for the browse grids. */
    suspend fun browse(type: MediaType, skip: Int, limit: Int): Aggregated<MediaItem> {
        val wireType = StremioResponseParser.wireTypeOf(type)
        val addons = manager.capableOf(AddonResourceName.CATALOG, wireType)
        if (addons.isEmpty()) return Aggregated()

        return manager.fanOut(addons, AddonManager.CATALOG_TIMEOUT_MILLIS) { addon ->
            val catalog = addon.manifest.catalogs
                .firstOrNull { it.type == wireType && !it.requiresArgument }
                ?: return@fanOut AddonReply.Answered(emptyList())

            // An add-on that does not advertise paging is asked only for its
            // first page; asking for a later one would silently return the
            // first again and duplicate the grid.
            if (skip > 0 && !catalog.supportsPaging) {
                return@fanOut AddonReply.Answered(emptyList())
            }

            val extra = if (skip > 0) listOf(AddonCatalog.EXTRA_SKIP to skip.toString()) else emptyList()
            when (val outcome = client.fetch(url(addon, catalog, extra), CachePolicy.CATALOG)) {
                is StremioProtocolClient.Outcome.Success ->
                    AddonReply.Answered(
                        StremioResponseParser.parseCatalog(outcome.body).take(limit),
                    )
                is StremioProtocolClient.Outcome.Failure -> AddonReply.Failed(outcome.reason)
            }
        }
    }

    /** Search, across every catalogue that advertises it. */
    suspend fun search(query: String, type: MediaType?): Aggregated<MediaItem> {
        val wireType = type?.let(StremioResponseParser::wireTypeOf)
        val addons = manager.capableOf(AddonResourceName.CATALOG, wireType)
        if (addons.isEmpty()) return Aggregated()

        return manager.fanOut(addons) { addon ->
            val catalog = addon.manifest.catalogs
                .firstOrNull { it.supportsSearch && (wireType == null || it.type == wireType) }
                ?: return@fanOut AddonReply.Answered(emptyList())

            val extra = listOf(AddonCatalog.EXTRA_SEARCH to query)
            when (val outcome = client.fetch(url(addon, catalog, extra), CachePolicy.CATALOG)) {
                is StremioProtocolClient.Outcome.Success ->
                    AddonReply.Answered(StremioResponseParser.parseCatalog(outcome.body))
                is StremioProtocolClient.Outcome.Failure -> AddonReply.Failed(outcome.reason)
            }
        }
    }

    private suspend fun fetchRow(
        addon: Addon,
        catalog: AddonCatalog,
    ): AddonReply<ResolvedRow> =
        when (val outcome = client.fetch(url(addon, catalog), CachePolicy.CATALOG)) {
            is StremioProtocolClient.Outcome.Success -> {
                val items = StremioResponseParser.parseCatalog(outcome.body)
                if (items.isEmpty()) {
                    // An empty catalogue is a successful answer, but not a row:
                    // a titled heading with nothing under it is worse than no
                    // heading at all.
                    AddonReply.Answered(emptyList())
                } else {
                    AddonReply.Answered(
                        listOf(
                            ResolvedRow(
                                row = ContentRow(
                                    id = "${addon.id}:${catalog.type}:${catalog.id}",
                                    title = rowTitle(addon, catalog),
                                    items = items,
                                ),
                                addon = addon,
                            ),
                        ),
                    )
                }
            }
            is StremioProtocolClient.Outcome.Failure -> AddonReply.Failed(outcome.reason)
        }

    /**
     * What to call the row.
     *
     * The catalogue's own name where it has one. Many add-ons leave it blank on
     * their only catalogue, in which case the add-on's name is what the viewer
     * would recognise anyway.
     */
    private fun rowTitle(addon: Addon, catalog: AddonCatalog): String =
        catalog.name?.takeIf { it.isNotBlank() } ?: addon.name

    private fun url(
        addon: Addon,
        catalog: AddonCatalog,
        extra: List<Pair<String, String>> = emptyList(),
    ): String = AddonUrls.resourceUrl(
        baseUrl = addon.baseUrl,
        resource = AddonResourceName.CATALOG.wireName,
        type = catalog.type,
        id = catalog.id,
        extra = extra,
    )

    private fun mediaTypeOf(wire: String): MediaType? = when (wire.lowercase()) {
        "movie" -> MediaType.MOVIE
        "series" -> MediaType.SERIES
        else -> null
    }

    private companion object {
        /**
         * A viewer scrolls perhaps six rows. Asking for many more would be
         * requests and artwork spent on a screen nobody reaches.
         */
        const val MAX_HOME_ROWS = 10
    }
}
