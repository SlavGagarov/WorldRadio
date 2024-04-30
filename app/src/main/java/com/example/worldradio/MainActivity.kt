package com.example.worldradio

import android.content.ContentValues.TAG
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
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log


class MainActivity : ComponentActivity() {

    private var radioIds = arrayOf("ajJyClv8", "gM0FbQlC", "I9m2o3ys", "HLMePPFo")
    private var radioPosition = 0
    private lateinit var mediaSession: MediaSession


    private lateinit var player: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Use Compose UI if needed
        }
        setContentView(R.layout.activity_main)

        initializePlayer()
        initializeMediaSession()
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
        Toast.makeText(this, "Playing $radioPosition", Toast.LENGTH_SHORT).show()
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSession(this, "MyMediaSession")
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

        // Set callback for MediaSession
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                Log.d(TAG, "HERE")
                nextRadio()
            }

            override fun onPause() {
                Log.d(TAG, "HERE")
            }

            override fun onSkipToNext() {
                Log.d(TAG, "HERE")
            }

            override fun onSkipToPrevious() {
                Log.d(TAG, "HERE")
            }

            override fun onStop() {
                Log.d(TAG, "HERE")
            }
        })

        // Set initial playback state
        val playbackState = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
            .setState(PlaybackState.STATE_PAUSED, 0, 0f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        // Start the MediaSession
        mediaSession.isActive = true
    }
}
