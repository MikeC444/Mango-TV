package tv.mango.app.ui.detail

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import tv.mango.app.R
import tv.mango.app.databinding.ViewSeasonsBinding
import tv.mango.app.models.Episode
import tv.mango.app.models.Season
import tv.mango.app.ui.core.CardSpacingDecoration

/**
 * The seasons and episodes section of a series.
 *
 * Only ever holds one season's episodes. A long-running series is thousands of
 * records, and the difference between loading a season and loading a run is the
 * difference between a screen that opens now and one that stalls on data nobody
 * asked to see all of.
 *
 * The selector hides itself for a series with a single season, where a row of
 * one button would be a control that cannot do anything.
 */
class SeasonsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSeasonsBinding.inflate(LayoutInflater.from(context), this)

    private val tabAdapter = SeasonTabAdapter(onSelected = { onSeasonSelected?.invoke(it) })
    private val episodeAdapter = EpisodeAdapter(onSelected = { onEpisodeSelected?.invoke(it) })

    var onSeasonSelected: ((Int) -> Unit)? = null
    var onEpisodeSelected: ((Episode) -> Unit)? = null

    init {
        orientation = VERTICAL
        // A focused episode card grows past the list's bounds.
        clipChildren = false

        val gap = resources.getDimensionPixelSize(R.dimen.card_gap)

        binding.seasonTabs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tabAdapter
            itemAnimator = null
        }

        binding.episodeList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(CardSpacingDecoration.Row(gap))
            adapter = episodeAdapter
            itemAnimator = null
            // No setHasFixedSize here: the list has wrap_content height and
            // starts empty, so binding the first season does change its size.
            isFocusable = false
            descendantFocusability = FOCUS_AFTER_DESCENDANTS
        }
    }

    fun showSeasons(seasons: List<Season>, selected: Int) {
        binding.seasonTabs.visibility = if (seasons.size > 1) View.VISIBLE else View.GONE
        tabAdapter.submit(seasons, selected)
    }

    fun setSelectedSeason(number: Int) {
        tabAdapter.setSelected(number)
    }

    fun showEpisodes(episodes: List<Episode>) {
        episodeAdapter.submit(episodes)
        // A new season starts at its first episode rather than wherever the
        // previous season happened to be scrolled to.
        binding.episodeList.scrollToPosition(0)
    }

    fun release() {
        binding.seasonTabs.adapter = null
        binding.episodeList.adapter = null
    }
}
