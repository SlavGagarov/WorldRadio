package com.example.worldradio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainApplication : Application(){
    private var radioPlayerService: RadioPlayerService? = null

    override fun onCreate() {
        super.onCreate()
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        startService(serviceIntent)
        bindService()
    }

    private fun bindService() {
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioPlayerService.LocalBinder
            radioPlayerService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            radioPlayerService = null
        }
    }

    fun getRadioPlayerService(): RadioPlayerService? {
        return radioPlayerService
    }

    override fun onTerminate() {
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        stopService(serviceIntent)
        unbindService(serviceConnection)
        super.onTerminate()
    }

    fun changeRadio(radioId : String){
        Handler(Looper.getMainLooper()).post {
            radioPlayerService?.changeRadio(radioId)
        }

//        radioPlayerService?.changeRadio(radioId)
    }
}