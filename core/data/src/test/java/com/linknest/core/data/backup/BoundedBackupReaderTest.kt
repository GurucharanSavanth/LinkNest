package com.linknest.core.data.backup

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Test

class BoundedBackupReaderTest {
    @Test fun acceptsExactLimit() {
        assertEquals("abcd", StringReader("abcd").readBackupText(4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedInput() {
        StringReader("abcde").readBackupText(4)
    }
}
