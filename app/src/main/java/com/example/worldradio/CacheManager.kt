package com.example.worldradio

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

object CacheManager {
    private const val TAG = "WorldRadio.CacheManager"
    private const val PREF_NAME = "StringListCache"
    private const val KEY_STRING_LIST = "string_list"
    private const val CURRENT_RADIO = "current_radio"


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

    fun saveCurrentRadio(context: Context, current: String) {
        Log.i(TAG, "Saving current radio to cache")
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(current)
        editor.putString(CURRENT_RADIO, json)
        editor.apply()
    }

    fun getCurrentRadio(context: Context): String {
        Log.i(TAG, "Getting current radio from cache")
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(CURRENT_RADIO, null)
        val type = object : TypeToken<String>() {}.type
        return if (json != null) {
            gson.fromJson(json, type)
        } else {
            ""
        }
    }
}
