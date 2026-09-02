package tv.mango.app.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.data.FailureReason
import tv.mango.app.databinding.FragmentSearchBinding
import tv.mango.app.databinding.ItemSearchChipBinding
import tv.mango.app.di.appGraph
import tv.mango.app.models.MediaItem
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.ui.core.BrowseGridLayoutManager
import tv.mango.app.ui.core.CardSpacingDecoration
import tv.mango.app.ui.core.MediaCardAdapter

/**
 * Search by title, across films and series.
 *
 * The results grid is the same [MediaCardAdapter] and [BrowseGridLayoutManager]
 * the browse screens use, so a result card focuses, lifts and opens exactly
 * like every other poster in the application - search is a different way of
 * arriving at the same content, not a different kind of screen.
 */
class SearchFragment : Fragment() {

    private var binding: FragmentSearchBinding? = null

    private val viewModel: SearchViewModel by viewModels {
        viewModelFactory {
            initializer { SearchViewModel(appGraph.catalogRepository) }
        }
    }

    private val resultsAdapter = MediaCardAdapter(onSelected = ::openDetail)

    /** Retried on request, since the failed state itself carries no query. */
    private var lastQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentSearchBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.searchResults.apply {
            val gap = resources.getDimensionPixelSize(R.dimen.card_gap)
            val cardWidth = resources.getDimensionPixelSize(R.dimen.card_poster_width)
            layoutManager = BrowseGridLayoutManager(
                context = requireContext(),
                itemWidthPx = cardWidth + gap,
                laneOffset = 0,
            )
            addItemDecoration(CardSpacingDecoration.Grid(gap))
            setHasFixedSize(true)
            itemAnimator = null
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            adapter = resultsAdapter
        }

        views.searchField.setOnEditorActionListener { textView, actionId, event ->
            val committed = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (committed) submit(textView.text?.toString().orEmpty())
            committed
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.recentSearches.collect(::renderRecentSearches) }
            }
        }

        views.searchField.post { views.searchField.requestFocus() }
    }

    private fun submit(query: String) {
        if (query.isBlank()) return
        lastQuery = query.trim()
        viewModel.search(lastQuery)
    }

    private fun render(state: SearchState) {
        val views = binding ?: return

        views.searchProgress.visibility = View.GONE
        views.searchResults.visibility = View.GONE
        views.searchMessage.visibility = View.GONE

        when (state) {
            SearchState.Idle -> showMessage(R.string.search_prompt_title, R.string.search_prompt_body)

            SearchState.Loading -> views.searchProgress.visibility = View.VISIBLE

            is SearchState.Content -> {
                resultsAdapter.submit(state.results)
                views.searchResults.visibility = View.VISIBLE
            }

            is SearchState.Empty -> showMessage(
                getString(R.string.search_empty_title, state.query),
                R.string.search_empty_body,
            )

            is SearchState.Error -> when (state.reason) {
                FailureReason.NETWORK -> showMessage(
                    R.string.error_network_title,
                    R.string.error_network_body,
                    showRetry = true,
                )
                else -> showMessage(
                    R.string.error_generic_title,
                    R.string.error_generic_body,
                    showRetry = true,
                )
            }
        }
    }

    private fun showMessage(titleRes: Int, bodyRes: Int?, showRetry: Boolean = false) {
        val views = binding ?: return
        views.searchMessage.visibility = View.VISIBLE
        views.searchMessage.setMessage(titleRes, bodyRes)
        setRetry(showRetry)
    }

    private fun showMessage(title: CharSequence, bodyRes: Int?, showRetry: Boolean = false) {
        val views = binding ?: return
        views.searchMessage.visibility = View.VISIBLE
        views.searchMessage.setMessage(title, bodyRes)
        setRetry(showRetry)
    }

    private fun setRetry(showRetry: Boolean) {
        val views = binding ?: return
        if (showRetry && lastQuery.isNotEmpty()) {
            views.searchMessage.setAction(R.string.action_retry) { viewModel.search(lastQuery) }
        } else {
            views.searchMessage.setAction(null)
        }
    }

    private fun renderRecentSearches(recent: List<String>) {
        val views = binding ?: return
        views.searchRecentGroup.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        views.searchRecentChips.removeAllViews()
        recent.forEach { query ->
            val chip = ItemSearchChipBinding.inflate(
                layoutInflater,
                views.searchRecentChips,
                false,
            )
            chip.root.text = query
            chip.root.setOnClickListener {
                views.searchField.setText(query)
                views.searchField.setSelection(query.length)
                submit(query)
            }
            views.searchRecentChips.addView(chip.root)
        }
    }

    private fun openDetail(item: MediaItem) {
        (activity as? NavigationHost)?.openDetail(item)
    }

    override fun onDestroyView() {
        binding?.searchResults?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
