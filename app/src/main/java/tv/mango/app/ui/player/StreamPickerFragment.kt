package tv.mango.app.ui.player

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
import tv.mango.app.addon.model.StreamResult
import tv.mango.app.cache.ImageLoader
import tv.mango.app.data.FailureReason
import tv.mango.app.databinding.FragmentStreamPickerBinding
import tv.mango.app.di.appGraph
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.player.PlayerActivity

/**
 * Shown when Play is pressed. Queries every enabled add-on that can offer a
 * stream and lists the ranked results - the viewer chooses, rather than the
 * application silently taking the first answer.
 */
class StreamPickerFragment : Fragment() {

    private var binding: FragmentStreamPickerBinding? = null
    private var firstLoad = true
    private val adapter = StreamAdapter(onSelected = ::onStreamSelected)

    private val viewModel: StreamPickerViewModel by viewModels {
        viewModelFactory {
            initializer {
                StreamPickerViewModel(
                    appGraph.streamProvider,
                    appGraph.subtitleProvider,
                    appGraph.libraryRepository,
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentStreamPickerBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        viewModel.displayTitle?.let { views.streamPickerTitle.text = it }
        viewModel.displayBackdrop?.let { key ->
            ImageLoader.loadBackdrop(
                target = views.streamPickerBackdrop,
                key = key,
                widthPx = BACKDROP_MAX_WIDTH_PX,
                heightPx = BACKDROP_MAX_HEIGHT_PX,
            )
        }

        views.streamPickerList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StreamPickerFragment.adapter
            itemAnimator = null
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: StreamPickerState) {
        val views = binding ?: return

        views.streamPickerProgress.visibility = View.GONE
        views.streamPickerList.visibility = View.GONE
        views.streamPickerMessage.visibility = View.GONE

        when (state) {
            // Nothing has been decided yet, so firstLoad must not be consumed
            // here - it needs to still be true when the real state arrives.
            StreamPickerState.Loading -> {
                views.streamPickerProgress.visibility = View.VISIBLE
                return
            }

            is StreamPickerState.Content -> {
                adapter.submit(state.streams)
                views.streamPickerList.visibility = View.VISIBLE
                if (firstLoad) views.streamPickerList.post { views.streamPickerList.requestFocus() }
            }

            StreamPickerState.Empty -> showMessage(
                R.string.stream_picker_empty_title,
                R.string.stream_picker_empty_body,
            )

            is StreamPickerState.Error -> when (state.reason) {
                FailureReason.NETWORK ->
                    showMessage(R.string.error_network_title, R.string.error_network_body)
                else ->
                    showMessage(R.string.error_generic_title, R.string.error_generic_body)
            }
        }
        firstLoad = false
    }

    private fun showMessage(titleRes: Int, bodyRes: Int) {
        val views = binding ?: return
        views.streamPickerMessage.visibility = View.VISIBLE
        views.streamPickerMessage.setMessage(titleRes, bodyRes)
        views.streamPickerMessage.setAction(null)
        if (firstLoad) {
            views.streamPickerMessage.post { (activity as? NavigationHost)?.focusNavigation() }
        }
    }

    private fun onStreamSelected(stream: StreamResult) {
        viewLifecycleOwner.lifecycleScope.launch {
            val target = viewModel.playbackFor(stream) ?: return@launch
            startActivity(PlayerActivity.intent(requireContext(), target))
        }
    }

    override fun onDestroyView() {
        binding?.let {
            it.streamPickerList.adapter = null
            ImageLoader.clear(it.streamPickerBackdrop)
        }
        binding = null
        super.onDestroyView()
    }

    private companion object {
        /** As on the home hero and the detail screen: capped well below any television's resolution. */
        const val BACKDROP_MAX_WIDTH_PX = 1280
        const val BACKDROP_MAX_HEIGHT_PX = 720
    }
}
