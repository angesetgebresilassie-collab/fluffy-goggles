package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.example.data.model.Song
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class AudioSourceType {
    NONE,
    YOUTUBE_IFRAME,
    ITUNES_PREVIEW
}

data class PlayerState(
    val currentSong: Song? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentPositionSec: Float = 0f,
    val durationSec: Float = 0f,
    val sourceType: AudioSourceType = AudioSourceType.NONE,
    val activeYoutubeVideoId: String? = null,
    val errorMessage: String? = null,
    // True only when YouTube's player itself reported a restriction error code
    // (101/150). False when we merely timed out waiting for playback to start,
    // which is equally consistent with a slow connection - so the UI/copy should
    // never claim "restricted" with confidence when this is false.
    val isConfirmedRestriction: Boolean = false,
    val isMuted: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationSec > 0f) (currentPositionSec / durationSec).coerceIn(0f, 1f) else 0f

    val formattedCurrentTime: String
        get() {
            val totalSec = currentPositionSec.toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }

    val formattedDuration: String
        get() {
            val totalSec = durationSec.toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(m, s)
        }
}

/**
 * Plays YouTube audio via the official IFrame Player API - same underlying mechanism
 * and same ToS-compliant approach as before, but now driven through
 * PierfrancescoSoffritti/android-youtube-player instead of a hand-rolled WebView + JS
 * bridge. The library owns the WebView, the IFrame HTML hosting, progress/state
 * callbacks, and error reporting natively, which removes a lot of the fragile custom
 * bridge code (manual origin/baseURL hosting, a hand-written setInterval progress
 * poller, a raw JavascriptInterface) that was a likely source of the flakiness beyond
 * genuine embedding restrictions.
 */
class YouTubeAudioPlayer(val context: Context) {
    companion object {
        // Sentinel passed to onPlaybackErrorListener when the buffering watchdog gives up
        // waiting - NOT a real YouTube error code, so it can never be mistaken for one
        // (YouTube's actual codes are 2, 5, 100, 101, 150).
        const val TIMEOUT_ERROR_CODE = -1
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var playerView: YouTubePlayerView? = null
    private var youTubePlayer: YouTubePlayer? = null
    private var isIframeReady = false
    private var pendingVideoId: String? = null

    // Backup player for iTunes/Deezer-style 30s audio previews when YouTube playback
    // isn't available or fails.
    private var mediaPlayer: MediaPlayer? = null
    private var previewProgressRunnable: Runnable? = null
    private var bufferingWatchdogRunnable: Runnable? = null

    var onPlaybackErrorListener: ((song: Song, failedVideoId: String, errorCode: Int) -> Unit)? = null

    /**
     * Attaches the (invisible, 1dp) YouTubePlayerView created by ShrunkYouTubeView and
     * initializes it with a chromeless (controls=0) IFrame player - same "hidden
     * background audio engine" design as before, just backed by the library's own
     * WebView + IFrame hosting instead of ours.
     */
    fun attachPlayerView(view: YouTubePlayerView) {
        if (this.playerView === view) return
        this.playerView = view

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                mainHandler.post {
                    this@YouTubeAudioPlayer.youTubePlayer = youTubePlayer
                    isIframeReady = true
                    pendingVideoId?.let { videoId ->
                        pendingVideoId = null
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                mainHandler.post {
                    when (state) {
                        PlayerConstants.PlayerState.UNSTARTED, PlayerConstants.PlayerState.VIDEO_CUED -> {
                            // Mirrors the old JS behavior of auto-playing once a video is
                            // cued/unstarted, since we always intend to play immediately.
                            youTubePlayer.play()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                        }
                        PlayerConstants.PlayerState.ENDED -> {
                            stopBufferingWatchdog()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.ENDED)
                        }
                        PlayerConstants.PlayerState.PLAYING -> {
                            stopBufferingWatchdog()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING, errorMessage = null)
                        }
                        PlayerConstants.PlayerState.PAUSED -> {
                            stopBufferingWatchdog()
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                        }
                        PlayerConstants.PlayerState.BUFFERING -> {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                        }
                        PlayerConstants.PlayerState.UNKNOWN -> {}
                    }
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                mainHandler.post {
                    if (_playerState.value.sourceType == AudioSourceType.YOUTUBE_IFRAME) {
                        _playerState.value = _playerState.value.copy(currentPositionSec = second)
                    }
                }
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                mainHandler.post {
                    // Live streams report 0 here, same as the old "not finite" sentinel -
                    // treated as "unknown," keeps whatever duration we already had.
                    if (_playerState.value.sourceType == AudioSourceType.YOUTUBE_IFRAME && duration > 0f) {
                        _playerState.value = _playerState.value.copy(durationSec = duration)
                    }
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                mainHandler.post {
                    stopBufferingWatchdog()
                    val (code, msg, confirmedRestriction) = when (error) {
                        PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST ->
                            Triple(2, "Invalid YouTube video ID parameter", false)
                        PlayerConstants.PlayerError.HTML_5_PLAYER ->
                            Triple(5, "HTML5 player playback error", false)
                        PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                            Triple(100, "Video not found or removed", false)
                        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                            // This is YouTube's own, explicit "the owner disabled embedding"
                            // signal (equivalent to the old raw codes 101/150) - the only
                            // case that's a genuine, confirmed restriction.
                            Triple(101, "Playback restricted by copyright holder", true)
                        else ->
                            Triple(-2, "YouTube player error", false)
                    }

                    val currentSong = _playerState.value.currentSong
                    val activeId = _playerState.value.activeYoutubeVideoId
                    if (currentSong != null && !activeId.isNullOrBlank() && onPlaybackErrorListener != null) {
                        // Allow ViewModel to search for an alternative lyric video
                        onPlaybackErrorListener?.invoke(currentSong, activeId, code)
                    } else {
                        fallbackToPreviewWithReason(
                            currentSong,
                            "$msg. Switched to a preview clip.",
                            isConfirmedRestriction = confirmedRestriction
                        )
                    }
                }
            }
        }

        val options = IFramePlayerOptions.Builder()
            .controls(0)
            .rel(0)
            .ccLoadPolicy(0)
            .ivLoadPolicy(3)
            .build()

        view.initialize(listener, options)
    }

    fun fallbackToPreviewWithReason(
        song: Song?,
        reason: String,
        isConfirmedRestriction: Boolean = false
    ) {
        stopBufferingWatchdog()
        val fallbackPreview = song?.previewUrl
        if (!fallbackPreview.isNullOrBlank()) {
            _playerState.value = _playerState.value.copy(
                errorMessage = reason,
                isConfirmedRestriction = isConfirmedRestriction
            )
            playPreview(fallbackPreview, song)
        } else {
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = reason,
                isConfirmedRestriction = isConfirmedRestriction
            )
        }
    }

