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
        var mode:  MutableLiveData<String> = MutableLiveData()
    }

    override fun onCreate() {
        super.onCreate()
        SharedDataHolder.mode.postValue(WorldRadioConstants.FAVORITES_MODE)
        val cachedRadioIds: List<String> = CacheManager.getFavoritesList(this)
        SharedDataHolder.radioIdsLiveData.value = cachedRadioIds
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

    suspend fun getRandomRadio(): String{
        val randomPlaceId = SharedDataHolder.allPlacesIds.value?.random() ?: ""
        val radioIds = getRadiosForPlace(randomPlaceId)
        return radioIds.random()
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

    private fun setupApiService() {
        val retrofit = Retrofit.Builder()
            .baseUrl(RadioApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        radioApiService = retrofit.create(RadioApiService::class.java)
    }
}