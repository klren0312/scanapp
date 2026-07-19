package com.example.scanapp

import android.content.Context
import com.example.scanapp.ActivityHolder

actual object SettingsStore {
    private const val PREF_NAME = "scanapp_settings"

    private fun prefs(): android.content.SharedPreferences {
        val ctx = ActivityHolder.currentActivity
            ?: throw IllegalStateException("ActivityHolder.currentActivity is null; cannot access SettingsStore on Android")
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    actual fun getString(key: String, default: String): String = prefs().getString(key, default) ?: default

    actual fun putString(key: String, value: String) {
        prefs().edit().putString(key, value).apply()
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean = prefs().getBoolean(key, default)

    actual fun putBoolean(key: String, value: Boolean) {
        prefs().edit().putBoolean(key, value).apply()
    }
}
