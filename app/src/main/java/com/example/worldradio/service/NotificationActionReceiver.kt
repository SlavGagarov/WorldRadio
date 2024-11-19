package com.example.worldradio.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.util.UnstableApi

@UnstableApi
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class NotificationActionReceiver : BroadcastReceiver() {
    private val tag = "WorldRadio.NotificationActionReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val action = intent?.action
            if (action != null) {
                val serviceIntent = Intent(it, RadioPlayerService::class.java).apply {
                    this.action = action
                    Log.i(tag, "NotificationActionReceiver notification pressed: $action")
                }
                it.startService(serviceIntent)
            }
        }
    }
}