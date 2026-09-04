package tv.mango.app.ui.settings.home

/**
 * One row on a Settings -> Home Screen screen, entirely described by what it
 * shows and what happens on LEFT / RIGHT / SELECT - never by which field of
 * [tv.mango.app.settings.home.HomeScreenConfig] it happens to be bound to.
 * Every screen in this package is built from a `List<SettingsRowSpec>` and
 * nothing else; see [SettingsRowAdapter].
 */
sealed interface SettingsRowSpec {

    val label: String

    /** A value that steps through a fixed set of options with LEFT / RIGHT - an enum, a number, a named colour. */
    data class Cycle(
        override val label: String,
        val valueText: String,
        val onLeft: () -> Unit,
        val onRight: () -> Unit,
    ) : SettingsRowSpec

    /** The two-option special case of [Cycle] - ON or OFF, changed by LEFT, RIGHT or SELECT alike. */
    data class Toggle(
        override val label: String,
        val isOn: Boolean,
        val onToggle: () -> Unit,
    ) : SettingsRowSpec

    /** Opens another screen, or a dialog - a category from the root menu, a row from Catalog Rows, a preset. */
    data class Nav(
        override val label: String,
        val subtitle: String? = null,
        val onSelect: () -> Unit,
    ) : SettingsRowSpec
}
