package tv.mango.app.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
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
import tv.mango.app.ui.core.CardActionSheet
import tv.mango.app.ui.core.CardSpacingDecoration
import tv.mango.app.ui.core.CardTooltipController
import tv.mango.app.ui.core.MediaCardAdapter

/**
 * Search by title, across films and series.
 *
 * Results update live as the viewer types - [SearchViewModel] debounces the
 * actual query, so this screen never has to think about that itself, only
 * report every keystroke and render whatever state comes back. The results
 * grid is the same [MediaCardAdapter] and [BrowseGridLayoutManager] the
 * browse screens use, so a result card focuses, lifts and opens exactly like
 * every other poster in the application - search is a different way of
 * arriving at the same content, not a different kind of screen.
 */
class SearchFragment : Fragment() {

    private var binding: FragmentSearchBinding? = null

    private val viewModel: SearchViewModel by viewModels {
        viewModelFactory {
            initializer { SearchViewModel(appGraph.catalogRepository) }
        }
    }

    private val resultsAdapter = MediaCardAdapter(onSelected = ::openDetail, onLongSelected = ::onCardLongPressed)

    /** Retried on request, since the failed state itself carries no query. */
    private var lastQuery: String = ""

    private var tooltipController: CardTooltipController? = null

    private val queryWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString().orEmpty()
            // Kept in sync with every keystroke, not just an explicit submit,
            // so a retry after an error reached by live typing re-runs the
            // query actually on screen rather than a stale one.
            lastQuery = text.trim()
            viewModel.onQueryChanged(text)
        }
    }

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

        tooltipController = CardTooltipController(views.cardTooltip, view).apply { attach() }

        views.searchField.addTextChangedListener(queryWatcher)
        views.searchField.setOnEditorActionListener { textView, actionId, event ->
            val committed = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (committed) submit(textView.text?.toString().orEmpty())
            committed
        }

        // Fixed, never empty - bound once rather than on every state change.
        bindChips(views.searchPopularChips, viewModel.popularSearches)

        // Reached from a card's long-press menu: the field shows what the
        // search was seeded from, the same way picking a chip would, but the
        // results themselves come from a genre match rather than the title
        // this text would otherwise search for - detached and reattached so
        // setting it here does not also fire an ordinary typed search.
        PendingSimilarSearch.take()?.let { item ->
            views.searchField.removeTextChangedListener(queryWatcher)
            views.searchField.setText(item.title)
            views.searchField.setSelection(item.title.length)
            views.searchField.addTextChangedListener(queryWatcher)
            lastQuery = item.title
            viewModel.findSimilar(item)
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
        viewModel.submit(lastQuery)
    }

    private fun render(state: SearchState) {
        val views = binding ?: return

        views.searchProgress.visibility = View.GONE
        views.searchResults.visibility = View.GONE
        views.searchMessage.visibility = View.GONE
        views.searchSuggestions.visibility = View.GONE

        when (state) {
            SearchState.Idle -> views.searchSuggestions.visibility = View.VISIBLE

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
            views.searchMessage.setAction(R.string.action_retry) { viewModel.submit(lastQuery) }
        } else {
            views.searchMessage.setAction(null)
        }
    }

    private fun renderRecentSearches(recent: List<String>) {
        val views = binding ?: return
        views.searchRecentGroup.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        bindChips(views.searchRecentChips, recent)
    }

    private fun bindChips(container: LinearLayout, labels: List<String>) {
        container.removeAllViews()
        labels.forEach { label ->
            val chip = ItemSearchChipBinding.inflate(layoutInflater, container, false)
            chip.root.text = label
            chip.root.setOnClickListener { selectChip(label) }
            container.addView(chip.root)
        }
    }

    private fun selectChip(label: String) {
        val views = binding ?: return
        views.searchField.removeTextChangedListener(queryWatcher)
        views.searchField.setText(label)
        views.searchField.setSelection(label.length)
        views.searchField.addTextChangedListener(queryWatcher)
        submit(label)
    }

    private fun openDetail(item: MediaItem) {
        (activity as? NavigationHost)?.openDetail(item)
    }

    private fun onCardLongPressed(item: MediaItem): Boolean {
        val host = activity as? NavigationHost ?: return false
        CardActionSheet(
            context = requireContext(),
            item = item,
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
            it.searchField.removeTextChangedListener(queryWatcher)
            it.searchResults.adapter = null
        }
        tooltipController?.detach()
        tooltipController = null
        binding = null
        super.onDestroyView()
    }
}
