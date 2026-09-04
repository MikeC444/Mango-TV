package tv.mango.app.ui.core

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.databinding.DialogCardActionsBinding
import tv.mango.app.models.MediaItem
import tv.mango.app.models.MediaType
import tv.mango.app.repository.LibraryRepository
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.ThemeDrawables
import tv.mango.app.theme.ThemeDrawables.panelRadiusDp

/**
 * The quick-action menu a long press opens on any poster.
 *
 * Everything on it is already one press away, through the card itself or the
 * detail screen it opens - the point is skipping that trip on a remote where
 * every trip costs several D-pad presses.
 *
 * It opens as though it were part of the poster itself: the panel grows out
 * of [anchor]'s own position and size rather than simply appearing centred,
 * so the menu reads as this title's own controls rather than an unrelated
 * popup that happens to have opened on top of it.
 *
 * A plain [Dialog] rather than a DialogFragment: nothing here needs to survive
 * a rotation or a process-death Bundle round trip - this is fixed-orientation
 * hardware and the dialog is gone again in a couple of seconds either way -
 * and a [MediaItem] is not Parcelable, the same reasoning behind
 * [tv.mango.app.player.PendingPlayback] and
 * [tv.mango.app.ui.search.PendingSimilarSearch].
 */
class CardActionSheet(
    context: Context,
    private val item: MediaItem,
    private val anchor: View,
    private val library: LibraryRepository,
    private val scope: CoroutineScope,
    private val onPlay: (MediaItem) -> Unit,
    private val onDetails: (MediaItem) -> Unit,
    private val onFindSimilar: (MediaItem) -> Unit,
    /**
     * Only ever non-null for a Continue Watching card. Kept as a distinct,
     * optional row rather than folded into one of the others: it is the one
     * action here that removes something instead of opening or toggling it,
     * and every other card on the application has nothing to offer it.
     */
    private val onRemoveFromContinueWatching: ((MediaItem) -> Unit)? = null,
) : Dialog(context, R.style.Theme_Mango_ActionSheet) {

    private lateinit var binding: DialogCardActionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCardActionsBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // Hidden until show() can measure the panel's real, laid-out size and
        // position it against the anchor - otherwise the very first frame
        // would flash the panel at full size before the reveal transform
        // below ever got a chance to apply.
        binding.root.alpha = 0f
        binding.actionPanel.alpha = 0f

        applyTheme()

        binding.actionHeaderType.setText(
            if (item.type == MediaType.MOVIE) R.string.label_movie else R.string.label_series,
        )
        binding.actionHeaderTitle.text = item.title

        // Each action row is an <include>, which ViewBinding surfaces as its
        // own nested ItemCardActionBinding rather than the TextView directly
        // - .root is that TextView.
        binding.actionPlay.root.setText(R.string.action_sheet_play)
        binding.actionPlay.root.setOnClickListener { dismiss(); onPlay(item) }

        binding.actionDetails.root.setText(R.string.action_sheet_details)
        binding.actionDetails.root.setOnClickListener { dismiss(); onDetails(item) }

        binding.actionSimilar.root.setText(R.string.action_sheet_find_similar)
        binding.actionSimilar.root.setOnClickListener { dismiss(); onFindSimilar(item) }

        binding.actionWatchlist.root.setOnClickListener { toggleWatchlist() }
        binding.actionWatched.root.setOnClickListener { toggleWatched() }

        onRemoveFromContinueWatching?.let { remove ->
            binding.actionRemoveContinueWatching.root.setText(R.string.action_sheet_remove_continue_watching)
            binding.actionRemoveContinueWatching.root.visibility = View.VISIBLE
            binding.actionRemoveContinueWatching.root.setOnClickListener { dismiss(); remove(item) }
        }

        // Read once rather than collected: the sheet is on screen for a few
        // seconds and closes on any action, so there is nothing later for an
        // ongoing subscription to usefully update.
        scope.launch { bindWatchlist(library.isInWatchlist(item.id).first()) }
        scope.launch { bindWatched(library.isWatched(item.id).first()) }
    }

    override fun show() {
        super.show()

        binding.root.animate()
            .alpha(1f)
            .setDuration(MotionSpec.DURATION_STANDARD)
            .setInterpolator(MotionSpec.standard)
            .start()

        // Waits for the panel's own layout pass - its wrap_content height
        // depends on the title's line count, known only once measured - so
        // the shrink-to-anchor starting point below is computed against its
        // real size rather than a guess.
        binding.actionPanel.post(::revealFromAnchor)

        binding.actionPlay.root.post { binding.actionPlay.root.requestFocus() }
    }

    /**
     * Parks the panel over [anchor] at [anchor]'s own size, then animates it
     * to its resting size and centred position - the "grows out of the
     * poster" motion. Falls back to a plain fade if the anchor has scrolled
     * off screen and been recycled in the moment it took to get here.
     */
    private fun revealFromAnchor() {
        val panel = binding.actionPanel
        if (!anchor.isAttachedToWindow || panel.width == 0 || panel.height == 0) {
            panel.alpha = 1f
            return
        }

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val panelLocation = IntArray(2)
        panel.getLocationOnScreen(panelLocation)

        val anchorCenterX = anchorLocation[0] + anchor.width / 2f
        val anchorCenterY = anchorLocation[1] + anchor.height / 2f
        val panelCenterX = panelLocation[0] + panel.width / 2f
        val panelCenterY = panelLocation[1] + panel.height / 2f

        val startScaleX = (anchor.width / panel.width.toFloat()).coerceIn(MIN_SCALE, 1f)
        val startScaleY = (anchor.height / panel.height.toFloat()).coerceIn(MIN_SCALE, 1f)

        panel.pivotX = panel.width / 2f
        panel.pivotY = panel.height / 2f
        panel.scaleX = startScaleX
        panel.scaleY = startScaleY
        panel.translationX = anchorCenterX - panelCenterX
        panel.translationY = anchorCenterY - panelCenterY

        panel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(MotionSpec.DURATION_EMPHASIZED)
            .setInterpolator(MotionSpec.emphasized)
            .start()
    }

    private fun applyTheme() {
        val colors = RuntimeTheme.colors
        val glass = RuntimeTheme.config.value.glass
        val density = context.resources.displayMetrics.density
        val panelCorner = glass.cornerRadius.panelRadiusDp() * density
        val rowCorner = context.resources.getDimension(R.dimen.panel_corner) / 2f

        binding.actionPanel.background = ThemeDrawables.glassPanel(colors, glass, panelCorner)
        binding.actionHeaderType.setTextColor(colors.accent)

        listOf(
            binding.actionPlay.root,
            binding.actionWatchlist.root,
            binding.actionWatched.root,
            binding.actionDetails.root,
            binding.actionSimilar.root,
            binding.actionRemoveContinueWatching.root,
        ).forEach { row -> row.background = ThemeDrawables.surfaceFocusBackground(colors, glass, rowCorner) }
    }

    private fun toggleWatchlist() {
        scope.launch {
            val next = !library.isInWatchlist(item.id).first()
            library.setInWatchlist(item.id, next)
            bindWatchlist(next)
        }
    }

    private fun toggleWatched() {
        scope.launch {
            val next = !library.isWatched(item.id).first()
            library.setWatched(item.id, next)
            bindWatched(next)
        }
    }

    private fun bindWatchlist(inLibrary: Boolean) {
        binding.actionWatchlist.root.setText(
            if (inLibrary) R.string.action_sheet_remove_from_library else R.string.action_sheet_add_to_library,
        )
    }

    private fun bindWatched(watched: Boolean) {
        binding.actionWatched.root.setText(
            if (watched) R.string.action_unmark_watched else R.string.action_mark_watched,
        )
    }

    private companion object {
        /** A poster is narrower and shorter than the panel; never shrink past legibility. */
        const val MIN_SCALE = 0.35f
    }
}
