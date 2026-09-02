package tv.mango.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tv.mango.app.R
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.databinding.FragmentPlaceholderBinding

/**
 * Stands in for a section that has not been built yet.
 *
 * Present so the navigation rail has no dead entries during a phased build:
 * every destination leads somewhere that says plainly what it is. Each section
 * replaces this with its own fragment as it arrives.
 */
class PlaceholderFragment : Fragment() {

    private var binding: FragmentPlaceholderBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentPlaceholderBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sectionName = getString(requireArguments().getInt(ARG_SECTION_NAME))
        binding?.message?.apply {
            setMessage(sectionName, R.string.placeholder_body)
            setAction(null)
        }

        // Nothing on this screen can take focus, so it is handed back to the
        // rail. Otherwise the remote would be dead until the viewer guessed
        // that left still worked.
        view.post { (activity as? NavigationHost)?.focusNavigation() }
    }

    override fun onDestroyView() {
        // Released so the view hierarchy is not held past its own lifetime.
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_SECTION_NAME = "section_name"

        fun of(sectionNameRes: Int): PlaceholderFragment = PlaceholderFragment().apply {
            arguments = Bundle(1).apply { putInt(ARG_SECTION_NAME, sectionNameRes) }
        }
    }
}
