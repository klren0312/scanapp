package com.example.scanapp.service

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadServiceTest {

    private val captured = mutableListOf<String>()

    private fun fakeTransport() = object : UploadTransportLike {
        override suspend fun postJson(url: String, token: String, body: String): Boolean {
            captured.add(body)
            return true
        }
    }

    @Test
    fun `enqueue sets payload and enabled false drops`() = runTest {
        val svc = UploadService(fakeTransport())
        val batch = ScanBatch(
            uploaderId = "u1",
            wifi = listOf(WifiSighting("AA:BB", "cafe", -50, 2412, 39.9, 116.4, 1000L)),
            bluetooth = emptyList()
        )
        val payload = svc.buildPayload(batch)
        assertTrue(payload.contains("AA:BB"))
        assertTrue(payload.contains("\"uploaderId\":\"u1\""))
    }

    @Test
    fun `buildPayload drops zero coordinates`() = runTest {
        val svc = UploadService(fakeTransport())
        val batch = ScanBatch(
            uploaderId = "u1",
            wifi = listOf(WifiSighting("AA:BB", "cafe", -50, 2412, 0.0, 0.0, 1000L)),
            bluetooth = emptyList()
        )
        val payload = svc.buildPayload(batch)
        assertEquals("{\"uploaderId\":\"u1\",\"wifi\":[],\"bluetooth\":[]}", payload)
    }

    @Test
    fun `buildPayload drops partial zero coordinates`() = runTest {
        // A sighting with one zero axis (e.g. lat=0, lng!=0) is not a valid GPS fix and must be
        // dropped, otherwise the server-side isInvalidCoord check rejects it.
        val svc = UploadService(fakeTransport())
        val batch = ScanBatch(
            uploaderId = "u1",
            wifi = listOf(
                WifiSighting("AA:BB", "cafe", -50, 2412, 0.0, 116.4, 1000L),
                WifiSighting("CC:DD", "cafe2", -60, 2412, 39.9, 0.0, 1000L)
            ),
            bluetooth = emptyList()
        )
        val payload = svc.buildPayload(batch)
        assertEquals("{\"uploaderId\":\"u1\",\"wifi\":[],\"bluetooth\":[]}", payload)
    }
}
