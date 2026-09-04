package tv.mango.app.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import tv.mango.app.R
import tv.mango.app.models.MediaType
import tv.mango.app.ui.addon.AddAddonFragment
import tv.mango.app.ui.addon.AddonDetailFragment
import tv.mango.app.ui.addon.AddonListFragment
import tv.mango.app.ui.browse.BrowseFragment
import tv.mango.app.ui.common.PlaceholderFragment
import tv.mango.app.ui.detail.DetailFragment
import tv.mango.app.ui.home.HomeFragment
import tv.mango.app.ui.player.StreamPickerFragment
import tv.mango.app.ui.search.SearchFragment
import tv.mango.app.ui.settings.SettingsFragment
import tv.mango.app.ui.settings.home.CatalogRowsFragment
import tv.mango.app.ui.settings.home.HomeScreenMenuFragment
import tv.mango.app.ui.settings.home.HomeScreenOptionsFragment
import tv.mango.app.ui.settings.home.PresetsFragment
import tv.mango.app.ui.settings.home.PreviewHomeScreenFragment

/**
 * Screen changes, in one place.
 *
 * Back has to be predictable everywhere, which here means one rule the viewer
 * can learn in a single press:
 *
 *  - Home is the root and is never on the back stack.
 *  - Choosing another section replaces whatever section is open, so Back from
 *    any section returns to Home rather than retracing every section visited.
 *  - Details stack on top of wherever they were opened from, so Back returns
 *    to that row, in that section, with focus where it was left.
 *  - Back from Home leaves the application.
 *
 * Transitions are a cross-fade only. A television sits still until something
 * changes; sliding screens around would add motion that carries no meaning and
 * costs a frame budget the hardware does not have.
 */
class Navigator(
    private val fragmentManager: FragmentManager,
    private val containerId: Int = R.id.content_container,
) {

    /** Notified whenever the visible section changes, so the rail can follow. */
    var onSectionChanged: ((Route.Section) -> Unit)? = null

    var currentSection: Route.Section = Route.Home
        private set

    /** Places Home as the permanent root. Called once, on a cold start. */
    fun start() {
        if (fragmentManager.findFragmentById(containerId) != null) return
        fragmentManager.commit {
            setReorderingAllowed(true)
            replace(containerId, HomeFragment(), Route.Home.tag())
        }
        notifySection(Route.Home)
    }

    /** Moves to a rail destination, discarding any section already open. */
    fun goToSection(section: Route.Section) {
        if (section == currentSection && fragmentManager.backStackEntryCount == 0) return

        unwindToHome()

        if (section != Route.Home) {
            fragmentManager.commit {
                setReorderingAllowed(true)
                setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                replace(containerId, fragmentFor(section), section.tag())
                addToBackStack(null)
            }
        }
        notifySection(section)
    }

    /** Opens a screen on top of the current one. */
    fun push(route: Route) {
        fragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            replace(containerId, fragmentFor(route), route.tag())
            addToBackStack(null)
        }
    }

    /**
     * Clears the stack back to Home.
     *
     * Popping by the root entry's id, inclusive, unwinds everything in one
     * transaction. Naming the entry instead would only find the section, and
     * would strand a detail screen that had been opened on top of it - so
     * choosing a new section while looking at a title would leave that title
     * underneath, and Back would return to it.
     */
    private fun unwindToHome() {
        if (fragmentManager.backStackEntryCount == 0) return
        val rootEntryId = fragmentManager.getBackStackEntryAt(0).id
        fragmentManager.popBackStackImmediate(
            rootEntryId,
            FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
    }

    /**
     * Handles a Back press.
     *
     * @return false when there is nothing left to unwind, meaning the caller
     *   should let the press leave the application.
     */
    fun pop(): Boolean {
        if (fragmentManager.backStackEntryCount == 0) return false
        fragmentManager.popBackStack()
        return true
    }

    /** Re-derives the visible section after the back stack unwinds. */
    fun syncSectionFromBackStack() {
        val section = if (fragmentManager.backStackEntryCount == 0) {
            Route.Home
        } else {
            currentSection
        }
        notifySection(section)
    }

    private fun notifySection(section: Route.Section) {
        currentSection = section
        onSectionChanged?.invoke(section)
    }

    private fun fragmentFor(route: Route): Fragment = when (route) {
        Route.Home -> HomeFragment()
        Route.Movies -> BrowseFragment.forMovies()
        Route.Series -> BrowseFragment.forSeries()
        Route.Search -> SearchFragment()
        // Library arrives in a later phase. Until then it is an honest empty
        // state rather than a dead entry on the rail.
        Route.Library -> PlaceholderFragment.of(R.string.nav_library)
        Route.Settings -> SettingsFragment()
        Route.HomeScreenMenu -> HomeScreenMenuFragment()
        is Route.HomeScreenSection -> HomeScreenOptionsFragment.forSection(route.section)
        Route.CatalogRows -> CatalogRowsFragment()
        is Route.EditRow -> HomeScreenOptionsFragment.forRow(route.rowId, route.rowTitle)
        Route.Presets -> PresetsFragment()
        Route.PreviewHomeScreen -> PreviewHomeScreenFragment()
        is Route.MovieDetail -> DetailFragment.of(route.id, MediaType.MOVIE)
        is Route.SeriesDetail -> DetailFragment.of(route.id, MediaType.SERIES)
        Route.AddonList -> AddonListFragment()
        Route.AddAddon -> AddAddonFragment()
        is Route.AddonDetail -> AddonDetailFragment.of(route.addonId)
        Route.StreamPicker -> StreamPickerFragment()
        Route.SimilarTitles -> SearchFragment()
    }

    private fun Route.tag(): String = this::class.java.name
}
