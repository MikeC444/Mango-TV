package tv.mango.app.models

/**
 * A snapshot to carry into [tv.mango.app.repository.LibraryRepository.recordProgress] -
 * the player never keeps a live reference to a [MediaItem]. Everything a
 * Continue Watching card needs to render itself and everything resuming it
 * needs to hand back to playback both come from here.
 */
data class ResumePoint(
    val title: String,
    val type: MediaType,
    val posterKey: String,
    val backdropKey: String,
    val runtimeMinutes: Int?,
    val episodeId: String? = null,
    val episodeSeason: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
)

/** One row of Continue Watching, as the home screen renders it. */
data class ContinueWatchingItem(
    val id: MediaId,
    val type: MediaType,
    val title: String,
    val posterKey: String,
    val backdropKey: String,
    val fraction: Float,
    val episodeId: String?,
    val episodeSeason: Int?,
    val episodeNumber: Int?,
    val episodeTitle: String?,
    val remainingMinutes: Int?,
) {
    /** Null for a film, or a series a provider never gave episode numbers for. */
    val episodeLabel: String?
        get() = if (episodeSeason != null && episodeNumber != null) {
            "S$episodeSeason E$episodeNumber"
        } else {
            null
        }

    /**
     * The card the home screen's Continue Watching row actually shows.
     *
     * Carries its own snapshot of the artwork and title rather than the
     * catalogue's, since resolving an arbitrary episode back through an
     * add-on's catalogue is not something this application assumes any
     * provider can do.
     */
    fun toMediaItem(): MediaItem = MediaItem(
        id = id,
        type = type,
        title = title,
        images = MediaImages(poster = posterKey, backdrop = backdropKey),
        progress = fraction,
        resume = this,
    )
}
