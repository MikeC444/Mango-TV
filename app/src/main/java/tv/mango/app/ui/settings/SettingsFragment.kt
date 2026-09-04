package tv.mango.app.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tv.mango.app.databinding.FragmentSettingsBinding
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.navigation.Route

/**
 * The settings section. Home Screen and Add-ons today, with room for others to
 * join it without either having to move screens.
 */
class SettingsFragment : Fragment() {

    private var binding: FragmentSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentSettingsBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            views.settingsHomeScreenRow.defaultFocusHighlightEnabled = false
            views.settingsAddonsRow.defaultFocusHighlightEnabled = false
        }
        views.settingsHomeScreenRow.setOnClickListener {
            (activity as? NavigationHost)?.push(Route.HomeScreenMenu)
        }
        views.settingsAddonsRow.setOnClickListener {
            (activity as? NavigationHost)?.push(Route.AddonList)
        }
        views.settingsHomeScreenRow.post { views.settingsHomeScreenRow.requestFocus() }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
