package tv.mango.app.ui.settings.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.databinding.FragmentHomeScreenListBinding
import tv.mango.app.di.appGraph
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.navigation.Route
import tv.mango.app.settings.home.HomePreset
import tv.mango.app.settings.home.HomeScreenSettingsSection

/**
 * Settings -> Home Screen's own menu: the thirteen categories the rest of
 * this package implements, all reachable from here and nowhere else on the
 * main navigation - see the class docs on [tv.mango.app.ui.settings.SettingsFragment].
 */
class HomeScreenMenuFragment : Fragment() {

    private var binding: FragmentHomeScreenListBinding? = null
    private val adapter = SettingsRowAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentHomeScreenListBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.screenTitle.setText(R.string.home_screen_settings_title)
        views.screenSubtitle.visibility = View.VISIBLE
        views.screenSubtitle.setText(R.string.home_screen_settings_subtitle)
        views.optionsList.layoutManager = LinearLayoutManager(requireContext())
        views.optionsList.itemAnimator = null
        views.optionsList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appGraph.homeScreenConfigRepository.config.collect { config ->
                    adapter.submit(buildRows(config.preset))
                    views.optionsList.post {
                        if (views.optionsList.findFocus() == null) views.optionsList.requestFocus()
                    }
                }
            }
        }
    }

    private fun buildRows(preset: HomePreset): List<SettingsRowSpec> {
        val host = activity as? NavigationHost
        fun section(section: HomeScreenSettingsSection, labelRes: Int) = SettingsRowSpec.Nav(
            label = getString(labelRes),
            onSelect = { host?.push(Route.HomeScreenSection(section)) },
        )

        return listOf(
            section(HomeScreenSettingsSection.LAYOUT, R.string.home_screen_section_layout),
            SettingsRowSpec.Nav(
                label = getString(R.string.home_screen_section_rows),
                onSelect = { host?.push(Route.CatalogRows) },
            ),
            section(HomeScreenSettingsSection.CARDS, R.string.home_screen_section_cards),
            section(HomeScreenSettingsSection.COLORS, R.string.home_screen_section_colors),
            section(HomeScreenSettingsSection.GLASS, R.string.home_screen_section_glass),
            section(HomeScreenSettingsSection.HERO, R.string.home_screen_section_hero),
            section(HomeScreenSettingsSection.BACKGROUND, R.string.home_screen_section_background),
            section(HomeScreenSettingsSection.NAVIGATION, R.string.home_screen_section_navigation),
            section(HomeScreenSettingsSection.TYPOGRAPHY, R.string.home_screen_section_typography),
            section(HomeScreenSettingsSection.ACCESSIBILITY, R.string.home_screen_section_accessibility),
            SettingsRowSpec.Nav(
                label = getString(R.string.home_screen_section_presets),
                subtitle = presetLabel(preset),
                onSelect = { host?.push(Route.Presets) },
            ),
            SettingsRowSpec.Nav(
                label = getString(R.string.home_screen_section_preview),
                onSelect = { host?.push(Route.PreviewHomeScreen) },
            ),
            SettingsRowSpec.Nav(
                label = getString(R.string.home_screen_section_reset),
                onSelect = { ResetConfirmationDialog(requireContext(), viewLifecycleOwner.lifecycleScope).show() },
            ),
        )
    }

    private fun presetLabel(preset: HomePreset): String = getString(
        when (preset) {
            HomePreset.DEFAULT -> R.string.preset_default
            HomePreset.CINEMATIC -> R.string.preset_cinematic
            HomePreset.COMPACT -> R.string.preset_compact
            HomePreset.MINIMAL -> R.string.preset_minimal
            HomePreset.LIQUID_GLASS -> R.string.preset_liquid_glass
            HomePreset.STREAMER -> R.string.preset_streamer
            HomePreset.CUSTOM -> R.string.preset_custom
        },
    )

    override fun onDestroyView() {
        binding?.optionsList?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
