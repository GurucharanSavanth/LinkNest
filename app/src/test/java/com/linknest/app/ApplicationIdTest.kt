package com.linknest.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationIdTest {
    @Test
    fun appId_isExpected() {
        assertEquals("com.linknest.app", MainActivity::class.java.`package`?.name)
    }
}
