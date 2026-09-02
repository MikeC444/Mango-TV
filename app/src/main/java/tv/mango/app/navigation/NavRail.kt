package tv.mango.app.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
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
 * The rail's laid-out width never changes. An expanding rail would push the
 * content across and force a full measure and layout pass on every frame of the
 * animation - the most expensive way to animate anything on Android. Instead
 * the rail is always its full width and simply draws over the content, and the
 * whole open-and-close is two alpha fades. Content never jumps, and the viewer
 * does not lose their place.
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

        buildItems()
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
