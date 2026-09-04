package tv.mango.app.ui.settings.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.data.UiState
import tv.mango.app.databinding.FragmentHomeScreenListBinding
import tv.mango.app.di.appGraph
import tv.mango.app.models.ContentRow
import tv.mango.app.models.ContinueWatchingItem
import tv.mango.app.models.HomeContent
import tv.mango.app.navigation.NavigationHost
import tv.mango.app.navigation.Route
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.settings.home.HomeScreenSettingsSection

/**
 * Settings -> Home Screen -> Catalog Rows: every row Home currently offers,
 * in the order it currently offers them, live from the same catalogue Home
 * itself reads.
 *
 * D-pad controls, matching Settings -> Home Screen's own legend:
 *  - UP / DOWN moves focus between rows.
 *  - LEFT / RIGHT reorders the focused row - Hero excepted, which is always
 *    first and describes the hero panel rather than a scrollable row.
 *  - SELECT opens Edit Row.
 *  - Holding SELECT toggles the row on or off without leaving this screen.
 *  - BACK returns to Settings -> Home Screen, not to Home.
 */
class CatalogRowsFragment : Fragment() {

    private var binding: FragmentHomeScreenListBinding? = null
    private val adapter = CatalogRowItemAdapter(
        onSelect = ::openRow,
        onToggle = ::toggleRow,
        onMove = ::moveRow,
    )

    private var entries: List<CatalogRowEntry> = emptyList()

    /** The row a LEFT/RIGHT reorder just moved - focus follows it to its new position rather than staying pinned to a screen slot. */
    private var pendingFocusRowId: String? = null

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

        views.screenTitle.setText(R.string.home_screen_section_rows)
        views.screenSubtitle.visibility = View.VISIBLE
        views.screenSubtitle.setText(R.string.catalog_rows_subtitle)
        views.optionsList.layoutManager = LinearLayoutManager(requireContext())
        views.optionsList.itemAnimator = null
        views.optionsList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    appGraph.catalogRepository.home(),
                    appGraph.libraryRepository.continueWatching(),
                    appGraph.homeScreenConfigRepository.config,
                    ::buildEntries,
                ).collect { built ->
                    entries = built
                    adapter.submit(built)
                    views.optionsList.post { restoreFocus(views) }
                }
            }
        }
    }

    private fun buildEntries(
        catalogState: UiState<HomeContent>,
        continueWatching: List<ContinueWatchingItem>,
        config: HomeScreenConfig,
    ): List<CatalogRowEntry> {
        val catalogRows = (catalogState as? UiState.Content)?.value?.rows.orEmpty()
        val rows = if (continueWatching.isNotEmpty()) {
            listOf(ContentRow(CONTINUE_WATCHING_ROW_ID, getString(R.string.label_continue_watching), emptyList())) + catalogRows
        } else {
            catalogRows
        }

        val byId = rows.associateBy { it.id }
        val ordered = config.rows.order.mapNotNull { byId[it] } + rows.filter { it.id !in config.rows.order }

        val hero = CatalogRowEntry(
            id = HERO_ROW_ID,
            title = getString(R.string.hero_row_label),
            visible = config.hero.enabled,
            reorderable = false,
        )
        val resetAction = CatalogRowEntry(
            id = RESET_ACTION_ID,
            title = getString(R.string.action_reset_row_order),
            visible = true,
            reorderable = false,
        )
        return listOf(hero) + ordered.map { row ->
            val rowConfig = config.rows.configFor(row.id)
            CatalogRowEntry(
                id = row.id,
                title = rowConfig.customTitle?.takeIf { it.isNotBlank() } ?: row.title,
                visible = rowConfig.visible,
                reorderable = true,
            )
        } + resetAction
    }

    private fun restoreFocus(views: FragmentHomeScreenListBinding) {
        val targetId = pendingFocusRowId
        val index = if (targetId != null) entries.indexOfFirst { it.id == targetId } else -1
        pendingFocusRowId = null

        if (index >= 0) {
            val holder = views.optionsList.findViewHolderForAdapterPosition(index)
            if (holder?.itemView?.requestFocus() == true) return
        }
        if (views.optionsList.findFocus() == null) views.optionsList.requestFocus()
    }

    private fun openRow(entry: CatalogRowEntry) {
        val host = activity as? NavigationHost ?: return
        when (entry.id) {
            HERO_ROW_ID -> host.push(Route.HomeScreenSection(HomeScreenSettingsSection.HERO))
            RESET_ACTION_ID -> viewLifecycleOwner.lifecycleScope.launch {
                appGraph.homeScreenConfigRepository.setRowOrder(emptyList())
            }
            else -> host.push(Route.EditRow(entry.id, entry.title))
        }
    }

    private fun toggleRow(entry: CatalogRowEntry) {
        if (entry.id == RESET_ACTION_ID) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (entry.id == HERO_ROW_ID) {
                appGraph.homeScreenConfigRepository.update { it.copy(hero = it.hero.copy(enabled = !entry.visible)) }
            } else {
                appGraph.homeScreenConfigRepository.updateRow(entry.id) { it.copy(visible = !entry.visible) }
            }
        }
    }

    /** [delta] is -1 to move up, +1 to move down; [entry] must be [CatalogRowEntry.reorderable]. */
    private fun moveRow(entry: CatalogRowEntry, delta: Int) {
        val reorderable = entries.filter { it.reorderable }
        val index = reorderable.indexOfFirst { it.id == entry.id }
        val target = index + delta
        if (index < 0 || target !in reorderable.indices) return

        val newOrder = reorderable.map { it.id }.toMutableList()
        val moved = newOrder.removeAt(index)
        newOrder.add(target, moved)

        pendingFocusRowId = entry.id
        viewLifecycleOwner.lifecycleScope.launch {
            appGraph.homeScreenConfigRepository.setRowOrder(newOrder)
        }
    }

    override fun onDestroyView() {
        binding?.optionsList?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val CONTINUE_WATCHING_ROW_ID = "continue_watching"
    }
}

