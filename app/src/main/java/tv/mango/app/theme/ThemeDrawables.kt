package tv.mango.app.theme

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import androidx.core.graphics.ColorUtils

/**
 * Builds every "liquid glass" and accent-carrying surface in the application
 * from [MangoColors] and [GlassConfig], in code, rather than the static
 * `@drawable` XML the very first version of these surfaces used.
 *
 * The reason is entirely about what a colour or a glass setting has to reach:
 * a `@color` reference compiled into a drawable resource cannot be changed at
 * runtime, so a viewer's accent and glass choices could never actually apply
 * to a `glass_card.xml` or a `button_background.xml`. Building the same
 * layered look here - a translucent fill, a faint sheen, a hairline border,
 * exactly the construction the original drawables describe in their own
 * comments - keeps the visual language identical while making every one of
 * those layers a function of the current theme.
 *
 * Performance stays where the original design left it: every shape here is a
 * flat, static `GradientDrawable` layered with `LayerDrawable`, never a
 * real-time blur. [GlassConfig.blur] steps a cheap stand-in - the sheen and
 * fill alpha - rather than turning on any actual blur pass, which is exactly
 * what the original glass drawables' own documentation calls out as the
 * affordable choice on this hardware.
 */
object ThemeDrawables {

    /** A large glass surface: the hero panel, a detail screen's info card. */
    fun glassPanel(colors: MangoColors, glass: GlassConfig, cornerRadiusPx: Float): Drawable =
        glassSurface(colors, glass, cornerRadiusPx, focused = false)

    /** The smaller glass surface every row, chip and settings control rests on. */
    fun glassCard(colors: MangoColors, glass: GlassConfig, cornerRadiusPx: Float, focused: Boolean = false): Drawable =
        glassSurface(colors, glass, cornerRadiusPx, focused)

    private fun glassSurface(
        colors: MangoColors,
        glass: GlassConfig,
        cornerRadiusPx: Float,
        focused: Boolean,
    ): Drawable {
        if (glass.effect == GlassEffectLevel.OFF) {
            // The flat, opaque fallback: no translucency, no sheen, nothing for
            // Reduced/Off animation or a low-powered Firestick to pay for.
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(if (focused) colors.secondaryBackground else colors.primaryBackground)
            }
        }

