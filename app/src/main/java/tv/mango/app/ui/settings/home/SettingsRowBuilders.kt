package tv.mango.app.ui.settings.home

import kotlin.math.roundToInt

/**
 * Builds a [SettingsRowSpec.Cycle] over a fixed, ordered list of options - the
 * one shape almost every row on a Home Screen settings screen turns out to be,
 * whether the options are an enum's constants, items-per-row (6/8/10/12/15/20)
 * or a swatch palette.
 */
fun <T> optionsRow(
    label: String,
    options: List<T>,
    current: T,
    displayName: (T) -> String,
    onChange: (T) -> Unit,
): SettingsRowSpec.Cycle {
    val index = options.indexOf(current).coerceAtLeast(0)
    val next = options[(index + 1) % options.size]
    val prev = options[(index - 1 + options.size) % options.size]
    return SettingsRowSpec.Cycle(
        label = label,
        valueText = displayName(current),
        onLeft = { onChange(prev) },
        onRight = { onChange(next) },
    )
}

/** [optionsRow], specialised for an enum's own constants - almost every row here. */
inline fun <reified T : Enum<T>> enumRow(
    label: String,
    current: T,
    noinline displayName: (T) -> String,
    crossinline onChange: (T) -> Unit,
): SettingsRowSpec.Cycle = optionsRow(label, enumValues<T>().toList(), current, displayName) { onChange(it) }

/** A stepped whole-number value, in its own unit - Custom poster width, in dp. */
fun intRow(
    label: String,
    current: Int,
    min: Int,
    max: Int,
    step: Int,
    format: (Int) -> String = { "${it}dp" },
    onChange: (Int) -> Unit,
): SettingsRowSpec.Cycle = SettingsRowSpec.Cycle(
    label = label,
    valueText = format(current),
    onLeft = { onChange((current - step).coerceIn(min, max)) },
    onRight = { onChange((current + step).coerceIn(min, max)) },
)

/**
 * A stepped numeric value - Focus Scale, Glass Opacity, Brightness... - shown
 * as a percentage by default and changed by [step] each press.
 */
fun floatRow(
    label: String,
    current: Float,
    min: Float,
    max: Float,
    step: Float,
    format: (Float) -> String = { "${(it * 100).roundToInt()}%" },
    onChange: (Float) -> Unit,
): SettingsRowSpec.Cycle = SettingsRowSpec.Cycle(
    label = label,
    valueText = format(current),
    onLeft = { onChange((current - step).coerceIn(min, max)) },
    onRight = { onChange((current + step).coerceIn(min, max)) },
)
