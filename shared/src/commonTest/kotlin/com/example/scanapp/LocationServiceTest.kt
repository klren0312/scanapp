package com.example.scanapp

import com.example.scanapp.models.LocationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationServiceTest {

    @Test
    fun testLocationRecordCreation() {
        val record = LocationRecord(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(39.9042, record.latitude)
        assertEquals(116.4074, record.longitude)
        assertEquals(50.0, record.altitude)
        assertEquals(10.0f, record.accuracy)
        assertNotNull(record.timestamp)
    }

    @Test
    fun testLocationRecordWithZeroCoordinates() {
        val record = LocationRecord(
            latitude = 0.0,
            longitude = 0.0,
            altitude = 0.0,
            accuracy = 0.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(0.0, record.latitude)
        assertEquals(0.0, record.longitude)
        assertEquals(0.0, record.altitude)
        assertEquals(0.0f, record.accuracy)
    }

    @Test
    fun testLocationRecordWithNegativeCoordinates() {
        val record = LocationRecord(
            latitude = -33.8688,
            longitude = 151.2093,
            altitude = 58.0,
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(-33.8688, record.latitude)
        assertEquals(151.2093, record.longitude)
    }

    @Test
    fun testLocationStateFlow() = runTest {
        val currentLocation = MutableStateFlow<LocationRecord?>(null)

        assertNull(currentLocation.value)

        val location = LocationRecord(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )
        currentLocation.value = location

        assertNotNull(currentLocation.value)
        assertEquals(39.9042, currentLocation.value?.latitude)
        assertEquals(116.4074, currentLocation.value?.longitude)
    }

    @Test
    fun testLocationAccuracy() {
        val highAccuracy = LocationRecord(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 1.0f,
            timestamp = System.currentTimeMillis()
        )

        val lowAccuracy = LocationRecord(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 100.0f,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(highAccuracy.accuracy < lowAccuracy.accuracy)
    }

    @Test
    fun testLocationTimestamp() {
        val timestamp = System.currentTimeMillis()
        val record = LocationRecord(
            latitude = 39.9042,
            longitude = 116.4074,
            altitude = 50.0,
            accuracy = 10.0f,
            timestamp = timestamp
        )

        assertEquals(timestamp, record.timestamp)
    }
}
