package tv.mango.app.ui.core

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.models.ContentRow
import tv.mango.app.models.MediaItem

/**
 * The vertical list of content rows.
 *
 * Each row holder owns a nested horizontal list. The nested lists all draw from
 * the outer list's recycled view pool, so cards are shared across the whole
 * screen rather than pooled per row.
 */
class ContentRowsAdapter(
    private val onItemSelected: (MediaItem) -> Unit,
    /** Called as focus moves between cards, so the hero can follow the selection. */
    private val onItemFocused: (MediaItem) -> Unit = {},
) : RecyclerView.Adapter<ContentRowsAdapter.RowViewHolder>() {

    private var rows: List<ContentRow> = emptyList()
    private var sharedPool: RecyclerView.RecycledViewPool? = null
    private var focusedRowProvider: () -> Int = { RecyclerView.NO_POSITION }

    init {
        setHasStableIds(true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        sharedPool = recyclerView.recycledViewPool
        (recyclerView as? RowsRecyclerView)?.let { rowsView ->
            focusedRowProvider = { rowsView.focusedRowPosition }
        }
    }

    /** Rows are delivered as a complete screen, not incrementally. */
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newRows: List<ContentRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemId(position: Int): Long = rows[position].id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_content_row, parent, false)
        return RowViewHolder(view as RowContainerView, sharedPool, onItemSelected, onItemFocused)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        // A row arriving on screen must already be in the right state. Animating
        // it into place would mean every row fading as it scrolls into view.
        val active = position == focusedRowProvider()
        holder.bind(rows[position], receded = !active)
    }

    class RowViewHolder(
        private val container: RowContainerView,
        sharedPool: RecyclerView.RecycledViewPool?,
        onItemSelected: (MediaItem) -> Unit,
        onItemFocused: (MediaItem) -> Unit,
    ) : RecyclerView.ViewHolder(container) {

        private val title: TextView = container.findViewById(R.id.row_title)
        private val list: RowRecyclerView = container.findViewById(R.id.row_list)
        private val cardAdapter = MediaCardAdapter(onItemSelected, onItemFocused)

        init {
            // Rows draw their cards from, and return them to, the pool owned by
            // the vertical list. The per-list view cache is left at its default:
            // it holds recently bound cards for an immediate return, which is
            // exactly what scrolling back along a row does.
            sharedPool?.let(list::setRecycledViewPool)
            list.adapter = cardAdapter
        }

        fun bind(row: ContentRow, receded: Boolean) {
            title.text = row.title
            list.resetFocusMemory()
            list.scrollToPosition(0)
            cardAdapter.submit(row.items)
            container.setReceded(receded, animate = false)
        }
    }
}
