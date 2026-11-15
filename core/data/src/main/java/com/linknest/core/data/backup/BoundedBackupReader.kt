package com.linknest.core.data.backup

import java.io.Reader

internal fun Reader.readBackupText(maxChars: Int = 32 * 1024 * 1024): String {
    val result = StringBuilder()
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toString()
        require(count <= maxChars - result.length) { "Backup payload is too large." }
        result.append(buffer, 0, count)
    }
}
