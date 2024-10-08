package com.example.worldradio.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import com.example.worldradio.MainApplication
import com.example.worldradio.R
import com.example.worldradio.WorldRadioConstants
import com.example.worldradio.activity.explore.ExploreCountriesActivity
import com.example.worldradio.activity.favorites.FavoritesActivity
import com.example.worldradio.service.RadioPlayerService


@UnstableApi
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity(), RadioPlayerService.RadioPlayerCallback {
    private val tag = "WorldRadio.MainActivity"

    private lateinit var radioNameText: TextView
    private var radioPlayerService: RadioPlayerService? = null

    private var bound = false

    private val serviceObserver = Observer<List<String>> { radioIds ->
        Log.i(tag, "Favorite radio ids changed: " + radioIds.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MainApplication.SharedDataHolder.mode.postValue(WorldRadioConstants.FAVORITES_MODE)
        radioNameText = findViewById(R.id.radioNameText)
        radioPlayerService = (application as MainApplication).getRadioPlayerService()
        MainApplication.SharedDataHolder.radioIdsLiveData.observe(this, serviceObserver)
    }

    override fun onResume() {
        MainApplication.SharedDataHolder.mode.postValue(WorldRadioConstants.FAVORITES_MODE)
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as RadioPlayerService.LocalBinder
            radioPlayerService = binder.getService()
            bound = true
            radioPlayerService?.addCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            bound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        radioPlayerService?.getRadioIds()?.removeObserver(serviceObserver)
        (application as MainApplication).onTerminate()
    }

    override fun onRadioChange(radioName: String) {
        runOnUiThread {
            radioNameText.text = radioName
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onNextRadioButtonClicked(view: View) {
        radioPlayerService?.playNextRadio()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPreviousRadioButtonClicked(view: View) {
        radioPlayerService?.playPreviousRadio()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFavoritesClicked(view: View) {
        startActivity(Intent(this@MainActivity, FavoritesActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRandomRadioClicked(view: View) {
        startActivity(Intent(this@MainActivity, RandomRadioActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun onExploreClicked(view: View) {
        startActivity(Intent(this@MainActivity, ExploreCountriesActivity::class.java))
    }
}