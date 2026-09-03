package tv.mango.app.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState
import tv.mango.app.databinding.FragmentBrowseBinding
import tv.mango.app.di.appGraph
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.ui.core.BrowseGridLayoutManager
import tv.mango.app.ui.core.CardActionSheet
import tv.mango.app.ui.core.CardSpacingDecoration
import tv.mango.app.ui.core.MediaCardAdapter

/**
 * A browsable grid of everything of one kind.
 *
 * The same card, the same focus behaviour and the same lane as the home rows -
 * moving from Home into Movies should feel like moving to a different view of
 * one surface, not to a different application.
 *
 * Pages are requested as the viewer approaches the end of what is loaded, so
 * the catalogue can be larger than memory without the viewer ever meeting a
 * loading state after the first screen.
 */
class BrowseFragment : Fragment() {

    private var binding: FragmentBrowseBinding? = null

    private val mediaType: MediaType
        get() = MediaType.valueOf(requireArguments().getString(ARG_TYPE)!!)

    private val viewModel: BrowseViewModel by viewModels {
        viewModelFactory {
            initializer { BrowseViewModel(appGraph.catalogRepository, mediaType) }
        }
    }

    private val cardAdapter = MediaCardAdapter(onSelected = ::openDetail, onLongSelected = ::onCardLongPressed)

    /** How far the grid has been scrolled up past the screen title, in pixels. */
    private var scrolledBy = 0

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            scrolledBy = (scrolledBy + dy).coerceAtLeast(0)
            fadeTitleForScroll()

            val manager = recyclerView.layoutManager as? BrowseGridLayoutManager ?: return
            val lastVisible = manager.findLastVisibleItemPosition()
            if (lastVisible >= manager.itemCount - PREFETCH_DISTANCE) {
                viewModel.loadNextPage()
            }
        }
    }

    /**
     * Cards scroll up over the screen title rather than being clipped at it -
     * clipping would also shave the lift and shadow off a focused card in the
     * top row. The title fades out instead, so the two never overlap legibly.
     */
    private fun fadeTitleForScroll() {
        val views = binding ?: return
        val fadeOver = resources.getDimensionPixelSize(R.dimen.space_10).toFloat()
        views.browseTitle.alpha = (1f - scrolledBy / fadeOver).coerceIn(0f, 1f)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentBrowseBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.browseTitle.setText(
            when (mediaType) {
                MediaType.MOVIE -> R.string.nav_movies
                MediaType.SERIES -> R.string.nav_series
            },
        )

        val safeHorizontal = resources.getDimensionPixelSize(R.dimen.safe_area_horizontal)
        val gap = resources.getDimensionPixelSize(R.dimen.card_gap)
        val cardWidth = resources.getDimensionPixelSize(R.dimen.card_poster_width)

        // The grid begins below the screen's title and scrolls up past it.
        val topInset = resources.getDimensionPixelSize(R.dimen.space_10)

        views.grid.apply {
            setPadding(safeHorizontal, topInset, safeHorizontal, safeHorizontal)
            layoutManager = BrowseGridLayoutManager(
                context = requireContext(),
                itemWidthPx = cardWidth + gap,
                laneOffset = resources.getDimensionPixelSize(R.dimen.safe_area_vertical),
            )
            addItemDecoration(CardSpacingDecoration.Grid(gap))
            setHasFixedSize(true)
            itemAnimator = null
            // The grid is a container, not a stop on the focus path: cards take
            // focus, the same as in a row.
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            adapter = cardAdapter
            addOnScrollListener(scrollListener)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: UiState<List<MediaItem>>) {
        val views = binding ?: return
        when (state) {
            is UiState.Loading -> {
                views.grid.visibility = View.GONE
                views.message.visibility = View.GONE
            }

            is UiState.Content -> {
                val firstLoad = views.grid.visibility != View.VISIBLE
                views.message.visibility = View.GONE
                views.grid.visibility = View.VISIBLE
                cardAdapter.submit(state.value)
                // Focus is taken on the first page only. Taking it again as
                // later pages arrive would drag the viewer back to the top
                // mid-scroll.
                if (firstLoad) views.grid.post { views.grid.requestFocus() }
            }

            is UiState.Empty -> showMessage(R.string.error_empty_title, null)

            is UiState.Error -> when (state.reason) {
                FailureReason.NETWORK ->
                    showMessage(R.string.error_network_title, R.string.error_network_body)
                else ->
                    showMessage(R.string.error_generic_title, R.string.error_generic_body)
            }
        }
    }

    private fun showMessage(titleRes: Int, bodyRes: Int?) {
        val views = binding ?: return
        views.grid.visibility = View.GONE
        views.message.visibility = View.VISIBLE
        views.message.setMessage(titleRes, bodyRes)
        views.message.setAction(R.string.action_retry) { viewModel.retry() }
        views.message.post { views.message.focusAction() }
    }

    private fun openDetail(item: MediaItem) {
        (activity as? NavigationHost)?.openDetail(item)
    }

    private fun onCardLongPressed(item: MediaItem, anchor: View): Boolean {
        val host = activity as? NavigationHost ?: return false
        CardActionSheet(
            context = requireContext(),
            item = item,
            anchor = anchor,
            library = appGraph.libraryRepository,
            scope = viewLifecycleOwner.lifecycleScope,
            onPlay = { host.requestPlayback(it) },
            onDetails = host::openDetail,
            onFindSimilar = host::findSimilar,
        ).show()
        return true
    }

    override fun onDestroyView() {
        binding?.let {
            it.grid.removeOnScrollListener(scrollListener)
            it.grid.adapter = null
        }
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TYPE = "media_type"

        /** How many cards from the end to start fetching the next page. */
        private const val PREFETCH_DISTANCE = 8

        fun forMovies(): BrowseFragment = of(MediaType.MOVIE)

        fun forSeries(): BrowseFragment = of(MediaType.SERIES)

        private fun of(type: MediaType): BrowseFragment = BrowseFragment().apply {
            arguments = Bundle(1).apply { putString(ARG_TYPE, type.name) }
        }
    }
}
