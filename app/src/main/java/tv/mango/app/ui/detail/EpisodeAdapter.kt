package tv.mango.app.ui.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.models.Episode
import tv.mango.app.ui.core.TvCardView
import tv.mango.app.utilities.Formatters

/**
 * One season's episodes.
 *
 * These cards carry text, unlike the poster cards, so each opts into being
 * rasterised while it scales - otherwise the focus animation would re-render
 * the glyphs on every frame.
 */
class EpisodeAdapter(
    private val onSelected: (Episode) -> Unit,
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    private var episodes: List<Episode> = emptyList()

    init {
        setHasStableIds(true)
    }

    /** A season is delivered whole; there is nothing to diff against. */
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newEpisodes: List<Episode>) {
        episodes = newEpisodes
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = episodes.size

    override fun getItemId(position: Int): Long = episodes[position].id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode_card, parent, false)
        return EpisodeViewHolder(view as TvCardView, onSelected)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(episodes[position])
    }

    override fun onViewRecycled(holder: EpisodeViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    class EpisodeViewHolder(
        private val card: TvCardView,
        onSelected: (Episode) -> Unit,
    ) : RecyclerView.ViewHolder(card) {

        private val thumbnail: ImageView = card.findViewById(R.id.episode_thumbnail)
        private val title: TextView = card.findViewById(R.id.episode_title)
        private val meta: TextView = card.findViewById(R.id.episode_meta)
        private val width = card.resources.getDimensionPixelSize(R.dimen.card_episode_width)
        private val height = card.resources.getDimensionPixelSize(R.dimen.card_episode_height)

        private var episode: Episode? = null

        init {
            card.rasteriseWhileScaling = true
            card.setOnClickListener { episode?.let(onSelected) }
        }

        fun bind(episode: Episode) {
            this.episode = episode
            val context = card.context

            title.text = context.getString(
                R.string.format_episode_title,
                episode.number,
                episode.title,
            )
            meta.text = Formatters.runtime(context, episode.runtimeMinutes)
            ImageLoader.loadPoster(thumbnail, episode.thumbnail, width, height)

            // The synopsis is the accessible description rather than a fourth
            // line of type: at this card size it would be two words per line.
            card.contentDescription = context.getString(
                R.string.cd_episode,
                episode.number,
                episode.title,
                episode.synopsis,
            )
        }

        fun recycle() {
            episode = null
            ImageLoader.clear(thumbnail)
            card.resetFocusState()
        }
    }
}
