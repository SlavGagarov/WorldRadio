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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ExploreCitiesActivity : ComponentActivity() {
    private val tag = "WorldRadio.ExploreCitiesActivity"

    private lateinit var filterEditText: EditText
    private lateinit var stringList: RecyclerView
    private lateinit var adapter: CountriesListAdapter
    private var dataList = mutableListOf<String>()


    companion object {
        const val COUNTRY_NAME = "com.example.worldradio.activity.explore.COUNTRY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explore_cities)

        val countryName = intent.getStringExtra(COUNTRY_NAME)

        dataList.clear()
        val locationDetailsList =
            MainApplication.SharedDataHolder.countryCityMap.value?.get(countryName) ?: emptyList()
        dataList.addAll(locationDetailsList.map{it.city})
        dataList.sort()
        setupListView()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onBackButtonClicked(view: View) {
        Log.d(tag, "Back Button Clicked Clicked")
        val intent = Intent(this@ExploreCitiesActivity, ExploreCountriesActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun setupListView() {
        filterEditText = findViewById(R.id.filterEditText)
        stringList = findViewById(R.id.stringList)

        adapter = CountriesListAdapter(dataList, WorldRadioConstants.EXPLORE_CITIES_ACTIVITY, application as MainApplication)
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