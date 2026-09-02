package tv.mango.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import tv.mango.app.di.appGraph
import tv.mango.app.models.MediaId
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
    private lateinit var progressId: MediaId

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
        startFraction = intent.getFloatExtra(EXTRA_START_FRACTION, 0f)
        seekPending = startFraction > 0f
        title = intent.getStringExtra(EXTRA_TITLE)

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        findViewById<PlayerView>(R.id.player_view).player = exoPlayer

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
        val exoPlayer = player ?: return
        val duration = exoPlayer.duration
        if (duration == C.TIME_UNSET || duration <= 0) return
        val fraction = (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        lifecycleScope.launch { appGraph.libraryRepository.recordProgress(progressId, fraction) }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        player?.pause()
        if (::progressId.isInitialized) recordProgress()
        super.onPause()
    }

    override fun onDestroy() {
        progressJob?.cancel()
        if (::progressId.isInitialized) recordProgress()
        player?.release()
        player = null
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

        private const val PROGRESS_INTERVAL_MILLIS = 10_000L
        private const val DEFAULT_SUBTITLE_MIME_TYPE = "application/x-subrip"

        fun intent(context: Context, target: PlaybackTarget): Intent {
            fun mimeTypeFor(subtitle: SubtitleResult): String =
                subtitle.format.mimeType ?: DEFAULT_SUBTITLE_MIME_TYPE

            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, target.title)
                putExtra(EXTRA_URL, target.url)
                putExtra(EXTRA_PROGRESS_ID, target.progressId.value)
                putExtra(EXTRA_START_FRACTION, target.startFraction)
                putStringArrayListExtra(EXTRA_SUB_URLS, ArrayList(target.subtitles.map { it.url }))
                putStringArrayListExtra(EXTRA_SUB_LANGUAGES, ArrayList(target.subtitles.map { it.language }))
                putStringArrayListExtra(EXTRA_SUB_LABELS, ArrayList(target.subtitles.map { it.label }))
                putStringArrayListExtra(EXTRA_SUB_MIME_TYPES, ArrayList(target.subtitles.map(::mimeTypeFor)))
            }
        }
    }
}
