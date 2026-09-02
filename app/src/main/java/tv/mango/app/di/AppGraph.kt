package tv.mango.app.di

import android.content.Context
import androidx.fragment.app.Fragment
import tv.mango.app.MangoApplication

/**
 * The application's object graph.
 *
 * Hand-written rather than generated. With a single module and a graph this
 * size, an annotation processor would cost every build round-trip more than it
 * saves in boilerplate. Everything is [lazy], so a dependency is constructed
 * only if some screen actually reaches for it - the database is not opened
 * because the home screen launched.
 *
 * Dependencies are exposed as interfaces, so swapping the mock catalogue for a
 * real provider is a change to this file alone.
 */
class AppGraph(private val application: Context) {

    val appContext: Context get() = application

    companion object {

        fun from(context: Context): AppGraph =
            (context.applicationContext as MangoApplication).graph
    }
}

/** Convenience accessor so fragments do not repeat the cast. */
val Fragment.appGraph: AppGraph
    get() = AppGraph.from(requireContext())
