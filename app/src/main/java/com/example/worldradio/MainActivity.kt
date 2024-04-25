package com.example.worldradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MainActivity : ComponentActivity() {

    private lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Use Compose UI if needed
        }

        initializePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(
                ProgressiveMediaSource.Factory(
                    DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)))
                )
            )
            .build()

        val mediaItem = MediaItem.fromUri("http://radio.garden/api/ara/content/listen/ouKzfwsE/channel.mp3")
        exoPlayer.setMediaItem(mediaItem)

        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    private fun releasePlayer() {
        exoPlayer.release()
    }
}
