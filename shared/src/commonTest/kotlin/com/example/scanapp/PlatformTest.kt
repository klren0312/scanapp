package com.example.scanapp

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun testPlatformName() {
        val platform = Platform()
        assertTrue(platform.name.isNotEmpty())
    }
}