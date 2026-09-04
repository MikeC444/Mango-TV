package tv.mango.app.navigation

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import tv.mango.app.R
import tv.mango.app.theme.MangoColors
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.ThemeDrawables
import tv.mango.app.ui.core.MotionSpec

/**
 * One destination in the navigation rail.
 *
 * Rail items do not lift or scale the way artwork does. A poster is an object
 * you are choosing between; a destination is a place, and giving it the same
 * physicality would flatten the difference between the two. Focus here is
 * carried by a raised surface behind the item and by the icon and label coming
 * up to full brightness.
 *
 * The current section is marked separately, by an accent bar at the leading
 * edge - a position and a shape, so which section you are in does not depend on
 * telling two colours apart. Its colour is [MangoColors.selectedNavColor]
 * specifically - the one colour role in Settings -> Home Screen -> Colours &
 * Accents named for exactly this.
 */
class NavItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val indicator: View
    private val icon: ImageView
    private val label: TextView

    private var colorActive = RuntimeTheme.colors.primaryText
    private var colorResting = RuntimeTheme.colors.secondaryText

    lateinit var section: Route.Section
        private set

    /** Set when this item is the section currently on screen. */
    var isCurrent: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            indicator.animate().cancel()
            indicator.animate()
                .alpha(if (value) 1f else 0f)
                .setDuration(MotionSpec.DURATION_FAST)
                .setInterpolator(MotionSpec.standard)
                .start()
            refreshTint()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isFocusableInTouchMode = false
        // The icon and label are decoration; focus lands on the item as a whole.
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS

        val padding = resources.getDimensionPixelSize(R.dimen.space_1)
        setPaddingRelative(padding, 0, padding, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }

        LayoutInflater.from(context).inflate(R.layout.view_nav_item, this, true)
        indicator = findViewById(R.id.indicator)
        icon = findViewById(R.id.icon)
        label = findViewById(R.id.label)

        applyTheme(RuntimeTheme.colors)
        background = ThemeDrawables.surfaceFocusBackground(
            RuntimeTheme.colors,
            RuntimeTheme.config.value.glass,
            resources.getDimensionPixelSize(R.dimen.nav_item_corner).toFloat(),
        )
    }

    fun bind(section: Route.Section, iconRes: Int, labelRes: Int) {
        this.section = section
        icon.setImageResource(iconRes)
        label.setText(labelRes)
        // The label carries the name for screen readers even while it is
        // visually collapsed, so the item is never an unlabelled icon.
        contentDescription = context.getString(labelRes)
        refreshTint()
    }

    /** Applies the current theme's colours. Called once at construction, and again if a viewer changes them live. */
    fun applyTheme(colors: MangoColors) {
        colorActive = colors.primaryText
        colorResting = colors.secondaryText
        indicator.background = ThemeDrawables.navIndicator(colors)
        refreshTint()
    }

    /**
     * Fades the label in and out as the rail opens and closes. Alpha only - the
     * item's width never changes, so nothing on the screen has to re-lay out.
     */
    fun setLabelExpanded(expanded: Boolean, animate: Boolean) {
        val target = if (expanded) 1f else 0f
        if (!animate) {
            label.animate().cancel()
            label.alpha = target
            return
        }
        if (label.alpha == target) return
        label.animate().cancel()
        label.animate()
            .alpha(target)
            .setDuration(MotionSpec.DURATION_STANDARD)
            .setInterpolator(MotionSpec.standard)
            .start()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        refreshTint()
    }

    private fun refreshTint() {
        val active = hasFocus() || isCurrent
        val color = if (active) colorActive else colorResting
        label.setTextColor(color)
        icon.setColorFilter(color)
    }
}
