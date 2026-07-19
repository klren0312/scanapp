package com.example.scanapp.service

expect object UploadTransport {
    suspend fun postJson(url: String, token: String, body: String): Boolean
}
