package com.example.worldradio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class FavoritesActivity : ComponentActivity() {
    private val tag = "WorldRadio.FavoritesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_favorite)
    }

    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Favorites Clicked")
        val intent = Intent(this@FavoritesActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
        getFavoritesIds()
    }

    private fun getFavoritesIds(){
        val r = RadioPlayerService.radioIds
        val x = 1;
    }
}

