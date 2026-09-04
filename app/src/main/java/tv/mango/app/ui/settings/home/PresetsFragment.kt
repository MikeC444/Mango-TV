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
import tv.mango.app.settings.home.HomePreset

/**
 * Settings -> Home Screen -> Presets: one-tap combinations of every other
 * screen in this package.
 *
 * Applying one only ever replaces
 * [tv.mango.app.settings.home.HomeScreenConfig] - see
 * [tv.mango.app.settings.home.HomeScreenPresets]'s own documentation for why
 * that can never touch watch history, Continue Watching, the watchlist,
 * add-ons or playback progress.
 */
class PresetsFragment : Fragment() {

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

        views.screenTitle.setText(R.string.home_screen_section_presets)
        views.screenSubtitle.visibility = View.VISIBLE
        views.screenSubtitle.setText(R.string.presets_subtitle)
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

    private fun buildRows(current: HomePreset): List<SettingsRowSpec> {
        fun row(preset: HomePreset, labelRes: Int, descriptionRes: Int) = SettingsRowSpec.Nav(
            label = getString(labelRes) + if (preset == current) "  •" else "",
            subtitle = getString(descriptionRes),
        ) {
            viewLifecycleOwner.lifecycleScope.launch { appGraph.homeScreenConfigRepository.applyPreset(preset) }
        }

        return listOf(
            row(HomePreset.DEFAULT, R.string.preset_default, R.string.preset_default_description),
            row(HomePreset.CINEMATIC, R.string.preset_cinematic, R.string.preset_cinematic_description),
            row(HomePreset.COMPACT, R.string.preset_compact, R.string.preset_compact_description),
            row(HomePreset.MINIMAL, R.string.preset_minimal, R.string.preset_minimal_description),
            row(HomePreset.LIQUID_GLASS, R.string.preset_liquid_glass, R.string.preset_liquid_glass_description),
        )
    }

    override fun onDestroyView() {
        binding?.optionsList?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
