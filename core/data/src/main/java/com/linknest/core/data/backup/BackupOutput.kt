package com.linknest.core.data.backup

import java.io.OutputStream

internal fun writeBackupPayload(output: OutputStream?, payload: String) {
    requireNotNull(output) { "Could not open file for writing." }.use {
        it.write(payload.toByteArray(Charsets.UTF_8))
        it.flush()
    }
}
