package tv.mango.app.ui.addon

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.addon.model.Addon
import tv.mango.app.cache.ImageLoader

/** The installed add-ons, in priority order. Selecting one opens its details. */
class AddonAdapter(
    private val onSelected: (Addon) -> Unit,
) : RecyclerView.Adapter<AddonAdapter.AddonViewHolder>() {

    private var addons: List<Addon> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newAddons: List<Addon>) {
        addons = newAddons
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = addons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonViewHolder =
        AddonViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_addon, parent, false),
            onSelected,
        )

    override fun onBindViewHolder(holder: AddonViewHolder, position: Int) {
        holder.bind(addons[position])
    }

    override fun onViewRecycled(holder: AddonViewHolder) {
        holder.release()
    }

    class AddonViewHolder(
        private val view: View,
        private val onSelected: (Addon) -> Unit,
    ) : RecyclerView.ViewHolder(view) {

        private val logo: ImageView = view.findViewById(R.id.addon_logo)
        private val name: TextView = view.findViewById(R.id.addon_name)
        private val subtitle: TextView = view.findViewById(R.id.addon_subtitle)

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view.defaultFocusHighlightEnabled = false
            }
        }

        fun bind(addon: Addon) {
            name.text = addon.name
            subtitle.text = subtitleFor(addon)
            view.contentDescription = null
            view.setOnClickListener { onSelected(addon) }

            val logoKey = addon.manifest.logo
            if (logoKey.isNullOrBlank()) {
                ImageLoader.clear(logo)
            } else {
                ImageLoader.loadPoster(
                    target = logo,
                    key = logoKey,
                    widthPx = view.resources.getDimensionPixelSize(R.dimen.addon_logo_size),
                    heightPx = view.resources.getDimensionPixelSize(R.dimen.addon_logo_size),
                )
            }
        }

        fun release() {
            ImageLoader.clear(logo)
        }

        private fun subtitleFor(addon: Addon): String {
            val version = view.context.getString(R.string.label_version, addon.manifest.version)
            val status = view.context.getString(
                if (addon.isEnabled) R.string.addon_status_enabled else R.string.addon_status_disabled,
            )
            val resources = addon.manifest.resources.joinToString(", ") { resource ->
                resource.name.replaceFirstChar { it.uppercase() }
            }
            return listOfNotNull(version, status, resources.ifBlank { null }).joinToString(" · ")
        }
    }
}
