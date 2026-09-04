package tv.mango.app.ui.settings.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.databinding.FragmentHomeScreenListBinding
import tv.mango.app.di.appGraph
import tv.mango.app.settings.home.AccentColor
import tv.mango.app.settings.home.AccessibilityConfig
import tv.mango.app.settings.home.AnimationLevel
import tv.mango.app.settings.home.BackgroundConfig
import tv.mango.app.settings.home.BackgroundType
import tv.mango.app.settings.home.BlurLevel
import tv.mango.app.settings.home.BorderLevel
import tv.mango.app.settings.home.CardConfig
import tv.mango.app.settings.home.ColorPalettes
import tv.mango.app.settings.home.ColorsConfig
import tv.mango.app.settings.home.ContentWidth
import tv.mango.app.settings.home.CornerRadiusOption
import tv.mango.app.settings.home.FocusEffect
import tv.mango.app.settings.home.FocusVisibility
import tv.mango.app.settings.home.GlassConfig
import tv.mango.app.settings.home.GlassEffectLevel
import tv.mango.app.settings.home.GlowLevel
import tv.mango.app.settings.home.HeroArtworkMode
import tv.mango.app.settings.home.HeroConfig
import tv.mango.app.settings.home.HeroOverlay
import tv.mango.app.settings.home.HeroRotation
import tv.mango.app.settings.home.HeroSize
import tv.mango.app.settings.home.HeroTransition
import tv.mango.app.settings.home.HomeDensity
import tv.mango.app.settings.home.HomeLayoutConfig
import tv.mango.app.settings.home.HomeScreenConfig
import tv.mango.app.settings.home.HomeScreenSettingsSection
import tv.mango.app.settings.home.NavItemId
import tv.mango.app.settings.home.NavStyle
import tv.mango.app.settings.home.NavigationConfig
import tv.mango.app.settings.home.PosterSizeOption
import tv.mango.app.settings.home.RowConfig
import tv.mango.app.settings.home.TextSizeOption
import tv.mango.app.settings.home.TypographyConfig

/**
 * Every Settings -> Home Screen category that is just a list of cycling
 * values - nine of the thirteen items on Settings -> Home Screen's own menu,
 * plus one row's own settings reached from Catalog Rows -> Edit Row - all
 * built from the same [SettingsRowAdapter], reading and writing through
 * [tv.mango.app.repository.HomeScreenConfigRepository] directly.
 *
 * One instance handles exactly one of the two: [section] is set when reached
 * from the main menu, [rowId] when reached as an Edit Row. Never both -
 * [forSection] and [forRow] are the only ways to construct this fragment.
 */
class HomeScreenOptionsFragment : Fragment() {

    private var binding: FragmentHomeScreenListBinding? = null
    private val adapter = SettingsRowAdapter()

    private val section: HomeScreenSettingsSection?
        get() = requireArguments().getString(ARG_SECTION)?.let { HomeScreenSettingsSection.valueOf(it) }

