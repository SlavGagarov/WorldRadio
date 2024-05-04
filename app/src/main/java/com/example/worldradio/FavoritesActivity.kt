package com.example.worldradio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(DelicateCoroutinesApi::class)
class FavoritesActivity : ComponentActivity() {
    private val tag = "WorldRadio.FavoritesActivity"

    private lateinit var adapter: CustomAdapter
    private val itemList = mutableListOf<String>()
    private lateinit var recyclerView: RecyclerView
    private var radioIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        GlobalScope.launch(Dispatchers.Main) {
            itemList.addAll(getFavoritesList())
            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = CustomAdapter(itemList)
            recyclerView.adapter = adapter
        }
    }

    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Favorites Clicked")
        val intent = Intent(this@FavoritesActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private suspend fun getFavoritesList(): List<String> = withContext(Dispatchers.IO) {
        radioIds = RadioPlayerService.radioIds
        val radioNames = mutableListOf<String>()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://radio.garden/api/")
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

    private fun processRadioData(radioResponse: RadioResponse): String {
        return radioResponse.data.title + ", " +
                radioResponse.data.country.title + ", " +
                radioResponse.data.place.title;
    }

}

