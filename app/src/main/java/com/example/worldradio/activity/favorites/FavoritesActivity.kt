package com.example.worldradio.activity.favorites

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.worldradio.MainApplication
import com.example.worldradio.R
import com.example.worldradio.activity.MainActivity
import com.example.worldradio.dto.RadioDetailsResponse
import com.example.worldradio.service.RadioApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(UnstableApi::class)

class FavoritesActivity : ComponentActivity() {
    private val tag = "WorldRadio.FavoritesActivity"

    private lateinit var adapter: FavoritesAdapter
    private lateinit var recyclerView: RecyclerView
    private val itemList = mutableListOf<String>()
    private var radioIdsLiveData: LiveData<List<String>>? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FavoritesAdapter(itemList) { item ->
            val radioPlayerService = (applicationContext as MainApplication).getRadioPlayerService()

            val position = itemList.indexOf(item)
            radioPlayerService?.deleteFavorite(position)
            itemList.remove(item)
            adapter.notifyItemRemoved(position)
        }
        recyclerView.adapter = adapter

        val radioPlayerService = (applicationContext as MainApplication).getRadioPlayerService()
        radioIdsLiveData = radioPlayerService?.getRadioIds()
        radioIdsLiveData?.observeOnce(this) { updatedRadioIds ->
            CoroutineScope(Dispatchers.IO).launch {
                itemList.addAll(getFavoritesList(updatedRadioIds))
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: Observer<T>) {
        observe(owner, object : Observer<T> {
            override fun onChanged(value: T) {
                observer.onChanged(value)
                removeObserver(this)
            }
        })
    }


    @Suppress("UNUSED_PARAMETER")
    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Back Button Clicked Clicked")
        val intent = Intent(this@FavoritesActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private suspend fun getFavoritesList(radioIds: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            val radioNames = mutableListOf<String>()

            val retrofit = Retrofit.Builder()
                .baseUrl(RadioApiService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val radioApiService = retrofit.create(RadioApiService::class.java)

            val requests = radioIds.map { id ->
                async {
                    val call = radioApiService.getRadio(id)
                    try {
                        val response = call.execute()
                        if (response.isSuccessful) {
                            val radioResponse = response.body()
                            if (radioResponse != null) {
                                processRadioData(radioResponse)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } catch (e: IOException) {
                        Log.e(tag, "Error when getting radio details data")
                        null
                    }
                }
            }

            val results = requests.awaitAll()
            radioNames.addAll(results.filterNotNull())
            radioNames
        }

    private fun processRadioData(radioDetailsResponse: RadioDetailsResponse): String {
        return radioDetailsResponse.data.title + ", " +
                radioDetailsResponse.data.country.title + ", " +
                radioDetailsResponse.data.place.title
    }
}

