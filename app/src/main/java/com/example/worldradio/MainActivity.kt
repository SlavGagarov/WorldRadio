package com.example.worldradio

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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity(), RadioPlayerService.RadioPlayerCallback {
    private val tag = "WorldRadio.MainActivity"

    private lateinit var radioNameText: TextView
    private var radioPlayerService: RadioPlayerService? = null

    object RadioIdsHolder {
        val radioIdsLiveData: MutableLiveData<List<String>> = MutableLiveData()
    }

    private var bound = false

    private val serviceObserver = Observer<List<String>> { radioIds ->
        Log.i(tag, "Favorite radio ids changed: " + radioIds.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radioNameText = findViewById(R.id.radioNameText)
        radioPlayerService = (application as MainApplication).getRadioPlayerService()
        RadioIdsHolder.radioIdsLiveData.observe(this, serviceObserver)
    }

    override fun onStart() {
        super.onStart()
        val cachedRadioIds: List<String> = FavoritesListCache.getFavoritesList(this)
        RadioIdsHolder.radioIdsLiveData.value = cachedRadioIds
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
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

    fun onNextRadioButtonClicked(view: View) {
        radioPlayerService?.nextRadio()
    }

    fun onPreviousRadioButtonClicked(view: View) {
        radioPlayerService?.previousRadio()
    }

    fun onFavoritesClicked(view: View) {
        startActivity(Intent(this@MainActivity, FavoritesActivity::class.java))
    }

    fun onRandomRadioClicked(view: View) {
        startActivity(Intent(this@MainActivity, RandomRadioActivity::class.java))
    }
}