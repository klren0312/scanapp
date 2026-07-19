package com.example.scanapp

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsStandardUserDefaults

actual object SettingsStore {
    private fun defaults(): NSUserDefaults = NSUserDefaultsStandardUserDefaults

    actual fun getString(key: String, default: String): String {
        return defaults().stringForKey(key) ?: default
    }

    actual fun putString(key: String, value: String) {
        defaults().setObject(value, key)
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return if (defaults().objectForKey(key) != null) {
            defaults().boolForKey(key)
        } else {
            default
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults().setBool(value, key)
    }
}
