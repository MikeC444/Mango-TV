package tv.mango.app.ui.addon

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
import tv.mango.app.addon.model.Addon
import tv.mango.app.cache.ImageLoader
import tv.mango.app.databinding.FragmentAddonDetailBinding
import tv.mango.app.di.appGraph
import tv.mango.app.navigation.NavigationHost

/**
 * One installed add-on: what it is, and the four things a viewer can do to it
 * - enable or disable it, move it in priority, remove it.
 */
class AddonDetailFragment : Fragment() {

    private var binding: FragmentAddonDetailBinding? = null
    private var wasShown = false

    private val addonId: String
        get() = requireArguments().getString(ARG_ADDON_ID)!!

    private val viewModel: AddonDetailViewModel by viewModels {
        viewModelFactory {
            initializer { AddonDetailViewModel(addonId, appGraph.addonRepository) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentAddonDetailBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.addonDetailToggleEnabled.setOnClickListener {
            viewModel.addon.value?.let { viewModel.setEnabled(!it.isEnabled) }
        }
        views.addonDetailMoveUp.setOnClickListener { viewModel.moveUp() }
        views.addonDetailMoveDown.setOnClickListener { viewModel.moveDown() }
        views.addonDetailRemove.setOnClickListener { viewModel.remove() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addon.collect(::render)
            }
        }
    }

    private fun render(addon: Addon?) {
        val views = binding ?: return

        if (addon == null) {
            if (wasShown) {
                // The add-on this screen was showing is gone - removed from
                // here, or from another screen. Nothing left to show.
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                views.addonDetailScroll.visibility = View.GONE
                views.addonDetailMessage.visibility = View.VISIBLE
                views.addonDetailMessage.setMessage(R.string.addon_not_found, null)
                (activity as? NavigationHost)?.focusNavigation()
            }
            return
        }

        val firstShow = !wasShown
        wasShown = true

        views.addonDetailMessage.visibility = View.GONE
        views.addonDetailScroll.visibility = View.VISIBLE

        views.addonDetailName.text = addon.name
        views.addonDetailVersion.text = getString(R.string.label_version, addon.manifest.version)

        views.addonDetailDescription.text = addon.manifest.description.orEmpty()
        views.addonDetailDescription.visibility =
            if (addon.manifest.description.isNullOrBlank()) View.GONE else View.VISIBLE

        views.addonDetailTypes.text = getString(R.string.format_types, addon.manifest.types.joinToString(", "))
        views.addonDetailResources.text = getString(
            R.string.format_resources,
            addon.manifest.resources.joinToString(", ") { it.name },
        )
        views.addonDetailManifestUrl.text = getString(R.string.format_manifest_url, addon.manifestUrl)

        views.addonDetailToggleEnabled.setText(
            if (addon.isEnabled) R.string.addon_status_enabled else R.string.addon_status_disabled,
        )

        val logoKey = addon.manifest.logo
        if (logoKey.isNullOrBlank()) {
            ImageLoader.clear(views.addonDetailLogo)
        } else {
            ImageLoader.loadPoster(
                target = views.addonDetailLogo,
                key = logoKey,
                widthPx = resources.getDimensionPixelSize(R.dimen.addon_logo_size),
                heightPx = resources.getDimensionPixelSize(R.dimen.addon_logo_size),
            )
        }

        if (firstShow) views.addonDetailToggleEnabled.post { views.addonDetailToggleEnabled.requestFocus() }
    }

    override fun onDestroyView() {
        binding?.let { ImageLoader.clear(it.addonDetailLogo) }
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ADDON_ID = "addon_id"

        fun of(addonId: String): AddonDetailFragment = AddonDetailFragment().apply {
            arguments = Bundle(1).apply { putString(ARG_ADDON_ID, addonId) }
        }
    }
}
