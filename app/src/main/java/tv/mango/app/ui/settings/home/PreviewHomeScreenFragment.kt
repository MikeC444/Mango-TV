package tv.mango.app.ui.settings.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tv.mango.app.databinding.FragmentPreviewHomeScreenBinding
import tv.mango.app.di.appGraph
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.ui.home.HomeFragment

/**
 * Settings -> Home Screen -> Preview Home Screen.
 *
 * Every other screen in this package applies a change the instant a viewer
 * makes it - Settings -> Home Screen's own "Live Application" promise, honoured
 * everywhere rather than only here. What Preview adds on top is a safety net:
 * it remembers the configuration as it stood the moment this screen opened,
 * so a viewer who tries out several changes while looking at a live Home
 * screen can back every one of them out with Cancel, or confirm they are
 * happy with Apply. Neither button is the only way to keep a change - leaving
 * this screen any other way (Back, the navigation rail) keeps whatever is
 * current too, the same as every other settings screen.
 *
 * The preview itself is not a look-alike: it hosts the real
 * [HomeFragment] as a child fragment, so the hero, every row, focus and
 * scrolling all behave exactly as they do on the actual Home screen.
 */
class PreviewHomeScreenFragment : Fragment() {

    private var binding: FragmentPreviewHomeScreenBinding? = null
    private var snapshot: HomeScreenConfig? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentPreviewHomeScreenBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        if (childFragmentManager.findFragmentById(views.previewHomeContainer.id) == null) {
            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(views.previewHomeContainer.id, HomeFragment())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            snapshot = appGraph.homeScreenConfigRepository.config.first()
        }

        views.previewCancel.setOnClickListener {
            val restore = snapshot
            if (restore != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    appGraph.homeScreenConfigRepository.restore(restore)
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        views.previewApply.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        views.previewCancel.post { views.previewCancel.requestFocus() }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
