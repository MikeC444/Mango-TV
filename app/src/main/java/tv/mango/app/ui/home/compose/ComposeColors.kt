package tv.mango.app.ui.home.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import tv.mango.app.theme.MangoColors
import tv.mango.app.theme.RuntimeTheme

/**
 * [MangoColors] as Compose [Color]s, so a viewer's Settings -> Home Screen ->
 * Colours & Accents choice (accent, background, text) still reaches the new
 * Home surface. Every other knob on [tv.mango.app.settings.home.HomeScreenConfig]
 * - glass level, hero rotation, per-row layout style - is out of scope for this
 * pass; NuvioTV's look is a fixed cinematic dark theme, not a configurable one,
 * and reproducing every one of those knobs in Compose is future work.
 */
@Immutable
data class HomeColors(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val focusGlow: Color,
)

private fun MangoColors.toHomeColors() = HomeColors(
    accent = Color(accent),
    background = Color(primaryBackground),
    surface = Color(secondaryBackground),
    primaryText = Color(primaryText),
    secondaryText = Color(secondaryText),
    tertiaryText = Color(tertiaryText),
    focusGlow = Color(focusGlow),
)

@Composable
fun rememberHomeColors(): HomeColors {
    val config by RuntimeTheme.config.collectAsState()
    return remember(config) { RuntimeTheme.colors.toHomeColors() }
}
