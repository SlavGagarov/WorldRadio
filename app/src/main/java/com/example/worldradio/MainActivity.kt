package com.example.worldradio

import android.content.ComponentName
import android.content.Context
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


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity(), RadioPlayerService.RadioPlayerCallback {
    private lateinit var radioNameText: TextView
    private var radioPlayerService: RadioPlayerService? = null
    private var bound = false

    private val serviceObserver = Observer<List<String>> { radioIds ->
        // Handle updates to radioIds here
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radioNameText = findViewById(R.id.radioNameText)

        // Get reference to RadioPlayerService from MainApplication
        radioPlayerService = (application as MainApplication).getRadioPlayerService()

        // Observe LiveData in RadioPlayerService for changes
        radioPlayerService?.getRadioIds()?.observe(this, serviceObserver)
    }

    override fun onStart() {
        super.onStart()
        // Bind to the service
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    // Define a ServiceConnection object
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as RadioPlayerService.LocalBinder
            radioPlayerService = binder.getService()
            bound = true
            // Register callback for updates in RadioPlayerService
            radioPlayerService?.setCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            bound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        radioPlayerService?.getRadioIds()?.removeObserver(serviceObserver)
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
}