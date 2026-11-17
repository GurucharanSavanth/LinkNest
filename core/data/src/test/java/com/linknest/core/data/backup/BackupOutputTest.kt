package com.linknest.core.data.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test

class BackupOutputTest {
    @Test fun writesUtf8AndClosesBeforeSuccess() {
        var closed = false
        val output = object : ByteArrayOutputStream() {
            override fun close() { closed = true; super.close() }
        }
        writeBackupPayload(output, "保存")
        assertEquals("保存", output.toString("UTF-8"))
        assertTrue(closed)
    }

    @Test fun nullStreamFails() {
        assertThrows(IllegalArgumentException::class.java) { writeBackupPayload(null, "payload") }
    }

    @Test fun writeFlushAndCloseFailuresNeverReportSuccess() {
        for (stage in listOf("write", "flush", "close")) {
            var closed = false
            val output = object : OutputStream() {
                override fun write(value: Int) { if (stage == "write") throw IOException("ENOSPC") }
                override fun flush() { if (stage == "flush") throw IOException("provider flush") }
                override fun close() { closed = true; if (stage == "close") throw IOException("provider close") }
            }
            assertThrows(IOException::class.java) { writeBackupPayload(output, "payload") }
            assertTrue("Stream must close after $stage failure", closed)
        }
    }
}
