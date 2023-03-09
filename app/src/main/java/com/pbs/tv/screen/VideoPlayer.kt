package com.pbs.tv.screen

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "VideoPlayer"

@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier, onEnded: () -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
  val playerView = remember { AtomicReference<StyledPlayerView>() }
  val exoPlayer = remember(key1 = videoUrl) {
    ExoPlayer.Builder(context).build().apply {
      playWhenReady = true
      setMediaItem(MediaItem.fromUri(videoUrl))
      prepare()

      addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          when (playbackState) {
            Player.STATE_ENDED -> {
              Log.i(TAG, "Current video has ended")
              onEnded()
            }

            Player.STATE_READY -> {
              playerView.get()?.showController()
            }

            else -> {}
          }
        }
      })
    }
  }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .focusTarget()
      .onKeyEvent {
        playerView
          .get()
          ?.showController()
        false
      },
    color = Color.DarkGray,
  ) {
    DisposableEffect(
      key1 = AndroidView(modifier = modifier.fillMaxSize(), factory = {
        StyledPlayerView(context).apply {
          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
          controllerAutoShow = true
          player = exoPlayer
          playerView.set(this)
        }
      }),
      effect = {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { _, event ->
          Log.d(TAG, "Got event: $event")
          when (event) {
            Lifecycle.Event.ON_RESUME -> exoPlayer.play()
            Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
            Lifecycle.Event.ON_STOP -> exoPlayer.stop()
            else -> {}
          }
        }
        lifecycle.addObserver(observer)

        onDispose {
          exoPlayer.release()
          lifecycle.removeObserver(observer)
          playerView.set(null)
        }
      },
    )
  }
}