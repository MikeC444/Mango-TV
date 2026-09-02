package tv.mango.app.navigation

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import tv.mango.app.databinding.ActivityMainBinding
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.utilities.Logger

/**
 * The application's only activity.
 *
 * Screens are fragments swapped inside one container. A single activity avoids
 * the window-creation cost of an activity transition on every navigation -
 * measurable on this class of hardware - and keeps Back behaviour in one place
 * rather than spread across manifest task affinities.
 */
class MainActivity : AppCompatActivity(), NavigationHost {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navigator = Navigator(supportFragmentManager).apply {
            onSectionChanged = binding.navRail::setCurrentSection
        }

        binding.navRail.onSectionSelected = navigator::goToSection

        // Back can unwind the fragment stack; when there is nothing left, the
        // press leaves the application, which is what a viewer expects from the
        // home screen of a television app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigator.pop()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Keeps the rail's marker honest when Back unwinds a section rather
        // than the rail being used to leave it.
        supportFragmentManager.addOnBackStackChangedListener {
            navigator.syncSectionFromBackStack()
        }

        navigator.start()
    }

    /**
     * Playback is not built yet.
     *
     * Left as an explicit, logged no-op rather than as buttons wired to
     * nothing: the seam is where the player attaches, and a gap that is named
     * is easier to close than one spread across the screens that need it.
     */
    override fun requestPlayback(
        item: MediaItem,
        episode: Episode?,
        startFromBeginning: Boolean,
    ) {
        Logger.d(
            "Playback requested for ${item.title}" +
                (episode?.let { ", episode ${it.number}" } ?: "") +
                if (startFromBeginning) ", from the beginning" else "",
        )
    }

    override fun focusNavigation() {
        binding.navRail.focusCurrentSection()
    }

    override fun openDetail(item: MediaItem) {
        val route = when (item.type) {
            MediaType.MOVIE -> Route.MovieDetail(item.id.value)
            MediaType.SERIES -> Route.SeriesDetail(item.id.value)
        }
        navigator.push(route)
    }

    override fun push(route: Route) {
        navigator.push(route)
    }
}
