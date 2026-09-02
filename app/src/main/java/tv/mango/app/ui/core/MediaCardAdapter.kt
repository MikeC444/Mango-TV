package tv.mango.app.ui.core

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.models.MediaItem

/**
 * The cards inside one row.
 *
 * Every row in the application uses this one adapter and one view type, which
 * is what makes a single shared [RecyclerView.RecycledViewPool] worth having:
 * a card scrolled off the end of one row is immediately reusable by any other
 * row on the screen, so the number of card views allocated over a session stays
 * close to the number visible at once.
 */
class MediaCardAdapter(
    private val onSelected: (MediaItem) -> Unit,
    private val onFocused: (MediaItem) -> Unit = {},
    /**
     * @return true if the long press was handled - only ever true for a
     *   Continue Watching card, which is the only kind of card this applies
     *   to. Every other row leaves this at its default and the platform's
     *   own long-click handling simply finds nothing to do.
     */
    private val onLongSelected: (MediaItem) -> Boolean = { false },
) : RecyclerView.Adapter<MediaCardAdapter.CardViewHolder>() {

    private var items: List<MediaItem> = emptyList()

    init {
        // Lets RecyclerView keep holders associated with their content across
        // updates instead of rebinding everything by position.
        setHasStableIds(true)
    }

    /**
     * A row's contents are set once, wholesale. Diffing a list against an empty
     * one only walks it twice to reach the same answer, so the blunt
     * notification is the cheaper of the two here.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submit(newItems: List<MediaItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id.value.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_card, parent, false)
        return CardViewHolder(view as TvCardView, onSelected, onFocused, onLongSelected)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onViewRecycled(holder: CardViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    class CardViewHolder(
        private val card: TvCardView,
        onSelected: (MediaItem) -> Unit,
        onFocused: (MediaItem) -> Unit,
        onLongSelected: (MediaItem) -> Boolean,
    ) : RecyclerView.ViewHolder(card) {

        private val artwork: ImageView = card.findViewById(R.id.artwork)
        private val progress: View = card.findViewById(R.id.progress)
        private val posterWidth = card.resources.getDimensionPixelSize(R.dimen.card_poster_width)
        private val posterHeight = card.resources.getDimensionPixelSize(R.dimen.card_poster_height)
        private var item: MediaItem? = null

        init {
            // One listener per holder, created once, rather than a fresh lambda
            // on every bind.
            card.setOnClickListener { item?.let(onSelected) }
            // Reported so the screen can follow the selection - the hero
            // changing with focus, for instance. The card's own lift is handled
            // inside TvCardView and does not go through here.
            card.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) item?.let(onFocused)
            }
            // A remote's Select button already generates a long-click when
            // held, the same as it does on a touch screen - no extra key
            // handling is needed for this to be D-pad reachable.
            card.setOnLongClickListener { item?.let(onLongSelected) ?: false }

            // Every card inflates its progress bar from the same drawable
            // resource, and those instances share one constant state. Without
            // this, setting the level for one partly watched card would set it
            // for every card on the screen.
            progress.background = progress.background?.mutate()
        }

        fun bind(item: MediaItem) {
            this.item = item

            // Decoded at exactly the card's size, never at the artwork's.
            ImageLoader.loadPoster(artwork, item.images.poster, posterWidth, posterHeight)

            if (item.isPartiallyWatched) {
                progress.visibility = View.VISIBLE
                // Drawable levels run 0..10000.
                progress.background?.level = (item.progress * MAX_LEVEL).toInt()
            } else {
                progress.visibility = View.GONE
            }

            card.contentDescription = buildDescription(item)
        }

        /**
         * Cards carry no visible text, so the whole of what a card is has to be
         * said here for anyone using a screen reader. Progress is included
         * because it is otherwise conveyed only by a coloured bar.
         */
        private fun buildDescription(item: MediaItem): CharSequence {
            val context = card.context
            val base = context.getString(R.string.cd_poster, item.title)
            if (!item.isPartiallyWatched) return base
            val percent = (item.progress * 100).toInt()
            val withProgress = base + ", " + context.getString(R.string.cd_progress, percent)
            if (item.resume == null) return withProgress
            return withProgress + ", " + context.getString(R.string.cd_continue_watching_remove)
        }

        fun recycle() {
            item = null
            // Cancels any request still in flight. Without this a load started
            // for a card that has scrolled away can still complete and deliver
            // the wrong artwork into a view now showing something else.
            ImageLoader.clear(artwork)
            progress.visibility = View.GONE
            // A recycled holder must never arrive scaled or lit from whatever
            // it was last used for.
            card.resetFocusState()
        }

        private companion object {
            const val MAX_LEVEL = 10_000f
        }
    }
}