/** Shared with [CatalogRowItemAdapter], which is not nested inside the fragment. */
private const val HERO_ROW_ID = "hero"
private const val RESET_ACTION_ID = "__reset_row_order__"

/** One row on the Catalog Rows screen. */
data class CatalogRowEntry(
    val id: String,
    val title: String,
    val visible: Boolean,
    val reorderable: Boolean,
)

/** A drag-handle-and-status row per catalogue row, reusing [SettingsOptionRow] for its focus and glass treatment. */
private class CatalogRowItemAdapter(
    private val onSelect: (CatalogRowEntry) -> Unit,
    private val onToggle: (CatalogRowEntry) -> Unit,
    private val onMove: (CatalogRowEntry, Int) -> Unit,
) : RecyclerView.Adapter<CatalogRowItemAdapter.Holder>() {

    private var entries: List<CatalogRowEntry> = emptyList()

    init {
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newEntries: List<CatalogRowEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = entries.size

    override fun getItemId(position: Int): Long = entries[position].id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_option_row, parent, false)
        return Holder(view as SettingsOptionRow)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(entries[position], onSelect, onToggle, onMove)
    }

    class Holder(private val row: SettingsOptionRow) : RecyclerView.ViewHolder(row) {
        fun bind(
            entry: CatalogRowEntry,
            onSelect: (CatalogRowEntry) -> Unit,
            onToggle: (CatalogRowEntry) -> Unit,
            onMove: (CatalogRowEntry, Int) -> Unit,
        ) {
            row.setOnClickListener { onSelect(entry) }
            row.onLeft = null
            row.onRight = null

            if (entry.id == RESET_ACTION_ID) {
                row.bind(entry.title, "›")
                row.setOnLongClickListener(null)
                row.isLongClickable = false
                return
            }

            val status = row.context.getString(if (entry.visible) R.string.value_on else R.string.value_off)
            row.bind("☰  ${entry.title}", status)
            row.setOnLongClickListener { onToggle(entry); true }
            if (entry.reorderable) {
                row.onLeft = { onMove(entry, -1) }
                row.onRight = { onMove(entry, 1) }
            }
        }
    }
}
