package com.example.scanapp.service

import com.example.scanapp.SettingsStore

object UploadSettings {
    private const val KEY_URL = "upload_url"
    private const val KEY_TOKEN = "upload_token"
    private const val KEY_ID = "upload_id"
    private const val KEY_ENABLED = "upload_enabled"

    var serverUrl: String
        get() = SettingsStore.getString(KEY_URL, "")
        set(value) = SettingsStore.putString(KEY_URL, value)

    var uploadToken: String
        get() = SettingsStore.getString(KEY_TOKEN, "")
        set(value) = SettingsStore.putString(KEY_TOKEN, value)

    var uploaderId: String
        get() = SettingsStore.getString(KEY_ID, "default")
        set(value) = SettingsStore.putString(KEY_ID, value)

    var uploadEnabled: Boolean
        get() = SettingsStore.getBoolean(KEY_ENABLED, false)
        set(value) = SettingsStore.putBoolean(KEY_ENABLED, value)
}
