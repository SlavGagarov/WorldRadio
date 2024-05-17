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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.example.worldradio.dto.LocationDetails
import com.example.worldradio.service.RadioApiService
import com.example.worldradio.service.RadioPlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainApplication : Application(){
    private val tag = "WorldRadio.MainApplication"

    private var radioPlayerService: RadioPlayerService? = null
    private lateinit var radioApiService: RadioApiService

    object SharedDataHolder {
        val radioIdsLiveData: MutableLiveData<List<String>> = MutableLiveData()
        val allPlacesIds: MutableLiveData<List<String>> = MutableLiveData()
        val countryCityMap: MutableLiveData<MutableMap<String, MutableList<LocationDetails>>> =
            MutableLiveData()
        val radioNameIdMap: MutableLiveData<MutableMap<String,String>> =  MutableLiveData()
        var mode:  MutableLiveData<String> = MutableLiveData()
        var currentCountry: MutableLiveData<String> = MutableLiveData()
        var selectedRadioName: MutableLiveData<String> = MutableLiveData()
    }

    override fun onCreate() {
        super.onCreate()
        SharedDataHolder.mode.postValue(WorldRadioConstants.FAVORITES_MODE)
        val cachedRadioIds: List<String> = CacheManager.getFavoritesList(this)
        SharedDataHolder.radioIdsLiveData.value = cachedRadioIds
        SharedDataHolder.mode.postValue(WorldRadioConstants.FAVORITES_MODE)
        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        startService(serviceIntent)
        bindService()
        setupApiService()
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
        unbindService(serviceConnection)
        stopService(serviceIntent)
        super.onTerminate()
    }

    fun playNextRadio(){
        Handler(Looper.getMainLooper()).post {
            radioPlayerService?.playNextRadio()
        }
    }

    fun playPreviousRadio(){
        Handler(Looper.getMainLooper()).post {
            radioPlayerService?.playPreviousRadio()
        }
    }

    fun playSelectedRadio(){
        Handler(Looper.getMainLooper()).post {
            val radioId = SharedDataHolder.radioNameIdMap.value?.get(SharedDataHolder.selectedRadioName.value)
            if(radioId != null){
                radioPlayerService?.playRadioById(radioId)
            }
        }
    }

    suspend fun getRandomRadio(): String{
        val randomPlaceId = SharedDataHolder.allPlacesIds.value?.random() ?: ""
        if(getRadiosForPlace(randomPlaceId)){
            return SharedDataHolder.radioNameIdMap.value?.values?.random() ?: ""
        }
        return ""
    }

    fun addCurrentToFavorites() {
        radioPlayerService?.addCurrentFavorite()
    }

    suspend fun getRadiosForCity(cityName: String): List<String> {
        val cities = SharedDataHolder.countryCityMap.value?.get(SharedDataHolder.currentCountry.value)
        val cityDetails =
            cities?.find {
                it.city == cityName
            }
        if(cityDetails != null && getRadiosForPlace(cityDetails.cityId)){
            return SharedDataHolder.radioNameIdMap.value?.keys?.toList() ?: emptyList()
        }
        return emptyList()
    }

    private suspend fun getRadiosForPlace(placeId:String): Boolean {
        return withContext(Dispatchers.IO) {
            val radioMap = HashMap<String, String>()
            try {
                val call = radioApiService.getPlaceRadios(placeId)
                val response = call.execute()
                if (response.isSuccessful) {
                    val placeRadiosDetailsResponse = response.body()
                    if (placeRadiosDetailsResponse != null) {
                        val localRadioItems = placeRadiosDetailsResponse.data.content.first().items
                        for(radio in localRadioItems){
                            val url = radio.page.url
                            val id = url.substringAfterLast("/")
                            radioMap[radio.page.title] = id
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Error when getting place details data", e)
            }
            SharedDataHolder.radioNameIdMap.postValue(radioMap)
            true
        }
    }

    private fun setupApiService() {
        val retrofit = Retrofit.Builder()
            .baseUrl(RadioApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        radioApiService = retrofit.create(RadioApiService::class.java)
    }

    suspend fun populateCountryCityMap(): Boolean {
        if (!SharedDataHolder.countryCityMap.value.isNullOrEmpty()) {
            SharedDataHolder.allPlacesIds.postValue(
                SharedDataHolder.countryCityMap.value!!.keys.toList()
            )
            return true
        }
        return withContext(Dispatchers.IO) {
            val call = radioApiService.getAllPlaces()
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    val placeDetailsResponse = response.body()
                    if (placeDetailsResponse != null) {
                        val mutableMap = mutableMapOf<String, MutableList<LocationDetails>>()
                        for (place in placeDetailsResponse.data.list) {
                            if (mutableMap[place.country].isNullOrEmpty()) {
                                mutableMap[place.country] = mutableListOf(
                                    LocationDetails(
                                        place.country,
                                        place.title,
                                        place.id
                                    )
                                )
                            } else {
                                mutableMap[place.country]!!.add(
                                    LocationDetails(
                                        place.country,
                                        place.title,
                                        place.id
                                    )
                                )
                            }
                        }
                        SharedDataHolder.countryCityMap.postValue(mutableMap)
                        return@withContext true
                    }
                }
                return@withContext false
            } catch (e: IOException) {
                Log.e(tag, "Error when getting place details data", e)
                false
            }
        }
    }
}