    fun playSongWithYouTube(song: Song, videoId: String) {
        stopPreview()
        stopBufferingWatchdog()

        _playerState.value = _playerState.value.copy(
            currentSong = song.copy(youtubeVideoId = videoId),
            status = PlaybackStatus.BUFFERING,
            sourceType = AudioSourceType.YOUTUBE_IFRAME,
            activeYoutubeVideoId = videoId,
            currentPositionSec = 0f,
            durationSec = if (song.durationMs > 0) song.durationMs / 1000f else 0f,
            errorMessage = null
        )

        // Watchdog: if YouTube hangs on buffering for too long, fall back rather than
        // wait forever - see startBufferingWatchdog for the honesty-over-timeout logic.
        startBufferingWatchdog()

        val player = youTubePlayer
        if (isIframeReady && player != null) {
            player.loadVideo(videoId, 0f)
        } else {
            pendingVideoId = videoId
        }
    }

    private fun startBufferingWatchdog() {
        stopBufferingWatchdog()
        val runnable = Runnable {
            val current = _playerState.value
            if (current.sourceType == AudioSourceType.YOUTUBE_IFRAME && current.status == PlaybackStatus.BUFFERING) {
                val currentSong = current.currentSong
                val activeId = current.activeYoutubeVideoId

                // This is a TIMEOUT, not a confirmed restriction - we never got an
                // onError callback from YouTube itself. A slow/unstable connection
                // produces this exact same symptom (page never finishes loading, so
                // playback never starts), so don't tell the user it's "restricted"
                // when we genuinely don't know that. Give an honest, connection-aware
                // message instead.
                val reason = if (!isIframeReady) {
                    // The IFrame API page itself never loaded - almost certainly a
                    // connectivity problem, not anything about this specific video.
                    if (hasUsableNetwork()) {
                        "Taking too long to load over the network. Switched to a preview clip."
                    } else {
                        "No usable internet connection. Switched to a preview clip."
                    }
                } else {
                    // The player loaded but never started - could be a real
                    // restriction YouTube didn't report cleanly, or still just a slow
                    // stream. We can't tell the two apart from a timeout alone.
                    "This track didn't start playing in time. Switched to a preview clip."
                }

                if (currentSong != null && !activeId.isNullOrBlank() && onPlaybackErrorListener != null) {
                    onPlaybackErrorListener?.invoke(currentSong, activeId, TIMEOUT_ERROR_CODE)
                } else {
                    fallbackToPreviewWithReason(currentSong, reason, isConfirmedRestriction = false)
                }
            }
        }
        bufferingWatchdogRunnable = runnable
        // 15s was too aggressive - on a slow connection that's not even enough time
        // to finish loading the IFrame page, let alone start buffering a stream. 28s
        // gives real slow-network cases a fair shot before we give up.
        mainHandler.postDelayed(runnable, 28000)
    }

