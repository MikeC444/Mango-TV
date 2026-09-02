package tv.mango.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tv.mango.app.addon.model.StreamResult
import tv.mango.app.addon.model.SubtitleResult
import tv.mango.app.data.DataResult
import tv.mango.app.data.FailureReason
import tv.mango.app.data.provider.StreamProvider
import tv.mango.app.data.provider.SubtitleProvider
import tv.mango.app.models.MediaId
import tv.mango.app.models.ResumePoint
import tv.mango.app.player.PendingPlayback
import tv.mango.app.repository.LibraryRepository

/** What the picker screen shows. */
sealed interface StreamPickerState {
    data object Loading : StreamPickerState
    data class Content(val streams: List<StreamResult>) : StreamPickerState
    data object Empty : StreamPickerState
    data class Error(val reason: FailureReason) : StreamPickerState
}

/** Everything [tv.mango.app.player.PlayerActivity] needs to start playing one chosen stream. */
data class PlaybackTarget(
    val title: String,
    val url: String,
    val subtitles: List<SubtitleResult>,
    val progressId: MediaId,
    val startFraction: Float,
    /** What to hand back to [LibraryRepository.recordProgress] as the player reports progress. */
    val resumePoint: ResumePoint,
)

/**
 * Queries every enabled add-on that can offer a stream for the title or
 * episode Play was pressed on, and ranks what comes back.
 *
 * Never assumes the first stream to answer is the one to play: everything
 * capable is asked at once and the whole ranked list is shown, so the choice
 * of which source to use is the viewer's, not whichever add-on happened to
 * be fastest.
 */
class StreamPickerViewModel(
    private val streamProvider: StreamProvider,
    private val subtitleProvider: SubtitleProvider,
    private val library: LibraryRepository,
) : ViewModel() {

    private val request = PendingPlayback.take()

    /** Resource-level subtitles, resolved once alongside the streams themselves. */
    private var resourceSubtitles: List<SubtitleResult> = emptyList()

    private val _state = MutableStateFlow<StreamPickerState>(StreamPickerState.Loading)
    val state: StateFlow<StreamPickerState> = _state.asStateFlow()

    /** What to show as the screen's heading - null only if this screen was reached without a request. */
    val displayTitle: String? = request?.let(::titleFor)

    /** The title's own backdrop, shown dimmed behind the source list for the same cinematic feel as the rest of the app. */
    val displayBackdrop: String? = request?.item?.images?.backdrop

    init {
        val req = request
        if (req == null) {
            _state.value = StreamPickerState.Error(FailureReason.UNKNOWN)
        } else {
            viewModelScope.launch {
                // Fetched together rather than one after the other: whichever
                // takes longer decides how long the screen waits either way,
                // so there is nothing to gain from asking in sequence.
                val streamsDeferred = async { streamProvider.streams(req.item, req.episode) }
                val subtitlesDeferred = async { subtitleProvider.subtitles(req.item, req.episode) }

                val streamsResult = streamsDeferred.await()
                resourceSubtitles = (subtitlesDeferred.await() as? DataResult.Success)?.value.orEmpty()

                _state.value = when (streamsResult) {
                    is DataResult.Success -> StreamPickerState.Content(streamsResult.value)
                    is DataResult.Failure -> if (streamsResult.reason == FailureReason.NOT_FOUND) {
                        StreamPickerState.Empty
                    } else {
                        StreamPickerState.Error(streamsResult.reason)
                    }
                }
            }
        }
    }

    /** Null for a stream this application cannot play directly - a torrent, or a hand-off elsewhere. */
    suspend fun playbackFor(stream: StreamResult): PlaybackTarget? {
        val req = request ?: return null
        val url = stream.url?.takeIf { stream.isDirectlyPlayable } ?: return null

        // Always the series or film's own id, never an episode's: Continue
        // Watching is one card per title, and picking a different episode of
        // a show already in progress has to move that one card rather than
        // starting a second, unrelated one.
        val progressId = req.item.id
        val startFraction = if (req.startFromBeginning) 0f else library.progressOf(progressId).first()
        val subtitles = (resourceSubtitles + stream.subtitles)
            .distinctBy { Triple(it.providerId, it.language, it.url) }

        return PlaybackTarget(
            title = displayTitle ?: req.item.title,
            url = url,
            subtitles = subtitles,
            progressId = progressId,
            startFraction = startFraction,
            resumePoint = ResumePoint(
                title = req.item.title,
                type = req.item.type,
                posterKey = req.item.images.poster,
                backdropKey = req.item.images.backdrop,
                runtimeMinutes = req.episode?.runtimeMinutes ?: req.item.runtimeMinutes,
                episodeId = req.episode?.id,
                episodeSeason = req.episode?.season,
                episodeNumber = req.episode?.number,
                episodeTitle = req.episode?.title,
            ),
        )
    }

    private fun titleFor(request: PendingPlayback.Request): String {
        val episode = request.episode
        return if (episode != null) {
            "${request.item.title} · S${episode.season}E${episode.number}"
        } else {
            request.item.title
        }
    }
}
