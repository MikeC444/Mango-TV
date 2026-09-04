package tv.mango.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import tv.mango.app.R
import tv.mango.app.di.appGraph
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaItem
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.navigation.RefreshableScreen
import tv.mango.app.ui.core.CardActionSheet
import tv.mango.app.ui.home.compose.HomeScreen
import tv.mango.app.ui.home.compose.rememberHomeColors

/**
 * The home screen, rebuilt on Compose: a NuvioTV-style hero over catalog
 * rows, drawn by [tv.mango.app.ui.home.compose.HomeScreen].
 *
 * [HomeViewModel] is unchanged from the View-based screen this replaces -
 * only the rendering layer moved. Everything the view model already does
 * (Continue Watching merged in live, watched badges, Settings -> Home
 * Screen -> Catalog Rows' visibility/order/renaming) keeps working exactly
 * as before, because this fragment never touches that logic.
 *
 * Kept as the class named `HomeFragment`, in this package, so
 * [tv.mango.app.navigation.Navigator] and
 * [tv.mango.app.ui.settings.home.PreviewHomeScreenFragment] - which hosts
 * this fragment as a live preview - need no changes.
 *
 * Deliberately out of scope for this pass: Settings -> Home Screen's
 * fine-grained hero/card personalisation (glass level, hero rotation
 * interval, per-row layout style, background mode). Those settings screens
 * and their stored configuration are untouched; only their effect on Home's
 * own drawing is not yet wired into the Compose surface, matching a fixed
 * NuvioTV-style look. Row visibility/order/renaming, the accent colour and
 * whether the hero is present at all still apply, since those are resolved
 * before [HomeViewModel.state] is emitted.
 */
class HomeFragment : Fragment(), RefreshableScreen {

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                HomeViewModel(
                    appGraph.catalogRepository,
                    appGraph.libraryRepository,
                    appGraph.homeScreenConfigRepository,
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = ComposeView(requireContext())
        view.setContent {
            val state by viewModel.state.collectAsState()
            val colors = rememberHomeColors()
            HomeScreen(
                state = state,
                colors = colors,
                onCardClick = ::onCardSelected,
                onCardLongClick = { onCardLongPressed(it, view) },
                onPlay = ::playItem,
                onDetails = ::openDetail,
                onRetry = viewModel::retry,
            )
        }
        return view
    }

    override fun refresh() {
        viewModel.refresh()
    }

    private fun openDetail(item: MediaItem) {
        (activity as? NavigationHost)?.openDetail(item)
    }

    /**
     * A Continue Watching card resumes directly rather than opening the
     * detail screen first - "click it and it plays" is the entire point of
     * the row. Every other card behaves as it always has.
     */
    private fun onCardSelected(item: MediaItem) {
        if (item.resume == null) openDetail(item) else playItem(item)
    }

    /** Resumes exactly where a Continue Watching card's snapshot says it left off. */
    private fun playItem(item: MediaItem) {
        val resume = item.resume
        val episode = resume?.episodeId?.let { episodeId ->
            Episode(
                id = episodeId,
                seriesId = item.id,
                season = resume.episodeSeason ?: 1,
                number = resume.episodeNumber ?: 1,
                title = resume.episodeTitle ?: item.title,
                thumbnail = item.images.poster,
            )
        }
        (activity as? NavigationHost)?.requestPlayback(item, episode, startFromBeginning = false)
    }

    private fun playFromSheet(item: MediaItem) {
        if (item.resume != null) playItem(item) else (activity as? NavigationHost)?.requestPlayback(item)
    }

    /**
     * The long-press quick-action menu. [anchor] is the Compose surface
     * itself rather than the individual card the old View-based row could
     * hand over: a Compose node has no `View` of its own for
     * [CardActionSheet]'s grow-from-the-poster reveal to measure, so the
     * panel fades in centred on Home instead of growing out of the exact
     * card. Every action on it still works the same.
     */
    private fun onCardLongPressed(item: MediaItem, anchor: View) {
        val host = activity as? NavigationHost ?: return
        CardActionSheet(
            context = requireContext(),
            item = item,
            anchor = anchor,
            library = appGraph.libraryRepository,
            scope = viewLifecycleOwner.lifecycleScope,
            onPlay = ::playFromSheet,
            onDetails = host::openDetail,
            onFindSimilar = host::findSimilar,
            onRemoveFromContinueWatching = item.resume?.let { resume ->
                {
                    viewModel.removeFromContinueWatching(resume.id)
                    Toast.makeText(requireContext(), R.string.continue_watching_removed, Toast.LENGTH_SHORT).show()
                }
            },
        ).show()
    }
}
