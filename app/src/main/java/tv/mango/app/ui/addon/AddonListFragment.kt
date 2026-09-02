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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.addon.model.Addon
import tv.mango.app.databinding.FragmentAddonListBinding
import tv.mango.app.di.appGraph
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.navigation.Route

/** Every installed add-on. Reordering, enabling and removing live on the add-on's own detail screen. */
class AddonListFragment : Fragment() {

    private var binding: FragmentAddonListBinding? = null
    private val adapter = AddonAdapter(onSelected = ::openDetail)
    private var firstLoad = true

    private val viewModel: AddonListViewModel by viewModels {
        viewModelFactory {
            initializer { AddonListViewModel(appGraph.addonRepository) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentAddonListBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.addonList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddonListFragment.adapter
            itemAnimator = null
        }
        views.addonListAdd.setOnClickListener { openAddAddon() }
        views.addonListMessage.setAction(R.string.action_add_addon) { openAddAddon() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addons.collect(::render)
            }
        }
    }

    private fun render(addons: List<Addon>) {
        val views = binding ?: return
        adapter.submit(addons)

        if (addons.isEmpty()) {
            views.addonList.visibility = View.GONE
            views.addonListMessage.visibility = View.VISIBLE
            views.addonListMessage.setMessage(R.string.addon_list_empty_title, R.string.addon_list_empty_body)
            if (firstLoad) views.addonListMessage.post { views.addonListMessage.focusAction() }
        } else {
            views.addonListMessage.visibility = View.GONE
            views.addonList.visibility = View.VISIBLE
            if (firstLoad) views.addonList.post { views.addonList.requestFocus() }
        }
        firstLoad = false
    }

    private fun openDetail(addon: Addon) {
        (activity as? NavigationHost)?.push(Route.AddonDetail(addon.id))
    }

    private fun openAddAddon() {
        (activity as? NavigationHost)?.push(Route.AddAddon)
    }

    override fun onDestroyView() {
        binding?.addonList?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
