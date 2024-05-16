package com.example.worldradio.activity.explore

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.worldradio.MainApplication
import com.example.worldradio.R
import com.example.worldradio.WorldRadioConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class CountriesListAdapter(
    private var dataList: List<String>,
    private val sourceActivity: String
) : RecyclerView.Adapter<CountriesListAdapter.StringViewHolder>() {

    private val tag = "WorldRadio.CountriesListAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_string, parent, false)
        return StringViewHolder(view)
    }

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        val item = dataList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun updateList(newList: List<String>) {
        dataList = newList
        notifyDataSetChanged()
    }

    inner class StringViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textView)

        fun bind(item: String) {
            textView.text = item
            itemView.setOnClickListener {
                when(sourceActivity) {
                    WorldRadioConstants.EXPLORE_COUNTRIES_ACTIVITY -> {
                        MainApplication.SharedDataHolder.currentCountry.postValue(item)
                        val intent = Intent(itemView.context, ExploreCitiesActivity::class.java)
                        intent.putExtra(ExploreCitiesActivity.COUNTRY_NAME, item)
                        itemView.context.startActivity(intent)
                    }
                    WorldRadioConstants.EXPLORE_CITIES_ACTIVITY -> {
                        val intent = Intent(itemView.context, ExploreRadiosActivity::class.java)
                        intent.putExtra(ExploreRadiosActivity.CITY_NAME, item)
                        itemView.context.startActivity(intent)
                    }
                    WorldRadioConstants.EXPLORE_RADIOS_ACTIVITY -> {
                        CoroutineScope(Dispatchers.Main).launch {

                        }
                    }
                    else -> {
                        Log.e(tag, "Unknown source activity: $sourceActivity")
                    }
                }
            }
        }
    }
}