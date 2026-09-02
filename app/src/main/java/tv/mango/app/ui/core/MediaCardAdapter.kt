package tv.mango.app.ui.core

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
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
        return CardViewHolder(view as TvCardView, onSelected, onFocused)
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
    ) : RecyclerView.ViewHolder(card) {

        private val artwork: ImageView = card.findViewById(R.id.artwork)
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
        }

        fun bind(item: MediaItem) {
            this.item = item
            // Artwork arrives in the next phase, behind the image pipeline. The
            // card's placeholder surface stands in until then.
            artwork.setImageDrawable(null)
            card.contentDescription = card.context.getString(R.string.cd_poster, item.title)
        }

        fun recycle() {
            item = null
            artwork.setImageDrawable(null)
            // A recycled holder must never arrive scaled or lit from whatever
            // it was last used for.
            card.resetFocusState()
        }
    }
}
