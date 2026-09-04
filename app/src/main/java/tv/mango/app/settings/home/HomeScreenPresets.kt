package tv.mango.app.settings.home

/**
 * Ready-made combinations of [HomeScreenConfig]'s fields.
 *
 * A preset only ever replaces the fields on this page - nothing here touches
 * [tv.mango.app.repository.LibraryRepository], [tv.mango.app.addon.AddonRepository]
 * or anything else a viewer has built up. Row visibility, order and per-row
 * settings are the one exception worth calling out: a preset resets them to
 * that preset's own defaults, the same way it resets everything else on this
 * page - a viewer's row customisation is UI state, not library data, and
 * "Custom" is exactly the preset a viewer lands back on the moment they touch
 * any control again.
 */
object HomeScreenPresets {

    fun configFor(preset: HomePreset): HomeScreenConfig = when (preset) {
        HomePreset.DEFAULT -> default()
        HomePreset.CINEMATIC -> cinematic()
        HomePreset.COMPACT -> compact()
        HomePreset.MINIMAL -> minimal()
        HomePreset.LIQUID_GLASS -> liquidGlass()
        // Custom is not a preset to apply - it is the label the current
        // configuration already carries once a viewer changes anything.
        HomePreset.CUSTOM -> HomeScreenConfig.default().copy(preset = HomePreset.CUSTOM)
    }

    private fun default(): HomeScreenConfig = HomeScreenConfig(preset = HomePreset.DEFAULT)

    private fun cinematic(): HomeScreenConfig = HomeScreenConfig(
        preset = HomePreset.CINEMATIC,
        layout = HomeLayoutConfig(
            density = HomeDensity.SPACIOUS,
            contentWidth = ContentWidth.WIDE,
            posterSize = PosterSizeOption.LARGE,
        ),
        cards = CardConfig(
            cornerRadius = CornerRadiusOption.LARGE,
            focusEffect = FocusEffect.SCALE_GLOW,
            focusScale = 1.1f,
        ),
        glass = GlassConfig(
            effect = GlassEffectLevel.HIGH,
            opacity = 0.65f,
            blur = BlurLevel.HIGH,
            border = BorderLevel.STRONG,
            glow = GlowLevel.STRONG,
            focusGlow = GlowLevel.STRONG,
            cornerRadius = CornerRadiusOption.EXTRA_LARGE,
        ),
        hero = HeroConfig(
            size = HeroSize.LARGE,
            overlay = HeroOverlay.STRONG,
            rotation = HeroRotation.SEC_15,
            transition = HeroTransition.CROSSFADE,
        ),
        background = BackgroundConfig(
            type = BackgroundType.CINEMATIC,
            brightness = 0.9f,
            gradientStrength = 0.8f,
            artworkVisibility = 0.5f,
        ),
    )

    private fun compact(): HomeScreenConfig = HomeScreenConfig(
        preset = HomePreset.COMPACT,
        layout = HomeLayoutConfig(
            density = HomeDensity.COMPACT,
            contentWidth = ContentWidth.STANDARD,
            posterSize = PosterSizeOption.SMALL,
        ),
        rows = RowsConfig(
            rows = mapOf(DEFAULT_ROW_KEY_CONTINUE to RowConfig(itemsDisplayed = 20)),
        ),
        cards = CardConfig(
            cornerRadius = CornerRadiusOption.SMALL,
            focusEffect = FocusEffect.SCALE,
            focusScale = 1.05f,
        ),
        glass = GlassConfig(
            effect = GlassEffectLevel.LOW,
            opacity = 0.4f,
            cornerRadius = CornerRadiusOption.MEDIUM,
        ),
        hero = HeroConfig(size = HeroSize.COMPACT),
    )

    private fun minimal(): HomeScreenConfig = HomeScreenConfig(
        preset = HomePreset.MINIMAL,
        layout = HomeLayoutConfig(
            density = HomeDensity.COMPACT,
            posterSize = PosterSizeOption.MEDIUM,
        ),
        cards = CardConfig(
            cornerRadius = CornerRadiusOption.SMALL,
            focusEffect = FocusEffect.SCALE,
        ),
        glass = GlassConfig(
            effect = GlassEffectLevel.OFF,
            opacity = 0.2f,
            border = BorderLevel.OFF,
            glow = GlowLevel.OFF,
            focusGlow = GlowLevel.SUBTLE,
        ),
        hero = HeroConfig(
            size = HeroSize.COMPACT,
            showDescription = false,
            showSecondaryActions = false,
            overlay = HeroOverlay.LIGHT,
        ),
        background = BackgroundConfig(type = BackgroundType.SOLID),
        typography = TypographyConfig(
            textSize = TextSizeOption.SMALL,
            titleSize = TextSizeOption.SMALL,
            metadataSize = TextSizeOption.SMALL,
        ),
    )

    private fun liquidGlass(): HomeScreenConfig = HomeScreenConfig(
        preset = HomePreset.LIQUID_GLASS,
        layout = HomeLayoutConfig(
            density = HomeDensity.SPACIOUS,
            posterSize = PosterSizeOption.LARGE,
        ),
        cards = CardConfig(
            cornerRadius = CornerRadiusOption.EXTRA_LARGE,
            focusEffect = FocusEffect.GLASS_GLOW,
            focusScale = 1.1f,
        ),
        glass = GlassConfig(
            effect = GlassEffectLevel.HIGH,
            opacity = 0.7f,
            blur = BlurLevel.HIGH,
            border = BorderLevel.STRONG,
            glow = GlowLevel.STRONG,
            focusGlow = GlowLevel.STRONG,
            cornerRadius = CornerRadiusOption.EXTRA_LARGE,
        ),
        navigation = NavigationConfig(style = NavStyle.EXPANDED),
    )

    /** Matches [tv.mango.app.ui.home.HomeViewModel]'s synthesised Continue Watching row id. */
    private const val DEFAULT_ROW_KEY_CONTINUE = "continue_watching"
}
