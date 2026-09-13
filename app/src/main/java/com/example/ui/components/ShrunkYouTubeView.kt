package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.player.YouTubeAudioPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Inert background YouTubePlayerView that explicitly rejects all touch, key, and focus
 * events. Overriding dispatchTouchEvent, onTouchEvent, and dispatchKeyEvent to return
 * false guarantees it never intercepts user taps, scrolling, or keyboard focus in
 * Compose - same inert-by-construction approach as the old raw WebView subclass, just
 * wrapping the library's view instead of a hand-managed one.
 */
class InertYouTubePlayerView(context: Context) : YouTubePlayerView(context) {
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false
    override fun onHoverEvent(event: MotionEvent?): Boolean = false
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean = false
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean = false
    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean = false
    override fun hasFocus(): Boolean = false
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ShrunkYouTubeView(
    audioPlayer: YouTubeAudioPlayer,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(1.dp)
            .alpha(0.001f)
    ) {
        AndroidView(
            factory = { context ->
                InertYouTubePlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(1, 1)
                    isClickable = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    audioPlayer.attachPlayerView(this)
                }
            },
            modifier = Modifier.size(1.dp)
        )
    }
}
