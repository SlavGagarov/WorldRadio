package com.example.worldradio

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
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

    private var previousRadioId = ""
    private var currentRadioId = ""
    private val allPlacesIds = mutableListOf<String>()

    private lateinit var radioNameText: TextView
    private lateinit var radioApiService: RadioApiService
    private var radioPlayerService: RadioPlayerService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_radio)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        radioNameText = findViewById(R.id.radioNameText)


        radioPlayerService = (application as MainApplication).getRadioPlayerService()
        radioPlayerService?.addCallback(this@RandomRadioActivity)

        setupApiService()
        getAllPlaces()
    }

    fun onNextRadioButtonClicked(view: View) {
        GlobalScope.launch {
            playRandomRadio()
        }
    }

    fun onPreviousRadioButtonClicked(view: View) {
        val mainApplication = application as MainApplication
        currentRadioId = previousRadioId

        if(previousRadioId.isNotEmpty()) {
            mainApplication.changeRadio(previousRadioId)
        }
    }

    fun onAddToFavoritesClicked(view: View) {
        radioPlayerService?.addFavorite(currentRadioId)
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
        CoroutineScope(Dispatchers.IO).launch {
            val call = radioApiService.getAllPlaces()
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    val placeDetailsResponse = response.body()
                    if (placeDetailsResponse != null) {
                        withContext(Dispatchers.Main) {
                            for (place in placeDetailsResponse.data.list) {
                                allPlacesIds.add(place.id)
                            }
                            playRandomRadio()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Error when getting place details data", e)
            }
        }
    }

    private suspend fun playRandomRadio(){
        val randomPlaceId = allPlacesIds.random()
        val radioIds = getRadiosForPlace(randomPlaceId)

        previousRadioId = currentRadioId
        val randomRadioId = radioIds.random()
        currentRadioId = randomRadioId

        val mainApplication = application as MainApplication
        mainApplication.changeRadio(randomRadioId)
    }

    private suspend fun getRadiosForPlace(placeId:String): List<String> {
        return withContext(Dispatchers.IO) {
            val radioIds = mutableListOf<String>()
            try {
                val call = radioApiService.getPlaceRadios(placeId)
                val response = call.execute()
                if (response.isSuccessful) {
                    val placeRadiosDetailsResponse = response.body()
                    if (placeRadiosDetailsResponse != null) {
                        val localRadioItems = placeRadiosDetailsResponse.data.content.first().items
                        for(radio in localRadioItems){
                            val href = radio.href
                            if (href != null) {
                                val id = href.substringAfterLast("/")
                                radioIds.add(id)
                            } else {
                                Log.e(tag, "href is null for radio: $radio")
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Error when getting place details data", e)
            }
            radioIds
        }
    }

    override fun onRadioChange(radioName: String) {
        runOnUiThread {
            radioNameText.text = radioName
        }
    }
}