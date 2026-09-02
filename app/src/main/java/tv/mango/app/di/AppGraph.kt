package tv.mango.app.di

import android.content.Context
import androidx.fragment.app.Fragment
import tv.mango.app.MangoApplication
import tv.mango.app.data.mock.MockCatalogProvider
import tv.mango.app.data.provider.CatalogProvider
import tv.mango.app.repository.CatalogRepository

/**
 * The application's object graph.
 *
 * Hand-written rather than generated. With a single module and a graph this
 * size, an annotation processor would cost every build more than it saves in
 * boilerplate, and this file is short enough to read in one sitting - which is
 * the actual argument for dependency injection.
 *
 * Everything is [lazy], so a dependency is constructed only if some screen
 * reaches for it: the database is not opened because the home screen launched.
 *
 * Dependencies are held as interfaces. Replacing bundled mock content with a
 * real catalogue is a change to the one line below and nothing else.
 */
class AppGraph(private val application: Context) {

    val appContext: Context get() = application

    private val catalogProvider: CatalogProvider by lazy { MockCatalogProvider(application) }

    val catalogRepository: CatalogRepository by lazy { CatalogRepository(catalogProvider) }

    companion object {
        fun from(context: Context): AppGraph =
            (context.applicationContext as MangoApplication).graph
    }
}

/** Saves every fragment repeating the cast to reach the graph. */
val Fragment.appGraph: AppGraph
    get() = AppGraph.from(requireContext())