    private fun hasUsableNetwork(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true // can't check - don't block the message logic on this
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: SecurityException) {
            true // missing ACCESS_NETWORK_STATE permission - don't guess, just don't block
        }
    }

    private fun stopBufferingWatchdog() {
        bufferingWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        bufferingWatchdogRunnable = null
    }

    fun playPreview(previewUrl: String, song: Song? = null) {
        stopBufferingWatchdog()
        stopYouTube()
        stopPreview()

        val targetSong = song ?: _playerState.value.currentSong
        _playerState.value = _playerState.value.copy(
            currentSong = targetSong,
            status = PlaybackStatus.BUFFERING,
            sourceType = AudioSourceType.ITUNES_PREVIEW,
            currentPositionSec = 0f,
            durationSec = 30f,
            errorMessage = null
        )

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(previewUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    val durSec = mp.duration / 1000f
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.PLAYING,
                        durationSec = if (durSec > 0) durSec else 30f
                    )
                    startPreviewProgress()
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ENDED,
                        currentPositionSec = _playerState.value.durationSec
                    )
                    stopPreviewProgress()
                }
                setOnErrorListener { _, what, extra ->
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "Audio preview error: $what/$extra"
                    )
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "Failed to load preview: ${e.localizedMessage}"
            )
        }
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.status == PlaybackStatus.PLAYING) {
            pause()
        } else if (current.status == PlaybackStatus.PAUSED || current.status == PlaybackStatus.ENDED) {
            resume()
        }
    }

    fun resume() {
        when (_playerState.value.sourceType) {
            AudioSourceType.YOUTUBE_IFRAME -> {
                youTubePlayer?.play()
                _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING)
            }
            AudioSourceType.ITUNES_PREVIEW -> {
                mediaPlayer?.let {
                    it.start()
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING)
                    startPreviewProgress()
                }
            }
            AudioSourceType.NONE -> {}
        }
    }

    fun pause() {
        when (_playerState.value.sourceType) {
            AudioSourceType.YOUTUBE_IFRAME -> {
                youTubePlayer?.pause()
                _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
            }
            AudioSourceType.ITUNES_PREVIEW -> {
                mediaPlayer?.let {
                    if (it.isPlaying) it.pause()
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    stopPreviewProgress()
                }
            }
            AudioSourceType.NONE -> {}
        }
    }

    fun seekTo(seconds: Float) {
        when (_playerState.value.sourceType) {
            AudioSourceType.YOUTUBE_IFRAME -> {
                youTubePlayer?.seekTo(seconds)
                _playerState.value = _playerState.value.copy(currentPositionSec = seconds)
            }
            AudioSourceType.ITUNES_PREVIEW -> {
                mediaPlayer?.let {
                    val ms = (seconds * 1000).toInt()
                    it.seekTo(ms)
                    _playerState.value = _playerState.value.copy(currentPositionSec = seconds)
                }
            }
            AudioSourceType.NONE -> {}
        }
    }

    private fun stopYouTube() {
        stopBufferingWatchdog()
        youTubePlayer?.pause()
    }

    private fun stopPreview() {
        stopPreviewProgress()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
        }
    }

    private fun startPreviewProgress() {
        stopPreviewProgress()
        val runnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        val pos = mp.currentPosition / 1000f
                        _playerState.value = _playerState.value.copy(currentPositionSec = pos)
                        mainHandler.postDelayed(this, 500)
                    }
                }
            }
        }
        previewProgressRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopPreviewProgress() {
        previewProgressRunnable?.let { mainHandler.removeCallbacks(it) }
        previewProgressRunnable = null
    }

    fun release() {
        stopBufferingWatchdog()
        stopPreview()
        stopYouTube()
        playerView?.release()
        playerView = null
        youTubePlayer = null
    }
}
