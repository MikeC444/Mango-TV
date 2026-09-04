package tv.mango.app.ui.core

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import tv.mango.app.R
import tv.mango.app.theme.MangoColors
import tv.mango.app.theme.ThemeDefaults
import tv.mango.app.theme.ThemeDrawables

/**
 * Every `Widget.Mango.Button` in the application - the hero's Play and
 * Details, a detail screen's actions, Add-on setup, the empty-state retry
 * button - is one of these rather than a plain `Button`, so a viewer's accent
 * and glass choices reach every one of them from a single class instead of
 * being wired into each screen that happens to have a button on it.
 *
 * The style (`Widget.Mango.Button`, still applied through the ordinary XML
 * `style=` attribute) supplies the static fallback look and every other
 * property - size, padding, text appearance. Only the background and text
 * colour are then replaced here with theme-derived versions.
 */
class MangoButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr) {

    init {
        applyTheme(ThemeDefaults.colors)
    }

    fun applyTheme(colors: MangoColors) {
        val cornerRadiusPx = resources.getDimension(R.dimen.button_corner)
        background = ThemeDrawables.buttonBackground(colors, ThemeDefaults.glass, cornerRadiusPx)
        setTextColor(
            ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(colors.textOnAccent, colors.primaryText),
            ),
        )
    }
}
