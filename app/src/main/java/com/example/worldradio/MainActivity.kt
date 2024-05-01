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


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity(), RadioPlayerService.RadioPlayerCallback {
    private val tag = "WorldRadio.MainActivity"
    private lateinit var logsTextView: TextView
    private lateinit var radioNameText: TextView

    private var radioPlayerService: RadioPlayerService? = null
    private var bound = false


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as RadioPlayerService.LocalBinder
            radioPlayerService = binder.getService()
            bound = true
            radioPlayerService?.setCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        logsTextView = findViewById(R.id.logsTextView)
        radioNameText = findViewById(R.id.radioNameText)

        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }

    override fun onLogReceived(log: String) {
        runOnUiThread {
            logsTextView.append("$log\n")
        }
    }

    override fun onRadioChange(radioName: String) {
        runOnUiThread {
            radioNameText.text = radioName
        }
    }

    fun onNextRadioButtonClicked(view: View) {
        Log.d(tag, "Next Radio Clicked")
        radioPlayerService?.nextRadio()
    }

    fun onPreviousRadioButtonClicked(view: View) {
        Log.d(tag, "Previous Radio Clicked")
        radioPlayerService?.previousRadio()
    }
}
