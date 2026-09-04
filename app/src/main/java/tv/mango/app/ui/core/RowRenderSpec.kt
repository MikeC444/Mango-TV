package tv.mango.app.ui.core

/** How one row's cards should be measured, drawn and captioned. Resolved once per row bind. */
data class RowRenderSpec(
    val cardWidthPx: Int,
    val cardHeightPx: Int,
    val useBackdropArt: Boolean,
    val showTitle: Boolean,
    val showYear: Boolean,
    val showRating: Boolean,
    val showRuntime: Boolean,
    val showProgressBar: Boolean,
    val showWatchedIndicator: Boolean,
) {
    val showsCaption: Boolean get() = showTitle || showYear || showRating || showRuntime
}
