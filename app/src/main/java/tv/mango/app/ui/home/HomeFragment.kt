package tv.mango.app.ui.home

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
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.data.FailureReason
import tv.mango.app.data.UiState
import tv.mango.app.databinding.FragmentHomeBinding
import tv.mango.app.di.appGraph
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaItem
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.ui.core.ContentRowsAdapter

/**
 * The home screen: rows of content the viewer moves through with the D-pad.
 *
 * The cinematic hero arrives with real artwork in the next phase; the rows and
 * the focus behaviour beneath it are complete.
 */
class HomeFragment : Fragment() {

    private var binding: FragmentHomeBinding? = null

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer { HomeViewModel(appGraph.catalogRepository) }
        }
    }

    private val rowsAdapter = ContentRowsAdapter(
        onItemSelected = ::openDetail,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentHomeBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.rows?.adapter = rowsAdapter

        // Collection is tied to STARTED, so a screen that is not visible is not
        // holding a request open or updating views nobody can see.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: UiState<List<ContentRow>>) {
        val views = binding ?: return
        when (state) {
            is UiState.Loading -> {
                // Deliberately blank. A spinner here would be a continuous
                // animation on a screen that is about to fill in a moment; the
                // charcoal surface is a calmer wait than a moving one.
                views.rows.visibility = View.GONE
                views.message.visibility = View.GONE
            }

            is UiState.Content -> {
                views.message.visibility = View.GONE
                views.rows.visibility = View.VISIBLE
                rowsAdapter.submit(state.value)
                // Focus has to land somewhere the moment content appears, or
                // the remote does nothing on the first press.
                views.rows.post { views.rows.requestFocus() }
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
        views.rows.visibility = View.GONE
        views.message.visibility = View.VISIBLE
        views.message.setMessage(titleRes, bodyRes)
        views.message.setAction(R.string.action_retry) { viewModel.retry() }
        views.message.post { views.message.focusAction() }
    }

    private fun openDetail(item: MediaItem) {
        (activity as? NavigationHost)?.openDetail(item)
    }

    override fun onDestroyView() {
        // The adapter outlives the view; leaving it attached would keep the
        // whole hierarchy alive behind it.
        binding?.rows?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

