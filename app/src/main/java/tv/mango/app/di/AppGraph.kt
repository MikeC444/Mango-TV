package tv.mango.app.di

import android.content.Context
import androidx.fragment.app.Fragment
import okhttp3.OkHttpClient
import tv.mango.app.MangoApplication
import tv.mango.app.addon.AddonInstaller
import tv.mango.app.addon.AddonManager
import tv.mango.app.addon.AddonRepository
import tv.mango.app.addon.CatalogResolver
import tv.mango.app.addon.MetadataResolver
import tv.mango.app.addon.StreamResolver
import tv.mango.app.addon.SubtitleResolver
import tv.mango.app.addon.protocol.StremioProtocolClient
import tv.mango.app.data.local.HomeScreenConfigStore
import tv.mango.app.data.local.LibraryStore
import tv.mango.app.data.mock.MockCatalogProvider
import tv.mango.app.data.mock.MockCatalogSource
import tv.mango.app.data.mock.MockDetailProvider
import tv.mango.app.data.provider.AddonCatalogProvider
import tv.mango.app.data.provider.AddonDetailProvider
import tv.mango.app.data.provider.AddonStreamProvider
import tv.mango.app.data.provider.AddonSubtitleProvider
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.data.provider.CompositeCatalogProvider
import tv.mango.app.data.provider.FallbackMovieProvider
import tv.mango.app.data.provider.FallbackSeriesProvider
import tv.mango.app.data.provider.MovieProvider
import tv.mango.app.data.provider.SeriesProvider
import tv.mango.app.data.provider.StreamProvider
import tv.mango.app.data.provider.SubtitleProvider
import tv.mango.app.network.HttpClientFactory
import tv.mango.app.repository.CatalogRepository
import tv.mango.app.repository.HomeScreenConfigRepository
import tv.mango.app.repository.LibraryRepository

/**
 * The application's object graph.
 *
 * Hand-written rather than generated. With a single module and a graph this
 * size, an annotation processor would cost every build more than it saves in
 * boilerplate, and this file is short enough to read in one sitting - which is
 * the actual argument for dependency injection.
 *
 * Everything is [lazy], so a dependency is constructed only if some screen
 * reaches for it. In particular the HTTP client, and everything add-on related
 * behind it, is not built because the application launched: a viewer with no
 * add-ons installed never pays for any of it.
 *
 * This file is also the whole of the wiring between the add-on ecosystem and
 * the rest of the application. Add-ons are assembled here into something that
 * implements the same content interfaces the bundled catalogue does, and handed
 * to the same repository. Nothing above this file changed to support them.
 */
class AppGraph(private val application: Context) {

    val appContext: Context get() = application

    // --------------------------------------------------------------- bundled

    /** Shared so the bundled assets are parsed once between both providers. */
    private val mockSource: MockCatalogSource by lazy { MockCatalogSource(application) }

    private val bundledCatalogProvider: CatalogProvider by lazy {
        MockCatalogProvider(mockSource)
    }

    private val bundledDetailProvider: MockDetailProvider by lazy {
        MockDetailProvider(mockSource)
    }

    // --------------------------------------------------------------- add-ons

    private val httpClient: OkHttpClient by lazy { HttpClientFactory.create() }

    private val protocolClient: StremioProtocolClient by lazy {
        StremioProtocolClient(httpClient)
    }

    val addonRepository: AddonRepository by lazy { AddonRepository(application) }

    val addonManager: AddonManager by lazy {
        AddonManager(addonRepository, protocolClient)
    }

    val addonInstaller: AddonInstaller by lazy {
        AddonInstaller(protocolClient, addonRepository)
    }

    private val catalogResolver: CatalogResolver by lazy {
        CatalogResolver(addonManager, protocolClient)
    }

    private val metadataResolver: MetadataResolver by lazy {
        MetadataResolver(addonManager, protocolClient)
    }

    private val addonCatalogProvider: CatalogProvider by lazy {
        AddonCatalogProvider(catalogResolver, metadataResolver)
    }

    private val addonDetailProvider: AddonDetailProvider by lazy {
        AddonDetailProvider(metadataResolver)
    }

    private val streamResolver: StreamResolver by lazy {
        StreamResolver(addonManager, protocolClient)
    }

    private val subtitleResolver: SubtitleResolver by lazy {
        SubtitleResolver(addonManager, protocolClient)
    }

    /**
     * Add-ons are the only source of streams and subtitles: the bundled
     * catalogue is mock data with nothing playable behind it, so there is no
     * fallback to compose here the way there is for catalogues and metadata.
     */
    val streamProvider: StreamProvider by lazy { AddonStreamProvider(streamResolver) }

    val subtitleProvider: SubtitleProvider by lazy { AddonSubtitleProvider(subtitleResolver) }

    // ------------------------------------------------------------- composed

    /**
     * Add-ons where the viewer has any, the bundled catalogue where they do
     * not. The screens above see one content source either way.
     */
    private val catalogProvider: CatalogProvider by lazy {
        CompositeCatalogProvider(
            addons = addonCatalogProvider,
            bundled = bundledCatalogProvider,
            hasEnabledAddons = { addonRepository.enabled().isNotEmpty() },
        )
    }

    private val movieProvider: MovieProvider by lazy {
        FallbackMovieProvider(addonDetailProvider, bundledDetailProvider)
    }

    private val seriesProvider: SeriesProvider by lazy {
        FallbackSeriesProvider(addonDetailProvider, bundledDetailProvider)
    }

    val catalogRepository: CatalogRepository by lazy {
        CatalogRepository(catalogProvider, movieProvider, seriesProvider)
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(LibraryStore(application))
    }

    /**
     * Eager rather than `lazy`: [tv.mango.app.MangoApplication] starts
     * [tv.mango.app.theme.RuntimeTheme] observing this the moment the process
     * comes up, so the very first screen already reflects a viewer's saved
     * appearance rather than flashing the built-in defaults first. The
     * constructor itself does no I/O - only collecting [HomeScreenConfigRepository.config]
     * touches DataStore, and that happens on a background dispatcher.
     */
    val homeScreenConfigRepository: HomeScreenConfigRepository =
        HomeScreenConfigRepository(HomeScreenConfigStore(application))

    companion object {
        fun from(context: Context): AppGraph =
            (context.applicationContext as MangoApplication).graph
    }
}

/** Saves every fragment repeating the cast to reach the graph. */
val Fragment.appGraph: AppGraph
    get() = AppGraph.from(requireContext())
