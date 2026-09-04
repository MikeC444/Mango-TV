package tv.mango.app.ui.core

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import tv.mango.app.R
import tv.mango.app.cache.ImageLoader
import tv.mango.app.models.MediaItem
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.TypographyScale
import tv.mango.app.utilities.Formatters

/**
 * The cards inside one row.
 *
 * Every row in the application uses this one adapter and one view type, which
 * is what makes a single shared [RecyclerView.RecycledViewPool] worth having:
 * a card scrolled off the end of one row is immediately reusable by any other
 * row on the screen, so the number of card views allocated over a session stays
 * close to the number visible at once.
 *
 * Because that pool is shared across rows that can each have their own poster
 * size and layout style, a recycled card can arrive already sized for a
 * different row than the one it is about to render. [RowRenderSpec] is what
 * puts it right again on every bind, not just on creation.
 */
class MediaCardAdapter(
    private val onSelected: (MediaItem) -> Unit,
    private val onFocused: (MediaItem) -> Unit = {},
    /**
     * The card itself is passed along too, as the anchor a long-press menu
     * animates out of.
     *
     * @return true if the long press was handled.
     */
    private val onLongSelected: (MediaItem, View) -> Boolean = { _, _ -> false },
) : RecyclerView.Adapter<MediaCardAdapter.CardViewHolder>() {

    private var items: List<MediaItem> = emptyList()

    /** Null outside a Home row - Browse and Search keep the original fixed poster size and no caption. */
    private var renderSpec: RowRenderSpec? = null

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
    fun submit(newItems: List<MediaItem>, renderSpec: RowRenderSpec? = null) {
        items = newItems
        this.renderSpec = renderSpec
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
        holder.bind(items[position], renderSpec)
    }

    override fun onViewRecycled(holder: CardViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    class CardViewHolder(
        private val card: TvCardView,
        onSelected: (MediaItem) -> Unit,
        onFocused: (MediaItem) -> Unit,
        onLongSelected: (MediaItem, View) -> Boolean,
    ) : RecyclerView.ViewHolder(card) {

        private val artwork: ImageView = card.findViewById(R.id.artwork)
        private val progress: View = card.findViewById(R.id.progress)
        private val caption: View = card.findViewById(R.id.caption)
        private val captionTitle: TextView = card.findViewById(R.id.caption_title)
        private val captionMeta: TextView = card.findViewById(R.id.caption_meta)
        private val watchedBadge: TextView = card.findViewById(R.id.watched_badge)

        private val defaultWidth = card.resources.getDimensionPixelSize(R.dimen.card_poster_width)
        private val defaultHeight = card.resources.getDimensionPixelSize(R.dimen.card_poster_height)
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
            card.setOnLongClickListener { view -> item?.let { onLongSelected(it, view) } ?: false }

            // Every card inflates its progress bar from the same drawable
            // resource, and those instances share one constant state. Without
            // this, setting the level for one partly watched card would set it
            // for every card on the screen.
            progress.background = progress.background?.mutate()

            watchedBadge.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(RuntimeTheme.colors.accent)
            }
            watchedBadge.setTextColor(RuntimeTheme.colors.textOnAccent)
        }

        fun bind(item: MediaItem, spec: RowRenderSpec?) {
            this.item = item

            val width = spec?.cardWidthPx ?: defaultWidth
            val height = spec?.cardHeightPx ?: defaultHeight
            card.applySize(width, height)

            if (spec?.useBackdropArt == true) {
                ImageLoader.loadBackdrop(artwork, item.images.backdrop, width, height)
            } else {
                ImageLoader.loadPoster(artwork, item.images.poster, width, height)
            }

            val showProgress = (spec?.showProgressBar ?: true) && item.isPartiallyWatched
            if (showProgress) {
                progress.visibility = View.VISIBLE
                // Drawable levels run 0..10000.
                progress.background?.level = (item.progress * MAX_LEVEL).toInt()
            } else {
                progress.visibility = View.GONE
            }

            bindCaption(item, spec)
            bindWatchedBadge(item, spec)

            card.contentDescription = buildDescription(item)
        }

        private fun bindCaption(item: MediaItem, spec: RowRenderSpec?) {
            if (spec == null || !spec.showsCaption) {
                caption.visibility = View.GONE
                card.rasteriseWhileScaling = false
                return
            }
            caption.visibility = View.VISIBLE
            card.rasteriseWhileScaling = true

            val typography = RuntimeTheme.config.value.typography
            val accessibility = RuntimeTheme.config.value.accessibility
            TypographyScale.apply(captionTitle, CAPTION_TITLE_BASE_SP, TypographyScale.titleScale(typography, accessibility))
            TypographyScale.apply(captionMeta, CAPTION_META_BASE_SP, TypographyScale.metadataScale(typography, accessibility))

            captionTitle.text = item.title
            captionTitle.visibility = if (spec.showTitle) View.VISIBLE else View.GONE

            val meta = Formatters.cardCaptionLine(
                context = card.context,
                item = item,
                showYear = spec.showYear,
                showRating = spec.showRating,
                showRuntime = spec.showRuntime,
            )
            captionMeta.text = meta
            captionMeta.visibility = if (meta.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        private fun bindWatchedBadge(item: MediaItem, spec: RowRenderSpec?) {
            val show = (spec?.showWatchedIndicator ?: false) && item.watched
            watchedBadge.visibility = if (show) View.VISIBLE else View.GONE
        }

        /**
         * Cards carry no visible text by default, so the whole of what a card
         * is has to be said here for anyone using a screen reader. Progress is
         * included because it is otherwise conveyed only by a coloured bar.
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
            caption.visibility = View.GONE
            watchedBadge.visibility = View.GONE
            // A recycled holder must never arrive scaled or lit from whatever
            // it was last used for.
            card.resetFocusState()
        }

        private companion object {
            const val MAX_LEVEL = 10_000f

            /** Matches TextAppearance.Mango.CardTitle / CardMeta's own sp - the baseline [TypographyScale] scales from. */
            const val CAPTION_TITLE_BASE_SP = 16f
            const val CAPTION_META_BASE_SP = 14f
        }
    }
}
