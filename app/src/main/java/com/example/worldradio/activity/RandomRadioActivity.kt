package com.example.worldradio.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.example.worldradio.MainApplication
import com.example.worldradio.R
import com.example.worldradio.WorldRadioConstants
import com.example.worldradio.service.RadioApiService
import com.example.worldradio.service.RadioPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(DelicateCoroutinesApi::class)

class RandomRadioActivity : ComponentActivity(), RadioPlayerService.RadioPlayerCallback {
    private val tag = "WorldRadio.RandomRadioActivity"

    private lateinit var radioNameText: TextView
    private lateinit var radioApiService: RadioApiService
    private var radioPlayerService: RadioPlayerService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_radio)
        radioNameText = findViewById(R.id.radioNameText)

        MainApplication.SharedDataHolder.mode.postValue(WorldRadioConstants.RANDOM_MODE)
        radioPlayerService = (application as MainApplication).getRadioPlayerService()
        radioPlayerService?.addCallback(this@RandomRadioActivity)

        setupApiService()
        getAllPlaces()
    }

    override fun onResume() {
        MainApplication.SharedDataHolder.mode.postValue(WorldRadioConstants.RANDOM_MODE)
        super.onResume()
    }

    fun onNextRadioButtonClicked(view: View) {
        GlobalScope.launch {
            playRandomRadio()
        }
    }

    fun onPreviousRadioButtonClicked(view: View) {
        val mainApplication = application as MainApplication
        mainApplication.playPreviousRadio()
    }

    fun onAddToFavoritesClicked(view: View) {
        radioPlayerService?.addCurrentFavorite()
    }

    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Back Button Clicked Clicked")
        val intent = Intent(this@RandomRadioActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun setupApiService() {
        val retrofit = Retrofit.Builder()
            .baseUrl(RadioApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        radioApiService = retrofit.create(RadioApiService::class.java)
    }

    private fun getAllPlaces() {
        if (!MainApplication.SharedDataHolder.countryCityMap.value.isNullOrEmpty()) {
            MainApplication.SharedDataHolder.allPlacesIds.postValue(
                MainApplication.SharedDataHolder.countryCityMap.value?.values
                    ?.flatten()
                    ?.map { it.cityId }
                    ?: emptyList()
            )
            playRandomRadio()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val call = radioApiService.getAllPlaces()
            try {
                val response = call.execute()
                val mutableList = mutableListOf<String>()
                if (response.isSuccessful) {
                    val placeDetailsResponse = response.body()
                    if (placeDetailsResponse != null) {
                        withContext(Dispatchers.Main) {
                            for (place in placeDetailsResponse.data.list) {
                                mutableList.add(place.id)
                                MainApplication.SharedDataHolder.allPlacesIds.postValue(mutableList)
                            }
                        }
                        playRandomRadio()
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Error when getting place details data", e)
            }
        }
    }

    private fun playRandomRadio(){
        val mainApplication = application as MainApplication
        CoroutineScope(Dispatchers.Main).launch {
            mainApplication.playNextRadio()
        }
    }

    override fun onRadioChange(radioName: String) {
        runOnUiThread {
            radioNameText.text = radioName
        }
    }
}