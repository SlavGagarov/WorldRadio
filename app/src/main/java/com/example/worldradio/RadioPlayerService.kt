package com.example.worldradio


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private val tag = "WorldRadio.RadioPlayerService"

    private lateinit var player: Player
    private lateinit var mediaSession: MediaSession

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    private lateinit var focusRequest : AudioFocusRequest

    private val radioIds = MutableLiveData<List<String>>(emptyList()) // LiveData for radio IDs

    private var radioPosition = 0
    private val ignoreInterval: Long = 500
    private var lastEventTime: Long = 0

    private lateinit var context : Context
    private var callback: RadioPlayerCallback? = null
    private val notificationId = 123
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): RadioPlayerService = this@RadioPlayerService
    }

    interface RadioPlayerCallback {
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
        startForeground(notificationId, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        player.stop()
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
            val mutableList = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                mutableList.add(line!!)
            }
            reader.close()
            radioIds.postValue(mutableList)
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(context).build()
        radioIds.observeForever { updatedRadioIds ->
            if (updatedRadioIds.isNotEmpty()) {
                val firstRadioId = updatedRadioIds[radioPosition]
                changeRadio(firstRadioId)
            } else {
                Log.w(tag, "radioIds list is empty")
            }
        }
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
        if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
            val event =
                mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            if (event != null) {
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
        fetchRadioById(id)
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
        val radioIdsValue = radioIds.value ?: return // Check if radioIds has a value
        radioPosition = (radioPosition + 1) % radioIdsValue.size // Use modulo for circular behavior
        changeRadio(radioIdsValue[radioPosition])
    }

    fun previousRadio() {
        val radioIdsValue = radioIds.value ?: return // Check if radioIds has a value
        radioPosition = (radioPosition - 1 + radioIdsValue.size) % radioIdsValue.size // Handle negative modulo
        changeRadio(radioIdsValue[radioPosition])
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Radio Service")
            .setContentText("Radio is playing...")
            .setSmallIcon(R.drawable.ic_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)
        return builder.build()
    }

    fun getRadioIds(): LiveData<List<String>> {
        return radioIds
    }

    fun deleteFavorite(position: Int) {
        radioIds.value?.toMutableList()?.let { mutableList ->
            if (position in 0 until mutableList.size) {
                mutableList.removeAt(position)
                radioIds.postValue(mutableList)
            }
        }
        Log.i(tag, "deleting")
    }
}