package com.example.worldradio.activity.explore

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.worldradio.MainApplication
import com.example.worldradio.R
import com.example.worldradio.WorldRadioConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ExploreRadiosActivity : ComponentActivity() {
    private val tag = "WorldRadio.ExploreCitiesActivity"

    private lateinit var filterEditText: EditText
    private lateinit var stringList: RecyclerView
    private lateinit var adapter: CountriesListAdapter
    private var dataList = mutableListOf<String>()


    companion object {
        const val CITY_NAME = "com.example.worldradio.activity.explore.CITY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore_radios)

        val cityName = intent.getStringExtra(CITY_NAME) ?: ""

        dataList.clear()
        val mainApplication = application as MainApplication
        CoroutineScope(Dispatchers.Main).launch {
            dataList = mainApplication.getRadiosForCity(cityName).toMutableList()
            dataList.sort()
            setupListView()
        }
    }

    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Back Button Clicked Clicked")
        val intent = Intent(this@ExploreRadiosActivity, ExploreCitiesActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    fun onAddToFavoritesClicked(view: View) {
        Log.d(tag, "Add to favorites Clicked")
        val application = application as MainApplication
        application.addCurrentToFavorites()
    }

    private fun setupListView() {
        filterEditText = findViewById(R.id.filterEditText)
        stringList = findViewById(R.id.stringList)

        adapter = CountriesListAdapter(dataList, WorldRadioConstants.EXPLORE_RADIOS_ACTIVITY, application as MainApplication)
        stringList.adapter = adapter
        stringList.layoutManager = LinearLayoutManager(this)

        filterEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filteredList = dataList.filter {
                    it.contains(s.toString(), ignoreCase = true)
                }
                adapter.updateList(filteredList)
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })
    }
}