        val levelMultiplier = when (glass.effect) {
            GlassEffectLevel.LOW -> 0.65f
            GlassEffectLevel.MEDIUM -> 1f
            GlassEffectLevel.HIGH -> 1.35f
            GlassEffectLevel.OFF -> 0f
        }
        val fillAlpha = (glass.opacity * levelMultiplier).coerceIn(0.05f, 1f)
        val baseFill = if (focused) colors.glassTintFocused else colors.glassTint
        val fill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(scaleAlpha(baseFill, fillAlpha))
        }

        val blurMultiplier = when (glass.blur) {
            BlurLevel.LOW -> 0.5f
            BlurLevel.MEDIUM -> 1f
            BlurLevel.HIGH -> 1.6f
        }
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(scaleAlpha(colors.glassSheen, blurMultiplier.coerceAtMost(1.6f)), Color.TRANSPARENT),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
        }

        val layers = mutableListOf<Drawable>(fill, sheen)

        val borderColor = if (focused) colors.glassBorderFocused else colors.glassBorder
        val borderWidth = when (glass.border) {
            BorderLevel.OFF -> 0f
            BorderLevel.SUBTLE -> 1f
            BorderLevel.STRONG -> 2f
        }
        if (borderWidth > 0f) {
            layers += GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setStroke(borderWidth.toInt().coerceAtLeast(1), borderColor)
            }
        }

        val glowAlpha = when (glass.glow) {
            GlowLevel.OFF -> 0f
            GlowLevel.SUBTLE -> 0.35f
            GlowLevel.STRONG -> 0.7f
        }
        if (glowAlpha > 0f) {
            layers += GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setStroke(GLOW_STROKE_PX, scaleAlpha(colors.accent, glowAlpha))
            }
        }

        return LayerDrawable(layers.toTypedArray())
    }

    /**
     * The focused-card treatment: a brightness wash and an accent ring, both
     * carried by one drawable's alpha so [tv.mango.app.ui.core.TvCardView] can
     * animate the whole thing with a single `ValueAnimator`. Every call
     * returns a fresh instance - `mutate()` was needed for the same reason
     * against the old static resource, and stays needed here so one card's
     * focus alpha never bleeds into another's.
     */
    fun cardFocusOverlay(
        colors: MangoColors,
        effect: FocusEffect,
        cornerRadiusPx: Float,
        focusVisibility: FocusVisibility = FocusVisibility.NORMAL,
        highContrast: Boolean = false,
    ): Drawable {
        if (effect == FocusEffect.NONE) {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
                setColor(Color.TRANSPARENT)
            }
        }

        // Ensures D-pad focus is always obvious - Settings -> Home Screen ->
        // Accessibility -> Focus Visibility and High Contrast both make the
        // one cue every focused card already carries stronger, rather than
        // adding a second, unrelated one.
        val visibilityMultiplier = when (focusVisibility) {
            FocusVisibility.SUBTLE -> 0.7f
            FocusVisibility.NORMAL -> 1f
            FocusVisibility.STRONG -> 1.5f
        }
        val washAlpha = if (highContrast) 0x55 else 0x33

        val wash = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor((washAlpha shl 24) or 0xFFFFFF)
        }

        // Glass Glow trades the crisp accent ring for a softer, wider halo in
        // the glow colour instead - the same silhouette, a different material.
        val useGlassGlow = effect == FocusEffect.GLASS_GLOW
        val ringColor = if (useGlassGlow) scaleAlpha(colors.focusGlow, 0.8f) else colors.accent
        val dimColor = if (useGlassGlow) scaleAlpha(colors.focusGlow, 0.3f) else colors.accentDim
        val ringWidth = ((if (useGlassGlow) RING_GLOW_PX else RING_PX) * visibilityMultiplier).toInt().coerceAtLeast(1)
        val dimRingWidth = (RING_DIM_PX * visibilityMultiplier).toInt().coerceAtLeast(1)

        val dimRing = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setStroke(dimRingWidth, dimColor)
        }
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setStroke(ringWidth, ringColor)
        }
        return LayerDrawable(arrayOf(wash, dimRing, ring))
    }

    /** The resume indicator along a card's bottom edge - a track plus an accent fill clipped by level. */
    fun cardProgress(colors: MangoColors): Drawable {
        val track = GradientDrawable().apply { setColor(0xFF2A2A2E.toInt()) }
        val fill = android.graphics.drawable.ClipDrawable(
            GradientDrawable().apply { setColor(colors.accent) },
            android.view.Gravity.START,
            android.graphics.drawable.ClipDrawable.HORIZONTAL,
        )
        return LayerDrawable(arrayOf(track, fill))
    }

    /** The accent bar marking the current rail destination. */
    fun navIndicator(colors: MangoColors): Drawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(colors.selectedNavColor)
        cornerRadius = 1f
    }

    /** The scrim behind the navigation rail while it holds focus. */
    fun navRailScrim(colors: MangoColors): Drawable = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(colors.glassTintFocused, Color.TRANSPARENT),
    )

    /** A primary control: a quiet glass pill at rest, solid accent once focused. */
    fun buttonBackground(colors: MangoColors, glass: GlassConfig, cornerRadiusPx: Float): Drawable {
        val focused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(colors.buttonColor)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(StateSet.WILD_CARD, glassCard(colors, glass, cornerRadiusPx))
        }
    }

    /** A row that is not artwork - a settings row, an add-on card, a cast member. */
    fun surfaceFocusBackground(colors: MangoColors, glass: GlassConfig, cornerRadiusPx: Float): Drawable =
        StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), glassCard(colors, glass, cornerRadiusPx, focused = true))
            addState(StateSet.WILD_CARD, glassCard(colors, glass, cornerRadiusPx, focused = false))
        }

    @androidx.annotation.ColorInt
    private fun scaleAlpha(@androidx.annotation.ColorInt color: Int, factor: Float): Int {
        val currentAlpha = Color.alpha(color)
        val scaled = (currentAlpha * factor).toInt().coerceIn(0, 255)
        return ColorUtils.setAlphaComponent(color, scaled)
    }

    /** [CornerRadiusOption] resolved for a card - the tighter of the two scales this maps onto. */
    fun CornerRadiusOption.cardRadiusDp(): Float = when (this) {
        CornerRadiusOption.SMALL -> 6f
        CornerRadiusOption.MEDIUM -> 10f
        CornerRadiusOption.LARGE -> 16f
        CornerRadiusOption.EXTRA_LARGE -> 24f
    }

    /** [CornerRadiusOption] resolved for a glass panel - larger throughout than a card's own scale. */
    fun CornerRadiusOption.panelRadiusDp(): Float = when (this) {
        CornerRadiusOption.SMALL -> 12f
        CornerRadiusOption.MEDIUM -> 18f
        CornerRadiusOption.LARGE -> 28f
        CornerRadiusOption.EXTRA_LARGE -> 36f
    }

    private const val RING_PX = 5
    private const val RING_GLOW_PX = 9
    private const val RING_DIM_PX = 16
    private const val GLOW_STROKE_PX = 10
}
