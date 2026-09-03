package tv.mango.app.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import tv.mango.app.R
import tv.mango.app.ui.core.MotionSpec

/**
 * The navigation rail.
 *
 * At rest it is a narrow column of icons at the edge of the screen. When the
 * viewer moves left into it, labels fade in and a scrim settles behind them.
 *
 * The rail rests at a narrow, reserved width so its bounds never sit on top of
 * the content beside it - which keeps a poster from being covered by an
 * invisible strip of it, and keeps it a valid target when the platform's own
 * focus search looks for something to the left. It only grows to its full
 * width, over the content, on the one event where it gains or loses focus -
 * not on every frame of an animation, the most expensive way to animate
 * anything on Android. The content itself never moves either way.
 */
class NavRail @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scrim: View
    private val items: LinearLayout
    private val navItems = ArrayList<NavItemView>(DESTINATIONS.size)

    private var expanded = false

    /** Invoked when the viewer selects a destination. */
    var onSectionSelected: ((Route.Section) -> Unit)? = null

    /**
     * Focus arrives and leaves in the same dispatch pass, so a listener firing
     * on one item would see a half-settled state. Deferring the check by one
     * frame lets focus land before the rail decides whether it still holds it.
     */
    private val settleFocus = Runnable { setExpanded(hasFocus()) }

    init {
        clipChildren = false

        scrim = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = ContextCompat.getDrawable(context, R.drawable.nav_rail_scrim)
            alpha = 0f
            // Purely decorative, and never in the focus path.
            isFocusable = false
        }
        addView(scrim)

        items = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }
        addView(items)

        addView(buildLogo())

        buildItems()
    }

    /**
     * The brand mark, flush into the screen's own top-left corner rather than
     * inset with the rest of the rail's content - the same corner the rail
     * itself starts from. Living here rather than on the home hero means
     * every section shows it, not just Home.
     */
    private fun buildLogo(): ImageView {
        val size = resources.getDimensionPixelSize(R.dimen.nav_logo_size)
        return ImageView(context).apply {
            layoutParams = LayoutParams(size, size, Gravity.TOP or Gravity.START)
            setImageResource(R.drawable.logo_mango)
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun buildItems() {
        val height = resources.getDimensionPixelSize(R.dimen.nav_item_height)
        val gap = resources.getDimensionPixelSize(R.dimen.space_half)

        DESTINATIONS.forEach { destination ->
            val item = NavItemView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    height,
                ).apply { topMargin = gap }
                bind(destination.section, destination.icon, destination.label)
                setLabelExpanded(expanded = false, animate = false)
                setOnClickListener { onSectionSelected?.invoke(destination.section) }
                setOnFocusChangeListener { _, _ ->
                    removeCallbacks(settleFocus)
                    post(settleFocus)
                }
            }
            navItems += item
            items.addView(item)
        }
    }

    /** Marks which destination is currently on screen. */
    fun setCurrentSection(section: Route.Section) {
        navItems.forEach { it.isCurrent = it.section == section }
    }

    /** Moves focus onto the destination currently on screen. */
    fun focusCurrentSection() {
        navItems.firstOrNull { it.isCurrent }?.requestFocus() ?: navItems.firstOrNull()?.requestFocus()
    }

    private fun setExpanded(value: Boolean) {
        if (expanded == value) return
        expanded = value

        scrim.animate().cancel()
        scrim.animate()
            .alpha(if (value) 1f else 0f)
            .setDuration(MotionSpec.DURATION_STANDARD)
            .setInterpolator(MotionSpec.standard)
            .start()

        navItems.forEach { it.setLabelExpanded(value, animate = true) }

        // The one layout pass this view ever costs: growing over the content
        // while focus is here, and shrinking back to its reserved column the
        // moment it leaves.
        val widthRes = if (value) R.dimen.nav_rail_expanded else R.dimen.nav_rail_collapsed
        layoutParams = layoutParams.apply {
            width = resources.getDimensionPixelSize(widthRes)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(settleFocus)
        super.onDetachedFromWindow()
    }

    private data class Destination(
        val section: Route.Section,
        val icon: Int,
        val label: Int,
    )

    private companion object {
        /**
         * Order is the navigation order. Home first because it is where Back
         * always lands; Settings last because it is visited least.
         */
        val DESTINATIONS = listOf(
            Destination(Route.Home, R.drawable.ic_home, R.string.nav_home),
            Destination(Route.Movies, R.drawable.ic_movies, R.string.nav_movies),
            Destination(Route.Series, R.drawable.ic_series, R.string.nav_series),
            Destination(Route.Search, R.drawable.ic_search, R.string.nav_search),
            Destination(Route.Library, R.drawable.ic_library, R.string.nav_library),
            Destination(Route.Settings, R.drawable.ic_settings, R.string.nav_settings),
        )
    }
}
