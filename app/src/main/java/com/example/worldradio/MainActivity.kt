package com.example.worldradio

import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity() {

    private val ignoreInterval : Long = 500
    private var lastEventTime : Long = 0

    private val tag = "WorldRadio"
    private var radioIds = arrayOf("ajJyClv8", "gM0FbQlC", "I9m2o3ys", "HLMePPFo")
    private var radioPosition = 0

    private lateinit var mediaSession: MediaSession
    private lateinit var player: Player

    private lateinit var logsTextView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializePlayer()
        initializeMediaSession()

        logsTextView = findViewById(R.id.logsTextView)
        logsTextView.text = ""
    }

    override fun onDestroy() {
        player.release()
        mediaSession.release()
        super.onDestroy()
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

    private fun initializeMediaSession() {
        mediaSession = MediaSession(this, "MyMediaSession")

        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEventTime < ignoreInterval) {
                    return false
                }
                lastEventTime = currentTime

                handleMediaEvent(mediaButtonIntent)
                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        })

        val playbackState = PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
            .setState(PlaybackState.STATE_PAUSED, 0, 0f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        mediaSession.isActive = true
    }


    private fun handleMediaEvent(mediaButtonIntent : Intent){
        Log.d(tag, "onMediaButtonEvent triggered")
        val intentAction = mediaButtonIntent.action
        logsTextView.append("\n" + intentAction)

        if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
            val event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            if (event != null) {
                logsTextView.append("\n" + event.keyCode)
                if(event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT){
                    nextRadio()
                }
                else if(event.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS){
                    previousRadio()
                }
            }
        }
    }

    fun onNextRadioButtonClicked(view: View) {
        nextRadio()
    }

    fun onPreviousRadioButtonClicked(view: View){
        previousRadio()
    }

    private fun nextRadio() {
        if(radioPosition == radioIds.size-1)
            radioPosition = 0
        else
            radioPosition ++
        changeRadio(radioIds[radioPosition])
    }

    private fun previousRadio() {
        if(radioPosition == 0)
            radioPosition = radioIds.size-1
        else
            radioPosition --
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
}
