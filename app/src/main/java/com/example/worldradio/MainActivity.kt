package com.example.worldradio

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

class MainActivity : ComponentActivity() {

    private var radioIds = arrayOf("ajJyClv8", "gM0FbQlC", "I9m2o3ys", "HLMePPFo")
    private var radioPosition = 0

    private lateinit var player: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Use Compose UI if needed
        }
        setContentView(R.layout.activity_main)

        initializePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val mediaItem = MediaItem.Builder()
            .setUri("http://radio.garden/api/ara/content/listen/uPX6WnGn/channel.mp3")
            .build()

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        (player as ExoPlayer).setMediaSource(mediaSource)
        player.playWhenReady = true
        player.prepare()
    }

    private fun releasePlayer() {
        player.release()
    }

    fun onStartButtonClicked(view: View) {
        nextRadio()
        Toast.makeText(this, "Playing $radioPosition", Toast.LENGTH_SHORT).show()
    }

    private fun nextRadio() {
        if(radioPosition == radioIds.size-1)
            radioPosition = 0;
        else
            radioPosition ++;
        changeRadio(radioIds[radioPosition])
    }

    @OptIn(UnstableApi::class)
    private fun changeRadio(id : String) {
        player.stop()
        val audioUrl = "http://radio.garden/api/ara/content/listen/${id}/channel.mp3"

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val mediaItem = MediaItem.Builder()
            .setUri(audioUrl)
            .build()

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        (player as ExoPlayer).setMediaSource(mediaSource)
        player.playWhenReady = true
        player.prepare()
    }
}
