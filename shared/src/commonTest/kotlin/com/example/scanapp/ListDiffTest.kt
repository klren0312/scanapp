package com.example.scanapp

import com.example.scanapp.util.diffUpdate
import kotlin.test.Test
import kotlin.test.assertEquals

class ListDiffTest {

    @Test
    fun diffUpdate_handlesMiddleChanges() {
        val actual = mutableListOf(1, 2, 3, 4)

        actual.diffUpdate(listOf(1, 3, 5, 4))

        assertEquals(listOf(1, 3, 5, 4), actual)
    }

    @Test
    fun diffUpdate_handlesLargeReplacementWithoutQuadraticMatrix() {
        val actual = (0 until 600).toMutableList()
        val expected = (300 until 900).toList()

        actual.diffUpdate(expected)

        assertEquals(expected, actual)
    }
}
