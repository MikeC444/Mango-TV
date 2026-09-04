package tv.mango.app.settings.home

/**
 * The nine Settings -> Home Screen categories rendered by the generic
 * [tv.mango.app.ui.settings.home.HomeScreenOptionsFragment] - every category
 * except Catalog Rows, Presets, Preview and Reset, each of which needs its own
 * screen because its interaction is not just a list of cycling values.
 */
enum class HomeScreenSettingsSection {
    LAYOUT,
    CARDS,
    COLORS,
    GLASS,
    HERO,
    BACKGROUND,
    NAVIGATION,
    TYPOGRAPHY,
    ACCESSIBILITY,
}
