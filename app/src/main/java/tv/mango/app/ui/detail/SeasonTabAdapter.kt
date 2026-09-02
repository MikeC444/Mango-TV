package tv.mango.app.ui.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.models.Season

/**
 * The season selector.
 *
 * The chosen season is marked by being selected as well as by its fill, so it
 * stays distinguishable from the focused one - focus and selection are
 * different states and a viewer has to be able to see both at once.
 */
class SeasonTabAdapter(
    private val onSelected: (Int) -> Unit,
) : RecyclerView.Adapter<SeasonTabAdapter.SeasonViewHolder>() {

    private var seasons: List<Season> = emptyList()
    private var selectedNumber: Int = 1

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newSeasons: List<Season>, selected: Int) {
        seasons = newSeasons
        selectedNumber = selected
        notifyDataSetChanged()
    }

    fun setSelected(number: Int) {
        if (selectedNumber == number) return
        val previous = seasons.indexOfFirst { it.number == selectedNumber }
        val next = seasons.indexOfFirst { it.number == number }
        selectedNumber = number
        if (previous >= 0) notifyItemChanged(previous)
        if (next >= 0) notifyItemChanged(next)
    }

    override fun getItemCount(): Int = seasons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_season_tab, parent, false)
        return SeasonViewHolder(view as Button, onSelected)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        val season = seasons[position]
        holder.bind(season, isSelected = season.number == selectedNumber)
    }

    class SeasonViewHolder(
        private val button: Button,
        onSelected: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(button) {

        private var number: Int = 1

        init {
            button.setOnClickListener { onSelected(number) }
        }

        fun bind(season: Season, isSelected: Boolean) {
            number = season.number
            button.text = button.context.getString(R.string.label_season, season.number)
            button.isSelected = isSelected
        }
    }
}
