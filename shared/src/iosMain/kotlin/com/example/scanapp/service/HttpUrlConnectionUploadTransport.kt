package com.example.scanapp.service

actual object UploadTransport {
    actual suspend fun postJson(url: String, token: String, body: String): Boolean = false
}
