package tv.mango.app.ui.settings.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R

/** Renders a [SettingsRowSpec] list as [SettingsOptionRow]s - the one adapter every Home Screen settings screen uses. */
class SettingsRowAdapter : RecyclerView.Adapter<SettingsRowAdapter.RowHolder>() {

    private var rows: List<SettingsRowSpec> = emptyList()

    init {
        // A row's label is a stable identity across a screen's own lifetime -
        // only its value text changes as a viewer cycles it - so keeping
        // stable ids here is what lets a resubmit (every settings change
        // re-reads the whole configuration and rebuilds every row) rebind the
        // same, still-focused view in place rather than risk losing focus to
        // a freshly created one.
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newRows: List<SettingsRowSpec>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemId(position: Int): Long = rows[position].label.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_option_row, parent, false)
        return RowHolder(view as SettingsOptionRow)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bind(rows[position])
    }

    class RowHolder(private val row: SettingsOptionRow) : RecyclerView.ViewHolder(row) {

        fun bind(spec: SettingsRowSpec) {
            when (spec) {
                is SettingsRowSpec.Cycle -> {
                    row.bind(spec.label, "◀  ${spec.valueText}  ▶")
                    row.onLeft = spec.onLeft
                    row.onRight = spec.onRight
                    row.setOnClickListener { spec.onRight() }
                }
                is SettingsRowSpec.Toggle -> {
                    val text = if (spec.isOn) "◀  ON  ▶" else "◀  OFF  ▶"
                    row.bind(spec.label, text)
                    row.onLeft = { spec.onToggle() }
                    row.onRight = { spec.onToggle() }
                    row.setOnClickListener { spec.onToggle() }
                }
                is SettingsRowSpec.Nav -> {
                    val text = if (spec.subtitle != null) "${spec.subtitle}  ›" else "›"
                    row.bind(spec.label, text)
                    row.onLeft = null
                    row.onRight = null
                    row.setOnClickListener { spec.onSelect() }
                }
            }
        }
    }
}
