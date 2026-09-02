package tv.mango.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tv.mango.app.R
import tv.mango.app.addon.model.SubtitleResult
import tv.mango.app.di.AppGraph
import tv.mango.app.models.MediaId
import tv.mango.app.models.MediaType
import tv.mango.app.models.ResumePoint
import tv.mango.app.ui.player.PlaybackTarget
import tv.mango.app.utilities.Logger

/**
 * Plays exactly one stream, chosen on the screen before this one.
 *
 * A dedicated activity rather than a fragment: a video surface wants the
 * whole window, its own immersive chrome, and a lifecycle that is not tied
 * to a fragment back stack built for browsing screens.
 *
 * The player never learns which add-on produced this stream - it receives a
 * URL and a list of subtitle tracks, exactly what [tv.mango.app.ui.player.PlaybackTarget]
 * carries, with no add-on identity anywhere in this class.
 */
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var seekPending = false
    private var startFraction = 0f
    // Nullable rather than lateinit: MediaId is a value class, and Kotlin does
    // not allow lateinit on one.
    private var progressId: MediaId? = null
    private var resumePoint: ResumePoint? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        hideSystemBars()

        val url = intent.getStringExtra(EXTRA_URL)
        val progressIdValue = intent.getStringExtra(EXTRA_PROGRESS_ID)
        if (url.isNullOrBlank() || progressIdValue.isNullOrBlank()) {
            finish()
            return
        }
        progressId = MediaId(progressIdValue)
        resumePoint = readResumePoint()
        startFraction = intent.getFloatExtra(EXTRA_START_FRACTION, 0f)
        seekPending = startFraction > 0f
        title = intent.getStringExtra(EXTRA_TITLE)

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        val views = findViewById<PlayerView>(R.id.player_view)
        playerView = views
        views.player = exoPlayer
        // A D-pad press only reaches PlayerView's own "show the controls"
        // handling if PlayerView is actually the focused view - nothing else
        // on this screen ever takes focus, so without this the remote would
        // have nothing to press to bring the controls back once they hide.
        // Posted rather than called immediately: a view cannot reliably take
        // focus before its first layout pass has happened.
        views.post { views.requestFocus() }

        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                maybeSeekToStart(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                Logger.e("Playback failed", error)
                Toast.makeText(this@PlayerActivity, R.string.player_error, Toast.LENGTH_LONG).show()
                finish()
            }
        })

        exoPlayer.setMediaItem(buildMediaItem(url))
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()

        startProgressTracking()
    }

    private fun buildMediaItem(url: String): MediaItem {
        val subtitles = subtitleConfigurations()
        return MediaItem.Builder()
            .setUri(url)
            .setSubtitleConfigurations(subtitles)
            .build()
    }

    private fun subtitleConfigurations(): List<MediaItem.SubtitleConfiguration> {
        val urls = intent.getStringArrayListExtra(EXTRA_SUB_URLS).orEmpty()
        val languages = intent.getStringArrayListExtra(EXTRA_SUB_LANGUAGES).orEmpty()
        val labels = intent.getStringArrayListExtra(EXTRA_SUB_LABELS).orEmpty()
        val mimeTypes = intent.getStringArrayListExtra(EXTRA_SUB_MIME_TYPES).orEmpty()

        return urls.indices.map { index ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(urls[index]))
                .setMimeType(mimeTypes.getOrNull(index))
                .setLanguage(languages.getOrNull(index)?.takeIf { it.isNotBlank() })
                .setLabel(labels.getOrNull(index)?.takeIf { it.isNotBlank() })
                .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
    }

    /** Rebuilds the [ResumePoint] a Continue Watching entry needs, from intent extras. */
    private fun readResumePoint(): ResumePoint? {
        val resumeTitle = intent.getStringExtra(EXTRA_RESUME_TITLE) ?: return null
        val typeName = intent.getStringExtra(EXTRA_RESUME_TYPE) ?: return null
        val type = runCatching { MediaType.valueOf(typeName) }.getOrNull() ?: return null
        val posterKey = intent.getStringExtra(EXTRA_RESUME_POSTER) ?: return null
        val backdropKey = intent.getStringExtra(EXTRA_RESUME_BACKDROP) ?: return null
        return ResumePoint(
            title = resumeTitle,
            type = type,
            posterKey = posterKey,
            backdropKey = backdropKey,
            runtimeMinutes = intent.getIntExtra(EXTRA_RESUME_RUNTIME, -1).takeIf { it >= 0 },
            episodeId = intent.getStringExtra(EXTRA_RESUME_EPISODE_ID),
            episodeSeason = intent.getIntExtra(EXTRA_RESUME_EPISODE_SEASON, -1).takeIf { it >= 0 },
            episodeNumber = intent.getIntExtra(EXTRA_RESUME_EPISODE_NUMBER, -1).takeIf { it >= 0 },
            episodeTitle = intent.getStringExtra(EXTRA_RESUME_EPISODE_TITLE),
        )
    }

    /** Runs on every player event until the duration is known, then seeks once and stops checking. */
    private fun maybeSeekToStart(current: Player) {
        if (!seekPending) return
        val duration = current.duration
        if (duration == C.TIME_UNSET || duration <= 0) return
        current.seekTo((duration * startFraction).toLong())
        seekPending = false
    }

    private fun startProgressTracking() {
        progressJob = lifecycleScope.launch {
            while (isActive) {
                delay(PROGRESS_INTERVAL_MILLIS)
                recordProgress()
            }
        }
    }

    private fun recordProgress() {
        val id = progressId ?: return
        val point = resumePoint ?: return
        val exoPlayer = player ?: return
        val duration = exoPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0) return
        val fraction = (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        val library = AppGraph.from(this).libraryRepository
        lifecycleScope.launch { library.recordProgress(id, fraction, point) }
    }

    /**
     * Offers every key event to the player view first.
     *
     * PlayerView's own dispatchKeyEvent is what decides whether a D-pad press
     * should reveal the controls or act on them, but the platform only ever
     * calls it if PlayerView is somewhere in the currently-focused view's
     * ancestor chain. Requesting focus in onCreate covers the common case;
     * this covers the rest - a focus request that silently failed, or focus
     * having moved somewhere unexpected - so the remote is never left with a
     * hidden controller nothing can bring back.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        playerView?.dispatchKeyEvent(event) == true || super.dispatchKeyEvent(event)

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        player?.pause()
        recordProgress()
        super.onPause()
    }

    override fun onDestroy() {
        progressJob?.cancel()
        recordProgress()
        player?.release()
        player = null
        playerView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"
        private const val EXTRA_PROGRESS_ID = "progress_id"
        private const val EXTRA_START_FRACTION = "start_fraction"
        private const val EXTRA_SUB_URLS = "sub_urls"
        private const val EXTRA_SUB_LANGUAGES = "sub_languages"
        private const val EXTRA_SUB_LABELS = "sub_labels"
        private const val EXTRA_SUB_MIME_TYPES = "sub_mime_types"

        private const val EXTRA_RESUME_TITLE = "resume_title"
        private const val EXTRA_RESUME_TYPE = "resume_type"
        private const val EXTRA_RESUME_POSTER = "resume_poster"
        private const val EXTRA_RESUME_BACKDROP = "resume_backdrop"
        private const val EXTRA_RESUME_RUNTIME = "resume_runtime"
        private const val EXTRA_RESUME_EPISODE_ID = "resume_episode_id"
        private const val EXTRA_RESUME_EPISODE_SEASON = "resume_episode_season"
        private const val EXTRA_RESUME_EPISODE_NUMBER = "resume_episode_number"
        private const val EXTRA_RESUME_EPISODE_TITLE = "resume_episode_title"

        private const val PROGRESS_INTERVAL_MILLIS = 10_000L
        private const val DEFAULT_SUBTITLE_MIME_TYPE = "application/x-subrip"

        fun intent(context: Context, target: PlaybackTarget): Intent {
            fun mimeTypeFor(subtitle: SubtitleResult): String =
                subtitle.format.mimeType ?: DEFAULT_SUBTITLE_MIME_TYPE

            val point = target.resumePoint
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, target.title)
                putExtra(EXTRA_URL, target.url)
                putExtra(EXTRA_PROGRESS_ID, target.progressId.value)
                putExtra(EXTRA_START_FRACTION, target.startFraction)
                putStringArrayListExtra(EXTRA_SUB_URLS, ArrayList(target.subtitles.map { it.url }))
                putStringArrayListExtra(EXTRA_SUB_LANGUAGES, ArrayList(target.subtitles.map { it.language }))
                putStringArrayListExtra(EXTRA_SUB_LABELS, ArrayList(target.subtitles.map { it.label }))
                putStringArrayListExtra(EXTRA_SUB_MIME_TYPES, ArrayList(target.subtitles.map(::mimeTypeFor)))
                putExtra(EXTRA_RESUME_TITLE, point.title)
                putExtra(EXTRA_RESUME_TYPE, point.type.name)
                putExtra(EXTRA_RESUME_POSTER, point.posterKey)
                putExtra(EXTRA_RESUME_BACKDROP, point.backdropKey)
                point.runtimeMinutes?.let { putExtra(EXTRA_RESUME_RUNTIME, it) }
                putExtra(EXTRA_RESUME_EPISODE_ID, point.episodeId)
                point.episodeSeason?.let { putExtra(EXTRA_RESUME_EPISODE_SEASON, it) }
                point.episodeNumber?.let { putExtra(EXTRA_RESUME_EPISODE_NUMBER, it) }
                putExtra(EXTRA_RESUME_EPISODE_TITLE, point.episodeTitle)
            }
        }
    }
}
