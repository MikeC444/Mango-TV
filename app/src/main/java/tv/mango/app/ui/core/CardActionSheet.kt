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

/**
 * The quick-action menu a long press opens on any poster.
 *
 * Everything on it is already one press away, through the card itself or the
 * detail screen it opens - the point is skipping that trip on a remote where
 * every trip costs several D-pad presses.
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

        binding.actionHeaderType.setText(
            if (item.type == MediaType.MOVIE) R.string.label_movie else R.string.label_series,
        )
        binding.actionHeaderTitle.text = item.title

        binding.actionPlay.setText(R.string.action_sheet_play)
        binding.actionPlay.setOnClickListener { dismiss(); onPlay(item) }

        binding.actionDetails.setText(R.string.action_sheet_details)
        binding.actionDetails.setOnClickListener { dismiss(); onDetails(item) }

        binding.actionSimilar.setText(R.string.action_sheet_find_similar)
        binding.actionSimilar.setOnClickListener { dismiss(); onFindSimilar(item) }

        binding.actionWatchlist.setOnClickListener { toggleWatchlist() }
        binding.actionWatched.setOnClickListener { toggleWatched() }

        onRemoveFromContinueWatching?.let { remove ->
            binding.actionRemoveContinueWatching.setText(R.string.action_sheet_remove_continue_watching)
            binding.actionRemoveContinueWatching.visibility = View.VISIBLE
            binding.actionRemoveContinueWatching.setOnClickListener { dismiss(); remove(item) }
        }

        // Read once rather than collected: the sheet is on screen for a few
        // seconds and closes on any action, so there is nothing later for an
        // ongoing subscription to usefully update.
        scope.launch { bindWatchlist(library.isInWatchlist(item.id).first()) }
        scope.launch { bindWatched(library.isWatched(item.id).first()) }
    }

    override fun show() {
        super.show()
        binding.actionPlay.post { binding.actionPlay.requestFocus() }
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
        binding.actionWatchlist.setText(
            if (inLibrary) R.string.action_sheet_remove_from_library else R.string.action_sheet_add_to_library,
        )
    }

    private fun bindWatched(watched: Boolean) {
        binding.actionWatched.setText(
            if (watched) R.string.action_unmark_watched else R.string.action_mark_watched,
        )
    }
}
