package tv.mango.app.ui.settings.home

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import tv.mango.app.R
import tv.mango.app.theme.RuntimeTheme
import tv.mango.app.theme.ThemeDrawables

/**
 * One row across every Settings -> Home Screen screen: a label, and its
 * current value.
 *
 * A D-pad has no fine positioning and no secondary controls to tab into, so a
 * value here is never a separate focusable target - the row itself is LEFT
 * and RIGHT to change it, SELECT to open or apply it. [onLeft] and [onRight]
 * being null is how a row declares it is not cyclable at all - a pure
 * navigation row, whose only response is the click listener every focusable
 * view already has.
 */
class SettingsOptionRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val label: TextView
    private val value: TextView

    var onLeft: (() -> Unit)? = null
    var onRight: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isFocusableInTouchMode = false
        isClickable = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }

        val paddingH = resources.getDimensionPixelSize(R.dimen.space_3)
        val paddingV = resources.getDimensionPixelSize(R.dimen.space_2)
        setPaddingRelative(paddingH, paddingV, paddingH, paddingV)
        minimumWidth = resources.getDimensionPixelSize(R.dimen.settings_row_min_width)

        LayoutInflater.from(context).inflate(R.layout.view_settings_option_row, this, true)
        label = findViewById(R.id.settings_row_label)
        value = findViewById(R.id.settings_row_value)

        background = ThemeDrawables.surfaceFocusBackground(
            RuntimeTheme.colors,
            RuntimeTheme.config.value.glass,
            resources.getDimension(R.dimen.panel_corner),
        )
    }

    fun bind(label: CharSequence, value: CharSequence?) {
        this.label.text = label
        this.value.text = value
        this.value.visibility = if (value.isNullOrEmpty()) GONE else VISIBLE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> onLeft?.let { it(); true } ?: super.onKeyDown(keyCode, event)
        KeyEvent.KEYCODE_DPAD_RIGHT -> onRight?.let { it(); true } ?: super.onKeyDown(keyCode, event)
        else -> super.onKeyDown(keyCode, event)
    }
}
