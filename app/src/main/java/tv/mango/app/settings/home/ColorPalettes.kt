package tv.mango.app.settings.home

/**
 * Every colour a viewer can actually pick, as fixed swatches rather than a
 * hex field - a D-pad has no keyboard, and typing a hex code with one was
 * never going to be pleasant. Cycling LEFT/RIGHT through a curated set is the
 * whole interaction.
 */
object ColorPalettes {

    /** [AccentColor]'s own named options. [AccentColor.CUSTOM] has no fixed value here - see [ACCENT_SWATCHES]. */
    fun accentArgb(color: AccentColor): Int? = when (color) {
        AccentColor.DEFAULT -> 0xFFD9A05B.toInt()
        AccentColor.BLUE -> 0xFF5B8DD9.toInt()
        AccentColor.PURPLE -> 0xFFA05BD9.toInt()
        AccentColor.RED -> 0xFFD9635B.toInt()
        AccentColor.GREEN -> 0xFF5FAE71.toInt()
        AccentColor.ORANGE -> 0xFFE0863C.toInt()
        AccentColor.PINK -> 0xFFD95B9B.toInt()
        AccentColor.CYAN -> 0xFF5BC2D9.toInt()
        AccentColor.MONOCHROME -> 0xFFCFCDC8.toInt()
        AccentColor.CUSTOM -> null
    }

    /** Swatches offered once a viewer chooses "Custom", and for every individual colour override below. */
    val ACCENT_SWATCHES: List<Int> = listOf(
        0xFFD9A05B, 0xFF5B8DD9, 0xFFA05BD9, 0xFFD9635B, 0xFF5FAE71, 0xFFE0863C,
        0xFFD95B9B, 0xFF5BC2D9, 0xFFCFCDC8, 0xFFE0C94A, 0xFF7A8CD9, 0xFF4AC7A6,
        0xFFD98AC0, 0xFF8A7AD9, 0xFFC2D95B, 0xFFD95B7A,
    ).map { it.toInt() }

    /** A quiet, neutral set for backgrounds - deliberately narrower and darker than the accent swatches. */
    val SURFACE_SWATCHES: List<Int> = listOf(
        0xFF0B0B0C, 0xFF141416, 0xFF1C1C1F, 0xFF17181C, 0xFF14171A, 0xFF1A1613, 0xFF12151A,
    ).map { it.toInt() }

    /** A quiet, light set for text - a viewer wanting higher contrast reaches for one of these. */
    val TEXT_SWATCHES: List<Int> = listOf(
        0xFFF2EFE9, 0xFFFFFFFF, 0xFFE7E4DE, 0xFFD8D5CF, 0xFFC9C7C2,
    ).map { it.toInt() }
}
