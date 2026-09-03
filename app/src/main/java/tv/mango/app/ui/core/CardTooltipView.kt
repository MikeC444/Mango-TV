package tv.mango.app.ui.core

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import tv.mango.app.R
import tv.mango.app.databinding.ViewCardTooltipBinding
import tv.mango.app.models.MediaItem
import tv.mango.app.utilities.Formatters

/**
 * The floating "more about this" panel a focused card shows below itself.
 *
 * A screen adds exactly one of these to its own root, sized fixed rather than
 * to its content so the panel never visibly resizes as focus moves from a
 * short title to a long one. [CardTooltipController] is what actually decides
 * when it shows and for what; this view only knows how to render one item and
 * park itself under one anchor.
 *
 * A fade only, never a slide or a scale - the row underneath is not moving,
 * and a panel that arrived with its own motion on top of that would read as
 * competing with it rather than commenting on it.
 */
class CardTooltipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCardTooltipBinding.inflate(LayoutInflater.from(context), this)
    private val gap = resources.getDimensionPixelSize(R.dimen.card_tooltip_gap)
    private val edgeMargin = resources.getDimensionPixelSize(R.dimen.safe_area_horizontal)

    init {
        orientation = VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.glass_panel)
        val padding = resources.getDimensionPixelSize(R.dimen.space_2)
        setPadding(padding, padding, padding, padding)
        elevation = resources.getDimension(R.dimen.focus_elevation)
        alpha = 0f

        // Informational only - never a stop on the focus path or a target of
        // its own, and it only ever appears once a card already has focus.
        isFocusable = false
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun show(item: MediaItem, anchor: View) {
        binding.tooltipTitle.text = item.title
        val meta = Formatters.metadataLine(context, item)
        binding.tooltipMeta.text = meta
        binding.tooltipMeta.visibility = if (meta.isBlank()) GONE else VISIBLE

        // Position after this layout pass, once the new text has actually
        // settled the view's height - measuring against the old height here
        // would park a two-line title's panel as if it were still one line.
        post { position(anchor) }

        animate().cancel()
        animate()
            .alpha(1f)
            .setDuration(MotionSpec.DURATION_FAST)
            .setInterpolator(MotionSpec.standard)
            .start()
    }

    fun hide() {
        animate().cancel()
        animate()
            .alpha(0f)
            .setDuration(MotionSpec.DURATION_FAST)
            .setInterpolator(MotionSpec.standard)
            .start()
    }

    /** Below [anchor], centred on it, clamped so it never runs past the screen's safe area. */
    private fun position(anchor: View) {
        val root = parent as? ViewGroup ?: return
        if (!anchor.isAttachedToWindow) return

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val rootLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)

        val anchorLeftInRoot = (anchorLocation[0] - rootLocation[0]).toFloat()
        val anchorTopInRoot = (anchorLocation[1] - rootLocation[1]).toFloat()
        val anchorCenterX = anchorLeftInRoot + anchor.width / 2f

        val maxX = (root.width - width - edgeMargin).toFloat().coerceAtLeast(edgeMargin.toFloat())
        val targetX = (anchorCenterX - width / 2f).coerceIn(edgeMargin.toFloat(), maxX)
        val targetY = anchorTopInRoot + anchor.height + gap

        // Offset from this view's own laid-out position, not from the root's
        // origin directly - translationX/Y are relative to left/top, and left/
        // top is nonzero wherever the root pads its children, the search
        // screen's safe-area padding among them.
        translationX = targetX - left
        translationY = targetY - top
    }
}
