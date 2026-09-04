package tv.mango.app

import android.app.Application
import android.os.StrictMode
import tv.mango.app.di.AppGraph
import tv.mango.app.theme.RuntimeTheme

/**
 * Application entry point.
 *
 * Deliberately does almost nothing. Cold start on an entry-level Fire Stick is
 * dominated by work done here, so the object graph is built lazily and no I/O,
 * database open or image-loader initialisation happens on this path.
 */
class MangoApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        graph = AppGraph(this)
        // Observes the saved appearance from here on, so the first screen
        // built already reflects it rather than the built-in defaults.
        RuntimeTheme.start(graph.homeScreenConfigRepository)
    }

    /**
     * Debug-only. StrictMode's penalties are cheap but its detection is not, so
     * it never ships in a release build.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build(),
        )
    }
}
