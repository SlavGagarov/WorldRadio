package com.example.worldradio.service


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
import com.example.worldradio.MainApplication
import com.example.worldradio.MainApplication.SharedDataHolder.mode
import com.example.worldradio.CacheManager
import com.example.worldradio.R
import com.example.worldradio.WorldRadioConstants
import com.example.worldradio.dto.RadioDetailsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@kotlin.OptIn(DelicateCoroutinesApi::class)

class RadioPlayerService : Service() {
    private val tag = "WorldRadio.RadioPlayerService"

    private lateinit var player: Player
    private lateinit var mediaSession: MediaSession

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    private lateinit var focusRequest: AudioFocusRequest

    private val radioIds = MutableLiveData<List<String>>(emptyList())
    private var radioPosition = 0
    private var currentRadioId = ""
    private var previousRadioId = ""
    private val ignoreInterval: Long = 500
    private var lastEventTime: Long = 0

    private lateinit var context: Context
    private val callbacks = mutableListOf<RadioPlayerCallback>()
    private val notificationId = 555
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RadioPlayerService = this@RadioPlayerService
    }

    interface RadioPlayerCallback {
        fun onRadioChange(radioName: String)
    }

    fun addCallback(callback: RadioPlayerCallback) {
        callbacks.add(callback)
    }

    override fun onCreate() {
        super.onCreate()

        context = applicationContext
        val loadedRadioIds = MainApplication.SharedDataHolder.radioIdsLiveData.value

        if (loadedRadioIds.isNullOrEmpty()) {
            GlobalScope.launch(Dispatchers.Main) {
                val isLoadedSuccessfully = loadRadioIds()
                if (isLoadedSuccessfully) {
                    CacheManager.saveFavoritesList(context, radioIds.value ?: emptyList())
                    getAudioFocus()
                } else {
                    Log.e(tag, "Failed to load favorites from cache")
                }
            }
        } else {
            radioIds.value = loadedRadioIds
            getAudioFocus()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, createNotification())
        return START_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        player.stop()
        player.release()
        mediaSession.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
        super.onDestroy()
    }

    private suspend fun loadRadioIds(): Boolean {
        return withContext(Dispatchers.IO) {
            val inputStream = assets.open("radio-ids.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val mutableList = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                mutableList.add(line!!)
            }
            reader.close()
            radioIds.postValue(mutableList)
            return@withContext true
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(context).build()
        currentRadioId = CacheManager.getCurrentRadio(context)
        if(currentRadioId.isEmpty()) {
            if (radioIds.value?.isNotEmpty() == true) {
                changeRadio(radioIds.value!![0])
            } else {
                Log.w(tag, "radioIds list is empty")
            }
        }
        else {
            val positionInFavorites = radioIds.value?.indexOf(currentRadioId) ?: 0
            radioPosition = positionInFavorites
            changeRadio(currentRadioId)
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSession(context, "WorldRadioMediaSession")

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
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        playNextRadio()
                    }

                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        playPreviousRadio()
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE
                    -> {
                        if(player.isPlaying)
                            player.pause()
                        else
                            player.play()
                    }
                    else -> {
                        Log.i(tag, "No handler configured for key event: " + event.keyCode)
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun changeRadio(id: String) {
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
        CacheManager.saveCurrentRadio(context, currentRadioId)
    }

    fun playNextRadio(){
        when(mode.value.toString()) {
            WorldRadioConstants.FAVORITES_MODE -> {
                nextRadio()
            }
            WorldRadioConstants.RANDOM_MODE -> {
                val mainApplication = application as MainApplication
                CoroutineScope(Dispatchers.Main).launch {
                    previousRadioId = currentRadioId
                    currentRadioId = mainApplication.getRandomRadio()
                    changeRadio(currentRadioId)
                }
            }
            else -> {
                Log.e(tag, "Unknown mode: $mode")
            }
        }
    }

    fun playPreviousRadio(){
        when(mode.value.toString()) {
            WorldRadioConstants.FAVORITES_MODE -> {
                previousRadio()
            }
            WorldRadioConstants.RANDOM_MODE -> {
                val temp = currentRadioId
                currentRadioId = previousRadioId
                previousRadioId = temp
                changeRadio(currentRadioId)
            }
            else -> {
                Log.e(tag, "Unknown mode: $mode")
            }
        }
    }

    fun playRadioById(radioId:String){
        previousRadioId = currentRadioId
        currentRadioId = radioId
        changeRadio(currentRadioId)
    }

    private fun fetchRadioById(id: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(RadioApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val radioApiService = retrofit.create(RadioApiService::class.java)
        val call = radioApiService.getRadio(id)

        call.enqueue(object : Callback<RadioDetailsResponse> {
            override fun onResponse(call: Call<RadioDetailsResponse>, response: Response<RadioDetailsResponse>) {
                if (response.isSuccessful) {
                    val radioResponse = response.body()
                    if (radioResponse != null) {
                        updateRadioName(radioResponse)
                    }
                } else {
                    Log.e(tag, "Failed to fetch radio")
                }
            }

            override fun onFailure(call: Call<RadioDetailsResponse>, t: Throwable) {
                Log.e(tag, "Error fetching radio: ${t.message}")
            }
        })
    }

    fun updateRadioName(radioDetailsResponse: RadioDetailsResponse) {
        val place = radioDetailsResponse.data.country.title + ", " +
                radioDetailsResponse.data.place.title
        val updateText = radioDetailsResponse.data.title + "\n" + place
        for(callback in callbacks){
            callback.onRadioChange(updateText)
        }

        val metadataBuilder = MediaMetadata.Builder()
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, radioDetailsResponse.data.title)
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, place)
        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun getAudioFocus() {
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
        val radioIdsValue = radioIds.value ?: return
        radioPosition = (radioPosition + 1) % radioIdsValue.size
        previousRadioId = currentRadioId
        currentRadioId = radioIdsValue[radioPosition]
        changeRadio(radioIdsValue[radioPosition])
    }

    fun previousRadio() {
        val radioIdsValue = radioIds.value ?: return
        radioPosition = (radioPosition - 1 + radioIdsValue.size) % radioIdsValue.size
        previousRadioId = currentRadioId
        currentRadioId = radioIdsValue[radioPosition]
        changeRadio(radioIdsValue[radioPosition])
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "channel_id",
                "Channel Name",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Radio Service")
            .setContentText("Radio is playing :)")
            .setSmallIcon(R.drawable.ic_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
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
                if(position <= radioPosition){
                    radioPosition --
                }
                radioIds.postValue(mutableList)
                CacheManager.saveFavoritesList(context, mutableList)
            }
        }
    }

    fun addCurrentFavorite(){
        radioIds.value?.toMutableList()?.let { mutableList ->
            if(!mutableList.contains(currentRadioId)) {
                mutableList.add(currentRadioId)
                radioIds.postValue(mutableList)
                CacheManager.saveFavoritesList(context, mutableList)
                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
        }
    }
}