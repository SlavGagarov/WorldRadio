package com.example.worldradio.activity.favorites

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

object FavoritesListCache {
    private const val TAG = "WorldRadio.FavoritesListCache"
    private const val PREF_NAME = "StringListCache"
    private const val KEY_STRING_LIST = "string_list"

    fun saveFavoritesList(context: Context, stringList: List<String>) {
        Log.i(TAG, "Saving favorites to cache")
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(stringList)
        editor.putString(KEY_STRING_LIST, json)
        editor.apply()
    }

    fun getFavoritesList(context: Context): List<String> {
        Log.i(TAG, "Getting favorites from cache")
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(KEY_STRING_LIST, null)
        val type = object : TypeToken<List<String>>() {}.type
        return if (json != null) {
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
