package com.example.worldradio

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startAudio()
    }

    private fun startAudio() {
        mediaPlayer = MediaPlayer()
        val audioUrl = "http://radio.garden/api/ara/content/listen/gM0FbQlC/channel.mp3"
        val audioUrl2 = "http://radio.garden/api/ara/content/listen/ajJyClv8/channel.mp3"
        mediaPlayer.setDataSource(audioUrl2)
        Thread.sleep(1000)
        mediaPlayer.prepareAsync()
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}