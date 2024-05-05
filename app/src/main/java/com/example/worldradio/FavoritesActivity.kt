package com.example.worldradio

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
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
class FavoritesActivity : ComponentActivity() {
    private val tag = "WorldRadio.FavoritesActivity"

    private lateinit var adapter: CustomAdapter
    private lateinit var recyclerView: RecyclerView
    private val itemList = mutableListOf<String>()

    // LiveData instance for observing changes
    private var radioIdsLiveData: LiveData<List<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // Set up the RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Create the adapter and set it to the RecyclerView
        adapter = CustomAdapter(itemList) { item ->
            // Remove item from the list when delete button is clicked
            adapter.notifyDataSetChanged()

            // Call deleteFavorite method of RadioPlayerService to delete the item from the list
            val radioPlayerService = (applicationContext as MainApplication).getRadioPlayerService()
            // Assuming you have a deleteFavorite method that accepts the item itself

            val position = itemList.indexOf(item)
            radioPlayerService?.deleteFavorite(position)
            itemList.remove(item)
        }
        recyclerView.adapter = adapter

        // Load itemList only once
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

    private fun removeItem(position: Int) {
        itemList.removeAt(position)
        adapter.notifyItemRemoved(position)
    }

    // Other methods...

    // Extension function to observe LiveData once
    fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: Observer<T>) {
        observe(owner, object : Observer<T> {
            override fun onChanged(t: T) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    }

    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Favorites Clicked")
        val intent = Intent(this@FavoritesActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private suspend fun getFavoritesList(radioIds: List<String>): List<String> = withContext(Dispatchers.IO) {
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

