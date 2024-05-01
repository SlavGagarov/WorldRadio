package com.example.worldradio


import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class RadioPlayerService : Service(){
    private lateinit var player: Player
    private var radioIds = mutableListOf<String>()
    private var radioPosition = 0
    private lateinit var mediaSession: MediaSession
    private val ignoreInterval: Long = 500
    private var lastEventTime: Long = 0
    private val tag = "WorldRadio.RadioPlayerService"
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    private lateinit var focusRequest : AudioFocusRequest
    private lateinit var context : Context
    private var callback: RadioPlayerCallback? = null

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): RadioPlayerService = this@RadioPlayerService
    }

    interface RadioPlayerCallback {
        fun onLogReceived(log: String)
        fun onRadioChange(radioName: String)
    }

    fun setCallback(callback: RadioPlayerCallback) {
        this.callback = callback
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext
        loadRadioIds()
        getAudioFocus()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        player.release()
        mediaSession.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
        super.onDestroy()
    }

    @kotlin.OptIn(DelicateCoroutinesApi::class)
    private fun loadRadioIds() {
        GlobalScope.launch(Dispatchers.IO) {
            val inputStream = assets.open("radio-ids.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                radioIds.add(line!!)
            }
            reader.close()
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(context).build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val firstRadioId = radioIds[radioPosition]
        val mediaItem = MediaItem.Builder()
            .setUri("http://radio.garden/api/ara/content/listen/$firstRadioId/channel.mp3")
            .build()

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        (player as ExoPlayer).setMediaSource(mediaSource)
        player.playWhenReady = true
        player.prepare()
        fetchRadioById(firstRadioId)
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSession(context, "MyMediaSession")

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


    private fun handleMediaEvent(mediaButtonIntent: Intent) {
        Log.d(tag, "onMediaButtonEvent triggered")
        val intentAction = mediaButtonIntent.action
        callback?.onLogReceived("" + intentAction)

        if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
            val event =
                mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            if (event != null) {
                callback?.onLogReceived("" + event.keyCode)
                if (event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                    nextRadio()
                } else if (event.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    previousRadio()
                }
            }
        }
    }


    @OptIn(UnstableApi::class)
    private fun changeRadio(id: String) {
        player.stop()
        val audioUrl = "http://radio.garden/api/ara/content/listen/${id}/channel.mp3"

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaItem = MediaItem.Builder()
            .setUri(audioUrl)
            .build()

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        (player as ExoPlayer).setMediaSource(mediaSource)
        player.playWhenReady = true
        player.prepare()
        val position = radioPosition + 1
        Toast.makeText(context, "Playing $position", Toast.LENGTH_SHORT).show()
    }

    private fun fetchRadioById(id: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://radio.garden/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val radioApiService = retrofit.create(RadioApiService::class.java)
        val call = radioApiService.getRadio(id)

        call.enqueue(object : Callback<RadioResponse> {
            override fun onResponse(call: Call<RadioResponse>, response: Response<RadioResponse>) {
                if (response.isSuccessful) {
                    val radioResponse = response.body()
                    if (radioResponse != null) {
                        updateRadioName(radioResponse)
                    }
                } else {
                    Log.e(tag, "Failed to fetch radio")
                }
            }

            override fun onFailure(call: Call<RadioResponse>, t: Throwable) {
                Log.e(tag, "Error fetching radio: ${t.message}")
            }
        })
    }

    fun updateRadioName(radioResponse: RadioResponse) {
        val place = radioResponse.data.country.title + ", " +
                radioResponse.data.place.title
        val updateText = radioResponse.data.title + "\n" + place
        callback?.onRadioChange(updateText)

        val metadataBuilder = MediaMetadata.Builder()
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, radioResponse.data.title)
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, place)
        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun getAudioFocus(){
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    player.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    //temp audio focus loss
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    //temp audio focus loss, new focus owner doesn't require others to be silent
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    player.play()
                }
            }
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            initializePlayer()
            initializeMediaSession()
        } else {
            Log.e(tag, "Failed to gain audio focus")
        }
    }

    fun nextRadio() {
        if (radioPosition == radioIds.size - 1)
            radioPosition = 0
        else
            radioPosition++
        changeRadio(radioIds[radioPosition])
        fetchRadioById(radioIds[radioPosition])
    }

    fun previousRadio() {
        if (radioPosition == 0)
            radioPosition = radioIds.size - 1
        else
            radioPosition--
        changeRadio(radioIds[radioPosition])
        fetchRadioById(radioIds[radioPosition])
    }
}