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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.mango.app.R
import tv.mango.app.addon.AddonInstaller
import tv.mango.app.cache.ImageLoader
import tv.mango.app.databinding.FragmentAddAddonBinding
import tv.mango.app.di.appGraph
import tv.mango.app.pairing.QrCodeGenerator

/**
 * Paste a manifest URL, see what the add-on says about itself, confirm.
 *
 * Two steps, never one: the primary button previews until an add-on has
 * answered with something installable, and only then does the same button
 * install it. Nothing is written to storage before that second press.
 */
class AddAddonFragment : Fragment() {

    private var binding: FragmentAddAddonBinding? = null
    private var installedHandled = false

    private val viewModel: AddAddonViewModel by viewModels {
        viewModelFactory {
            initializer { AddAddonViewModel(appGraph.addonInstaller) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentAddAddonBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.addAddonPrimary.setOnClickListener { onPrimaryClicked() }
        views.addAddonCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        views.addAddonUrl.post { views.addAddonUrl.requestFocus() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.pairingUrl.collect(::renderPairing) }
            }
        }
    }

    /**
     * The QR panel is simply absent, not an error state, when there is no
     * address to offer yet - most often because the pairing server is still
     * starting, occasionally because the device has no network at all.
     */
    private suspend fun renderPairing(url: String?) {
        if (url == null) {
            binding?.addAddonPairing?.visibility = View.GONE
            return
        }
        val sizePx = resources.getDimensionPixelSize(R.dimen.qr_code_size)
        val bitmap = withContext(Dispatchers.Default) { QrCodeGenerator.generate(url, sizePx) }
        val views = binding ?: return
        if (bitmap != null) {
            views.addAddonQr.setImageBitmap(bitmap)
            views.addAddonPairing.visibility = View.VISIBLE
        }
    }

    private fun onPrimaryClicked() {
        val views = binding ?: return
        when (viewModel.state.value) {
            is AddAddonState.Ready -> viewModel.install()
            else -> viewModel.preview(views.addAddonUrl.text?.toString().orEmpty())
        }
    }

    private fun render(state: AddAddonState) {
        val views = binding ?: return

        views.addAddonPrimary.isEnabled = state !is AddAddonState.Loading && state !is AddAddonState.Installing
        views.addAddonPrimary.setText(
            if (state is AddAddonState.Ready) R.string.action_install else R.string.action_preview,
        )
        views.addAddonError.visibility = View.GONE
        views.addAddonPreview.visibility = View.GONE
        views.addAddonConfiguration.visibility = View.GONE

        when (state) {
            AddAddonState.Idle, AddAddonState.Loading, AddAddonState.Installing -> Unit

            is AddAddonState.Ready -> showReady(state.preview)

            is AddAddonState.NeedsConfiguration -> {
                views.addAddonConfiguration.visibility = View.VISIBLE
                views.addAddonConfigureUrl.text = state.preview.configureUrl
                syncUrlField(state.preview.manifestUrl)
            }

            is AddAddonState.Failed -> {
                views.addAddonError.visibility = View.VISIBLE
                views.addAddonError.setText(errorFor(state.reason))
            }

            AddAddonState.Installed -> if (!installedHandled) {
                installedHandled = true
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun showReady(preview: AddonInstaller.Preview.Ready) {
        val views = binding ?: return
        val manifest = preview.manifest

        syncUrlField(preview.manifestUrl)
        views.addAddonPreview.visibility = View.VISIBLE
        views.addAddonName.text = manifest.name
        views.addAddonVersion.text = getString(R.string.label_version, manifest.version)

        views.addAddonDescription.text = manifest.description.orEmpty()
        views.addAddonDescription.visibility =
            if (manifest.description.isNullOrBlank()) View.GONE else View.VISIBLE

        views.addAddonTypes.text = getString(R.string.format_types, manifest.types.joinToString(", "))
        views.addAddonResources.text = getString(
            R.string.format_resources,
            manifest.resources.joinToString(", ") { it.name },
        )
        views.addAddonAlreadyInstalled.visibility =
            if (preview.alreadyInstalled) View.VISIBLE else View.GONE

        val logoKey = manifest.logo
        if (logoKey.isNullOrBlank()) {
            ImageLoader.clear(views.addAddonLogo)
        } else {
            ImageLoader.loadPoster(
                target = views.addAddonLogo,
                key = logoKey,
                widthPx = resources.getDimensionPixelSize(R.dimen.addon_logo_size),
                heightPx = resources.getDimensionPixelSize(R.dimen.addon_logo_size),
            )
        }
    }

    /**
     * Keeps the manual field showing whichever address is actually being
     * previewed, including one that arrived from a phone rather than being
     * typed - so the field never disagrees with what is on screen below it.
     */
    private fun syncUrlField(url: String) {
        val field = binding?.addAddonUrl ?: return
        if (field.text?.toString() != url) field.setText(url)
    }

    private fun errorFor(reason: AddonInstaller.Preview.Reason): Int = when (reason) {
        AddonInstaller.Preview.Reason.INVALID_URL -> R.string.add_addon_error_invalid_url
        AddonInstaller.Preview.Reason.UNREACHABLE -> R.string.add_addon_error_unreachable
        AddonInstaller.Preview.Reason.NOT_AN_ADDON -> R.string.add_addon_error_not_an_addon
        AddonInstaller.Preview.Reason.INCOMPLETE_MANIFEST -> R.string.add_addon_error_incomplete
        AddonInstaller.Preview.Reason.NOTHING_USABLE -> R.string.add_addon_error_nothing_usable
    }

    override fun onDestroyView() {
        binding?.let {
            ImageLoader.clear(it.addAddonLogo)
            it.addAddonQr.setImageBitmap(null)
        }
        binding = null
        super.onDestroyView()
    }
}
