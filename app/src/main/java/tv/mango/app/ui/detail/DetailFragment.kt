package tv.mango.app.ui.detail

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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState
import tv.mango.app.databinding.FragmentDetailBinding
import tv.mango.app.di.appGraph
import tv.mango.app.models.Episode
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.models.TitleDetail
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.utilities.Formatters

/**
 * The detail screen, for a film or a series.
 *
 * One fragment rather than two. The shell is identical - backdrop, poster,
 * title, metadata, actions, synopsis, cast - and the only difference is that a
 * series adds a seasons section. Splitting it would duplicate all of the shared
 * part to avoid one conditional; instead the seasons section is its own view
 * that stays gone for a film.
 *
 * The screen leads with imagery and puts the words in a fixed order beneath it,
 * so it reads as being about a title rather than as a record of one.
 */
class DetailFragment : Fragment() {

    private var binding: FragmentDetailBinding? = null

    private val mediaId: MediaId
        get() = MediaId(requireArguments().getString(ARG_ID)!!)

    private val mediaType: MediaType
        get() = MediaType.valueOf(requireArguments().getString(ARG_TYPE)!!)

    private val viewModel: DetailViewModel by viewModels {
        viewModelFactory {
            initializer {
                DetailViewModel(
                    catalog = appGraph.catalogRepository,
                    library = appGraph.libraryRepository,
                    id = mediaId,
                    type = mediaType,
                )
            }
        }
    }

    private val castAdapter = CastAdapter()

    /** The title on screen, for the playback controls to act on. */
    private var shown: MediaItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentDetailBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.castList.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = castAdapter
            itemAnimator = null
            // The cast members take focus; the list around them does not.
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        views.detailSeasons.onSeasonSelected = viewModel::selectSeason
        views.detailSeasons.onEpisodeSelected = { episode -> play(episode = episode) }

        views.actionPlay.setOnClickListener { play() }
        views.actionRestart.setOnClickListener { play(startFromBeginning = true) }
        views.actionTrailer.setOnClickListener { play() }
        views.actionLibrary.setOnClickListener { viewModel.toggleLibrary() }

        collectState()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.detail.collect { render(it) } }
                launch {
                    viewModel.episodes.collect { binding?.detailSeasons?.showEpisodes(it) }
                }
                launch {
                    viewModel.selectedSeason.collect { binding?.detailSeasons?.setSelectedSeason(it) }
                }
                launch { viewModel.inLibrary.collect(::renderLibraryState) }
            }
        }
    }

    private fun render(state: UiState<TitleDetail>) {
        val views = binding ?: return
        when (state) {
            is UiState.Loading -> {
                views.detailScroll.visibility = View.GONE
                views.message.visibility = View.GONE
            }

            is UiState.Content -> showDetail(state.value)

            is UiState.Empty -> showMessage(R.string.error_empty_title, null)

            is UiState.Error -> when (state.reason) {
                FailureReason.NETWORK ->
                    showMessage(R.string.error_network_title, R.string.error_network_body)
                else ->
                    showMessage(R.string.error_generic_title, R.string.error_generic_body)
            }
        }
    }

    private fun showDetail(detail: TitleDetail) {
        val views = binding ?: return
        val item = detail.item
        shown = item

        views.message.visibility = View.GONE
        views.detailScroll.visibility = View.VISIBLE

        views.detailTitle.text = item.title
        views.detailMeta.text = Formatters.metadataLine(requireContext(), item)
        views.detailSynopsis.text = item.synopsis

        ImageLoader.loadBackdrop(
            target = views.detailBackdrop,
            key = item.images.backdrop,
            widthPx = BACKDROP_MAX_WIDTH_PX,
            heightPx = BACKDROP_MAX_HEIGHT_PX,
        )
        ImageLoader.loadPoster(
            target = views.detailPoster,
            key = item.images.poster,
            widthPx = resources.getDimensionPixelSize(R.dimen.detail_poster_width),
            heightPx = resources.getDimensionPixelSize(R.dimen.detail_poster_height),
        )

        // A partly watched title offers to resume, and to start again. An
        // unwatched one shows only Play, rather than a Start Over that would
        // do the same thing.
        views.actionPlay.setText(
            if (item.isPartiallyWatched) R.string.action_continue else R.string.action_play,
        )
        views.actionRestart.visibility =
            if (item.isPartiallyWatched) View.VISIBLE else View.GONE

        val hasCast = detail.cast.isNotEmpty()
        views.castHeading.visibility = if (hasCast) View.VISIBLE else View.GONE
        views.castList.visibility = if (hasCast) View.VISIBLE else View.GONE
        if (hasCast) castAdapter.submit(detail.cast)

        val hasSeasons = detail.seasons.isNotEmpty()
        views.detailSeasons.visibility = if (hasSeasons) View.VISIBLE else View.GONE
        if (hasSeasons) {
            views.detailSeasons.showSeasons(detail.seasons, viewModel.selectedSeason.value)
        }

        // Focus lands on the primary action, so the first press of the remote
        // does the obvious thing.
        views.actionPlay.post { views.actionPlay.requestFocus() }
    }

    /**
     * The library button reports its own state rather than acting as a toggle
     * the viewer has to remember pressing: the label says what is true now, and
     * a content description carries the same fact for a screen reader.
     */
    private fun renderLibraryState(saved: Boolean) {
        val views = binding ?: return
        views.actionLibrary.setText(
            if (saved) R.string.action_remove_from_library else R.string.action_add_to_library,
        )
        views.actionLibrary.contentDescription =
            if (saved) getString(R.string.cd_in_library) else null
    }

    /** Every playback control on this screen goes through the host's one seam. */
    private fun play(episode: Episode? = null, startFromBeginning: Boolean = false) {
        val item = shown ?: return
        (activity as? NavigationHost)?.requestPlayback(item, episode, startFromBeginning)
    }

    private fun showMessage(titleRes: Int, bodyRes: Int?) {
        val views = binding ?: return
        views.detailScroll.visibility = View.GONE
        views.message.visibility = View.VISIBLE
        views.message.setMessage(titleRes, bodyRes)
        views.message.setAction(R.string.action_retry) { viewModel.load() }
        views.message.post { views.message.focusAction() }
    }

    override fun onDestroyView() {
        binding?.let {
            ImageLoader.clear(it.detailBackdrop)
            ImageLoader.clear(it.detailPoster)
            it.castList.adapter = null
            it.detailSeasons.release()
        }
        shown = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "media_id"
        private const val ARG_TYPE = "media_type"

        /** As on the home hero: capped well below any television's resolution. */
        private const val BACKDROP_MAX_WIDTH_PX = 1280
        private const val BACKDROP_MAX_HEIGHT_PX = 720

        fun of(id: String, type: MediaType): DetailFragment = DetailFragment().apply {
            arguments = Bundle(2).apply {
                putString(ARG_ID, id)
                putString(ARG_TYPE, type.name)
            }
        }
    }
}