    private val rowId: String? get() = requireArguments().getString(ARG_ROW_ID)
    private val rowTitle: String? get() = requireArguments().getString(ARG_ROW_TITLE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = FragmentHomeScreenListBinding.inflate(inflater, container, false)
        binding = view
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        views.screenTitle.text = rowTitle ?: getString(titleRes(section))
        views.optionsList.layoutManager = LinearLayoutManager(requireContext())
        views.optionsList.itemAnimator = null
        views.optionsList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appGraph.homeScreenConfigRepository.config.collect { config ->
                    val id = rowId
                    val rows = if (id != null) {
                        rowRows(config.rows.configFor(id)) { transform ->
                            update { appGraph.homeScreenConfigRepository.updateRow(id, transform) }
                        }
                    } else {
                        buildSection(config)
                    }
                    adapter.submit(rows)
                    views.optionsList.post {
                        if (views.optionsList.findFocus() == null) views.optionsList.requestFocus()
                    }
                }
            }
        }
    }

    private fun titleRes(section: HomeScreenSettingsSection?): Int = when (section) {
        HomeScreenSettingsSection.LAYOUT -> R.string.home_screen_section_layout
        HomeScreenSettingsSection.CARDS -> R.string.home_screen_section_cards
        HomeScreenSettingsSection.COLORS -> R.string.home_screen_section_colors
        HomeScreenSettingsSection.GLASS -> R.string.home_screen_section_glass
        HomeScreenSettingsSection.HERO -> R.string.home_screen_section_hero
        HomeScreenSettingsSection.BACKGROUND -> R.string.home_screen_section_background
        HomeScreenSettingsSection.NAVIGATION -> R.string.home_screen_section_navigation
        HomeScreenSettingsSection.TYPOGRAPHY -> R.string.home_screen_section_typography
        HomeScreenSettingsSection.ACCESSIBILITY -> R.string.home_screen_section_accessibility
        null -> R.string.home_screen_settings_title
    }

    private fun buildSection(config: HomeScreenConfig): List<SettingsRowSpec> = when (section) {
        HomeScreenSettingsSection.LAYOUT -> layoutRows(config.layout)
        HomeScreenSettingsSection.CARDS -> cardRows(config.cards)
        HomeScreenSettingsSection.COLORS -> colorRows(config.colors)
        HomeScreenSettingsSection.GLASS -> glassRows(config.glass)
        HomeScreenSettingsSection.HERO -> heroRows(config.hero)
        HomeScreenSettingsSection.BACKGROUND -> backgroundRows(config.background)
        HomeScreenSettingsSection.NAVIGATION -> navigationRows(config.navigation)
        HomeScreenSettingsSection.TYPOGRAPHY -> typographyRows(config.typography)
        HomeScreenSettingsSection.ACCESSIBILITY -> accessibilityRows(config.accessibility)
        null -> emptyList()
    }

    // ----------------------------------------------------------------- update

    private fun update(body: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch { body() }
    }

    private fun updateConfig(transform: (HomeScreenConfig) -> HomeScreenConfig) {
        update { appGraph.homeScreenConfigRepository.update(transform) }
    }

    // ------------------------------------------------------------------ rows

    private fun layoutRows(layout: HomeLayoutConfig): List<SettingsRowSpec> {
        fun set(next: HomeLayoutConfig) = updateConfig { it.copy(layout = next) }
        val rows = mutableListOf(
            enumRow(getString(R.string.row_home_density), layout.density, { it.displayName() }) { set(layout.copy(density = it)) },
            enumRow(getString(R.string.row_content_width), layout.contentWidth, { it.displayName() }) { set(layout.copy(contentWidth = it)) },
            enumRow(getString(R.string.row_poster_size), layout.posterSize, { it.displayName() }) { set(layout.copy(posterSize = it)) },
        )
        if (layout.posterSize == PosterSizeOption.CUSTOM) {
            rows += intRow(getString(R.string.row_custom_poster_width), layout.customPosterWidthDp, 80, 220, 10) {
                set(layout.copy(customPosterWidthDp = it))
            }
        }
        return rows
    }

    private fun cardRows(cards: CardConfig): List<SettingsRowSpec> {
        fun set(next: CardConfig) = updateConfig { it.copy(cards = next) }
        return listOf(
            enumRow(getString(R.string.row_corner_radius), cards.cornerRadius, { it.displayName() }) { set(cards.copy(cornerRadius = it)) },
            enumRow(getString(R.string.row_focus_effect), cards.focusEffect, { it.displayName() }) { set(cards.copy(focusEffect = it)) },
            floatRow(getString(R.string.row_focus_scale), cards.focusScale, 1f, 1.25f, 0.02f, { "${"%.2f".format(it)}×" }) {
                set(cards.copy(focusScale = it))
            },
            SettingsRowSpec.Toggle(getString(R.string.row_card_title), cards.showTitle) { set(cards.copy(showTitle = !cards.showTitle)) },
            SettingsRowSpec.Toggle(getString(R.string.row_card_year), cards.showYear) { set(cards.copy(showYear = !cards.showYear)) },
            SettingsRowSpec.Toggle(getString(R.string.row_card_rating), cards.showRating) { set(cards.copy(showRating = !cards.showRating)) },
            SettingsRowSpec.Toggle(getString(R.string.row_card_runtime), cards.showRuntime) { set(cards.copy(showRuntime = !cards.showRuntime)) },
            SettingsRowSpec.Toggle(getString(R.string.row_card_watched), cards.showWatchedStatus) {
                set(cards.copy(showWatchedStatus = !cards.showWatchedStatus))
            },
        )
    }

    private fun colorRows(colors: ColorsConfig): List<SettingsRowSpec> {
        fun set(next: ColorsConfig) = updateConfig { it.copy(colors = next) }
        val swatchOptions: List<Int?> = listOf(null) + ColorPalettes.ACCENT_SWATCHES
        fun swatchName(argb: Int?): String =
            if (argb == null) getString(R.string.value_default) else "Colour ${ColorPalettes.ACCENT_SWATCHES.indexOf(argb) + 1}"

        fun colorRoleRow(label: String, current: Int?, onChange: (Int?) -> Unit) =
            optionsRow(label, swatchOptions, current, ::swatchName, onChange)

        val rows = mutableListOf(
            enumRow(getString(R.string.row_accent_colour), colors.accent, { it.displayName() }) { set(colors.copy(accent = it)) },
        )
        if (colors.accent == AccentColor.CUSTOM) {
            rows += optionsRow(
                getString(R.string.row_custom_colour),
                ColorPalettes.ACCENT_SWATCHES,
                colors.customAccentArgb ?: ColorPalettes.ACCENT_SWATCHES.first(),
                { "Colour ${ColorPalettes.ACCENT_SWATCHES.indexOf(it) + 1}" },
            ) { set(colors.copy(customAccentArgb = it)) }
        }
        rows += colorRoleRow(getString(R.string.row_primary_background), colors.primaryBackgroundArgb) {
            set(colors.copy(primaryBackgroundArgb = it))
        }
        rows += colorRoleRow(getString(R.string.row_secondary_background), colors.secondaryBackgroundArgb) {
            set(colors.copy(secondaryBackgroundArgb = it))
        }
        rows += colorRoleRow(getString(R.string.row_glass_tint), colors.glassTintArgb) { set(colors.copy(glassTintArgb = it)) }
        rows += colorRoleRow(getString(R.string.row_glass_border), colors.glassBorderArgb) { set(colors.copy(glassBorderArgb = it)) }
        rows += colorRoleRow(getString(R.string.row_focus_glow_color), colors.focusGlowArgb) { set(colors.copy(focusGlowArgb = it)) }
        rows += colorRoleRow(getString(R.string.row_primary_text), colors.primaryTextArgb) { set(colors.copy(primaryTextArgb = it)) }
        rows += colorRoleRow(getString(R.string.row_secondary_text), colors.secondaryTextArgb) { set(colors.copy(secondaryTextArgb = it)) }
        rows += colorRoleRow(getString(R.string.row_button_colour), colors.buttonColorArgb) { set(colors.copy(buttonColorArgb = it)) }
        rows += colorRoleRow(getString(R.string.row_selected_nav_colour), colors.selectedNavColorArgb) {
            set(colors.copy(selectedNavColorArgb = it))
        }
        return rows
    }

    private fun glassRows(glass: GlassConfig): List<SettingsRowSpec> {
        fun set(next: GlassConfig) = updateConfig { it.copy(glass = next) }
        return listOf(
            enumRow(getString(R.string.row_glass_effect), glass.effect, { it.displayName() }) { set(glass.copy(effect = it)) },
            floatRow(getString(R.string.row_glass_opacity), glass.opacity, 0.1f, 1f, 0.05f) { set(glass.copy(opacity = it)) },
            enumRow(getString(R.string.row_glass_blur), glass.blur, { it.displayName() }) { set(glass.copy(blur = it)) },
            enumRow(getString(R.string.row_glass_border), glass.border, { it.displayName() }) { set(glass.copy(border = it)) },
            enumRow(getString(R.string.row_glass_glow), glass.glow, { it.displayName() }) { set(glass.copy(glow = it)) },
            enumRow(getString(R.string.row_focus_glow_level), glass.focusGlow, { it.displayName() }) { set(glass.copy(focusGlow = it)) },
            enumRow(getString(R.string.row_corner_radius), glass.cornerRadius, { it.displayName() }) { set(glass.copy(cornerRadius = it)) },
        )
    }

    private fun heroRows(hero: HeroConfig): List<SettingsRowSpec> {
        fun set(next: HeroConfig) = updateConfig { it.copy(hero = next) }
        return listOf(
            SettingsRowSpec.Toggle(getString(R.string.row_hero), hero.enabled) { set(hero.copy(enabled = !hero.enabled)) },
            enumRow(getString(R.string.row_hero_size), hero.size, { it.displayName() }) { set(hero.copy(size = it)) },
            enumRow(getString(R.string.row_hero_artwork), hero.artworkMode, { it.displayName() }) { set(hero.copy(artworkMode = it)) },
            SettingsRowSpec.Toggle(getString(R.string.row_hero_title), hero.showTitle) { set(hero.copy(showTitle = !hero.showTitle)) },
            SettingsRowSpec.Toggle(getString(R.string.row_hero_description), hero.showDescription) {
                set(hero.copy(showDescription = !hero.showDescription))
            },
            SettingsRowSpec.Toggle(getString(R.string.row_hero_metadata), hero.showMetadata) {
                set(hero.copy(showMetadata = !hero.showMetadata))
            },
            SettingsRowSpec.Toggle(getString(R.string.row_hero_play_button), hero.showPlayButton) {
                set(hero.copy(showPlayButton = !hero.showPlayButton))
            },
            SettingsRowSpec.Toggle(getString(R.string.row_hero_secondary_actions), hero.showSecondaryActions) {
                set(hero.copy(showSecondaryActions = !hero.showSecondaryActions))
            },
            enumRow(getString(R.string.row_hero_overlay), hero.overlay, { it.displayName() }) { set(hero.copy(overlay = it)) },
            enumRow(getString(R.string.row_hero_rotation), hero.rotation, { it.displayName() }) { set(hero.copy(rotation = it)) },
            enumRow(getString(R.string.row_transition), hero.transition, { it.displayName() }) { set(hero.copy(transition = it)) },
        )
    }

    private fun backgroundRows(background: BackgroundConfig): List<SettingsRowSpec> {
        fun set(next: BackgroundConfig) = updateConfig { it.copy(background = next) }
        return listOf(
            enumRow(getString(R.string.row_background_type), background.type, { it.displayName() }) { set(background.copy(type = it)) },
            floatRow(getString(R.string.row_brightness), background.brightness, 0.3f, 1.3f, 0.1f) { set(background.copy(brightness = it)) },
            floatRow(getString(R.string.row_opacity), background.opacity, 0.2f, 1f, 0.1f) { set(background.copy(opacity = it)) },
            floatRow(getString(R.string.row_gradient_strength), background.gradientStrength, 0f, 1f, 0.1f) {
                set(background.copy(gradientStrength = it))
            },
            floatRow(getString(R.string.row_artwork_visibility), background.artworkVisibility, 0f, 1f, 0.1f) {
                set(background.copy(artworkVisibility = it))
            },
            floatRow(getString(R.string.row_blur_strength), background.blurStrength, 0f, 1f, 0.1f) {
                set(background.copy(blurStrength = it))
            },
        )
    }

    private fun navigationRows(navigation: NavigationConfig): List<SettingsRowSpec> {
        fun set(next: NavigationConfig) = updateConfig { it.copy(navigation = next) }
        val rows = mutableListOf<SettingsRowSpec>(
            enumRow(getString(R.string.row_navigation_style), navigation.style, { it.displayName() }) { set(navigation.copy(style = it)) },
        )
        NavItemId.entries.forEach { item ->
            val locked = item == NavItemId.HOME || item == NavItemId.SETTINGS
            rows += if (locked) {
                SettingsRowSpec.Nav(label = item.displayName(), subtitle = getString(R.string.value_always_on)) {}
            } else {
                SettingsRowSpec.Toggle(item.displayName(), navigation.isVisible(item)) {
                    val hidden = navigation.hiddenItems.toMutableSet()
                    if (item in hidden) hidden -= item else hidden += item
                    set(navigation.copy(hiddenItems = hidden))
                }
            }
        }
        return rows
    }

    private fun typographyRows(typography: TypographyConfig): List<SettingsRowSpec> {
        fun set(next: TypographyConfig) = updateConfig { it.copy(typography = next) }
        return listOf(
            enumRow(getString(R.string.row_text_size), typography.textSize, { it.displayName() }) { set(typography.copy(textSize = it)) },
            enumRow(getString(R.string.row_title_size), typography.titleSize, { it.displayName() }) { set(typography.copy(titleSize = it)) },
            enumRow(getString(R.string.row_metadata_size), typography.metadataSize, { it.displayName() }) {
                set(typography.copy(metadataSize = it))
            },
        )
    }

    private fun accessibilityRows(accessibility: AccessibilityConfig): List<SettingsRowSpec> {
        fun set(next: AccessibilityConfig) = updateConfig { it.copy(accessibility = next) }
        return listOf(
            enumRow(getString(R.string.row_animation), accessibility.animation, { it.displayName() }) { set(accessibility.copy(animation = it)) },
            enumRow(getString(R.string.row_focus_visibility), accessibility.focusVisibility, { it.displayName() }) {
                set(accessibility.copy(focusVisibility = it))
            },
            SettingsRowSpec.Toggle(getString(R.string.row_high_contrast), accessibility.highContrast) {
                set(accessibility.copy(highContrast = !accessibility.highContrast))
            },
            SettingsRowSpec.Toggle(getString(R.string.row_larger_text), accessibility.largerText) {
                set(accessibility.copy(largerText = !accessibility.largerText))
            },
        )
    }

    private fun rowRows(row: RowConfig, set: ((RowConfig) -> RowConfig) -> Unit): List<SettingsRowSpec> = listOf(
        SettingsRowSpec.Toggle(getString(R.string.row_visible), row.visible) { set { it.copy(visible = !it.visible) } },
        enumRow(getString(R.string.row_layout_style), row.layoutStyle, { it.displayName() }) { newStyle ->
            set { it.copy(layoutStyle = newStyle) }
        },
        optionsRow(
            getString(R.string.row_poster_size),
            listOf(null) + tv.mango.app.settings.home.RowPosterSize.entries,
            row.posterSize,
            { it?.displayName() ?: getString(R.string.value_default) },
        ) { newSize -> set { it.copy(posterSize = newSize) } },
        SettingsRowSpec.Toggle(getString(R.string.row_card_title), row.showTitle) { set { it.copy(showTitle = !it.showTitle) } },
        SettingsRowSpec.Toggle(getString(R.string.row_card_year), row.showYear) { set { it.copy(showYear = !it.showYear) } },
        SettingsRowSpec.Toggle(getString(R.string.row_card_rating), row.showRating) { set { it.copy(showRating = !it.showRating) } },
        SettingsRowSpec.Toggle(getString(R.string.row_card_runtime), row.showRuntime) { set { it.copy(showRuntime = !it.showRuntime) } },
        SettingsRowSpec.Toggle(getString(R.string.row_progress_bar), row.showProgressBar) {
            set { it.copy(showProgressBar = !it.showProgressBar) }
        },
        SettingsRowSpec.Toggle(getString(R.string.row_watched_indicator), row.showWatchedIndicator) {
            set { it.copy(showWatchedIndicator = !it.showWatchedIndicator) }
        },
        enumRow(getString(R.string.row_spacing), row.spacing, { it.displayName() }) { newSpacing -> set { it.copy(spacing = newSpacing) } },
        optionsRow(getString(R.string.row_items_displayed), ITEM_COUNT_OPTIONS, row.itemsDisplayed, { it.toString() }) { newCount ->
            set { it.copy(itemsDisplayed = newCount) }
        },
        SettingsRowSpec.Nav(
            label = getString(R.string.row_custom_title),
            subtitle = row.customTitle ?: getString(R.string.value_default),
        ) {
            TextInputDialog(
                context = requireContext(),
                titleRes = R.string.row_custom_title,
                initialValue = row.customTitle.orEmpty(),
            ) { newTitle ->
                set { it.copy(customTitle = newTitle.takeIf(String::isNotBlank)) }
            }.show()
        },
    )

    companion object {
        private const val ARG_SECTION = "section"
        private const val ARG_ROW_ID = "row_id"
        private const val ARG_ROW_TITLE = "row_title"

        private val ITEM_COUNT_OPTIONS = listOf(6, 8, 10, 12, 15, 20)

        fun forSection(section: HomeScreenSettingsSection): HomeScreenOptionsFragment =
            HomeScreenOptionsFragment().apply {
                arguments = Bundle(1).apply { putString(ARG_SECTION, section.name) }
            }

        fun forRow(rowId: String, rowTitle: String): HomeScreenOptionsFragment =
            HomeScreenOptionsFragment().apply {
                arguments = Bundle(2).apply {
                    putString(ARG_ROW_ID, rowId)
                    putString(ARG_ROW_TITLE, rowTitle)
                }
            }
    }

    override fun onDestroyView() {
        binding?.optionsList?.adapter = null
        binding = null
        super.onDestroyView()
    }
}
