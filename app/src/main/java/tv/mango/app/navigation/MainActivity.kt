package tv.mango.app.navigation

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import tv.mango.app.R
import tv.mango.app.databinding.ActivityMainBinding
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.player.PendingPlayback
import tv.mango.app.ui.search.PendingSimilarSearch

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
     * Every route into playback shares one seam: the request is handed to
     * [PendingPlayback] and the stream picker reads it back, since a
     * [MediaItem] and an [Episode] are not something a [Route] can carry.
     */
    override fun requestPlayback(
        item: MediaItem,
        episode: Episode?,
        startFromBeginning: Boolean,
    ) {
        PendingPlayback.set(PendingPlayback.Request(item, episode, startFromBeginning))
        navigator.push(Route.StreamPicker)
    }

    override fun findSimilar(item: MediaItem) {
        PendingSimilarSearch.set(item)
        navigator.push(Route.SimilarTitles)
    }

    override fun focusNavigation() {
        binding.navRail.focusCurrentSection()
    }

    /**
     * Routes the remote's Menu key to the visible screen as a refresh.
     *
     * Content is fetched once per launch and kept, so refreshing is something
     * the viewer asks for rather than something that happens to them. Menu is
     * the conventional options key on a Fire TV remote and is otherwise unused.
     *
     * Handled at the activity because key events dispatch to the focused view
     * and then to the activity - they do not bubble back up through ancestor
     * key listeners, so a listener on a fragment's root would never see this.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            val visible = supportFragmentManager.findFragmentById(R.id.content_container)
            (visible as? RefreshableScreen)?.let {
                it.refresh()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
