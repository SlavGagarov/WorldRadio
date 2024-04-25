package com.example.worldradio

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private var radioIds = arrayOf("ajJyClv8", "gM0FbQlC", "I9m2o3ys", "HLMePPFo")
    private var radioPosition = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupRadio(radioIds[radioPosition])
        setContentView(R.layout.activity_main)
    }

    fun onStartButtonClicked(view: View) {
        startAudio()
        Toast.makeText(this, "Playing $radioPosition", Toast.LENGTH_SHORT).show()
    }

    private fun startAudio() {
        if(radioPosition == radioIds.size-1)
            radioPosition = 0;
        else
            radioPosition ++;
        changeRadio(radioIds[radioPosition])
    }

    private fun changeRadio(id : String) {
        val audioUrl = "http://radio.garden/api/ara/content/listen/${id}/channel.mp3"
        mediaPlayer.apply {
            stop()
            reset()
            setDataSource(audioUrl)
            prepareAsync()
            setOnPreparedListener {
                it.start()
            }
        }
    }

    private fun setupRadio(id : String) {
        mediaPlayer = MediaPlayer()
        val audioUrl = "http://radio.garden/api/ara/content/listen/${id}/channel.mp3"
        mediaPlayer.setDataSource(audioUrl)
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