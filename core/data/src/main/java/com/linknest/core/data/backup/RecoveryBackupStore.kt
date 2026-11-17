package com.linknest.core.data.backup

import android.util.AtomicFile
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.RecoveryOutcome
import com.linknest.core.data.model.RecoveryReconciliation
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Two verified generations; incomplete operations remain protected until explicitly completed. */
internal class RecoveryBackupStore(
    private val directory: File,
    private val serializer: BackupSerializer,
    private val manifestWriter: (AtomicFile, ByteArray) -> Unit = ::writeRecoveryFile,
) {
    private data class Generation(
        val name: String, val checksum: String, val completed: Boolean, val createdAt: Long,
        val phase: String = "STAGED", val before: String? = null, val expected: String? = null,
    )
    private val manifest get() = AtomicFile(File(directory, "recovery-index.json"))

    @Synchronized fun stage(pkg: BackupPackage): File {
        val snapshot = serializer.deserialize(pkg.payload)
        validateBackup(snapshot)
        val bytes = pkg.payload.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Recovery snapshot exceeds the 32 MiB encoded limit." }
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create recovery directory." }
        val previous = generations()
        val verifiedSource = latest()?.name
        previous.filter { !it.completed }.forEach { entry ->
            val existing = File(directory, entry.name)
            val recovered = try {
                val text = AtomicFile(existing).openRead().bufferedReader(Charsets.UTF_8).use { it.readBackupText() }
                if (text.toByteArray(Charsets.UTF_8).checksum() == entry.checksum) serializer.deserialize(text) else null
            } catch (_: Exception) { null }
            if (recovered?.copy(exportedAt = 0) == snapshot.copy(exportedAt = 0)) return existing
        }
        check(previous.count { !it.completed } < RETAINED) {
            "Two recovery snapshots protect incomplete restores. Resolve recovery before replacing the library again."
        }
        // Reclaim only unpublished, app-owned generations after AtomicFile recovered the manifest.
        directory.listFiles()?.filter { it.name.matches(FILE_NAME) && previous.none { entry -> entry.name == it.name } }
            ?.forEach { AtomicFile(it).delete() }
        val protected = previous.filter { !it.completed || it.name == verifiedSource }
        check(protected.size < RETAINED) {
            "Recovery snapshots protect unresolved operations and the last verified recovery. Resolve recovery before replacing the library again."
        }
        val name = "recovery-${UUID.randomUUID()}.json"
        val target = File(directory, name)
        writeRecoveryFile(AtomicFile(target), bytes)
        check(target.readBytes().contentEquals(bytes)) { "Recovery snapshot verification failed." }
        val newest = Generation(name, bytes.checksum(), false, System.currentTimeMillis())
        val retained = (listOf(newest) + protected + previous.filter { it.completed })
            .distinctBy { it.name }.take(RETAINED)
        try {
            publish(retained)
        } catch (failure: Exception) {
            AtomicFile(target).delete()
            throw failure
        }
        previous.filter { old -> retained.none { it.name == old.name } }
            .forEach { AtomicFile(File(directory, it.name)).delete() }
        return target
    }

    @Synchronized fun complete(file: File) {
        val entries = generations()
        require(entries.any { File(directory, it.name).absolutePath == file.absolutePath }) { "Unknown recovery generation." }
        publish(entries.map { if (File(directory, it.name).absolutePath == file.absolutePath) it.copy(completed = true) else it })
    }

    @Synchronized fun prepare(name: String, current: BackupSnapshot, incoming: BackupSnapshot) {
        val entries = generations()
        val entry = entries.singleOrNull { it.name == name && !it.completed }
            ?: error("Recovery operation is missing or already completed.")
        val before = recoveryFingerprint(readSnapshot(entry))
        require(recoveryFingerprint(current) == before) { "The library changed after recovery staging. Preview and confirm again." }
        publish(entries.map { if (it.name == name) it.copy(phase = "PREPARED", before = before, expected = recoveryFingerprint(incoming)) else it })
    }

    @Synchronized fun committed(name: String) {
        val entries = generations()
        check(entries.any { it.name == name && it.phase == "PREPARED" }) { "Recovery operation was not prepared." }
        publish(entries.map { if (it.name == name) it.copy(phase = "COMMITTED") else it })
    }

    @Synchronized fun reconcile(current: BackupSnapshot): List<RecoveryReconciliation> {
        val entries = generations()
        val actual = recoveryFingerprint(current)
        val currentComparable = current.omittedCustomImages == 0 && current.websites.none { it.customIconUri != null && it.customIconAssetId == null }
        val results = entries.filter { !it.completed }.map { entry ->
            val staged = runCatching { readSnapshot(entry) }.getOrNull()
            val before = entry.before ?: staged?.let(::recoveryFingerprint)
            val beforeComparable = staged != null && staged.omittedCustomImages == 0 &&
                staged.websites.none { it.customIconUri != null && it.customIconAssetId == null }
            val outcome = when {
                entry.phase == "COMMITTED" -> RecoveryOutcome.COMMIT_RECORDED
                currentComparable && beforeComparable && actual == before -> RecoveryOutcome.ORIGINAL_PRESENT
                currentComparable && actual == entry.expected -> RecoveryOutcome.IMPORTED_PRESENT
                else -> RecoveryOutcome.UNKNOWN
            }
            RecoveryReconciliation(entry.name, outcome)
        }
        val proven = results.filter { it.outcome != RecoveryOutcome.UNKNOWN }.map { it.generationName }.toSet()
        if (proven.isNotEmpty()) publish(entries.map { if (it.name in proven) it.copy(completed = true) else it })
        return results
    }

    @Synchronized fun latest(): File? = generations().firstNotNullOfOrNull { entry ->
        try { readSnapshot(entry); File(directory, entry.name) }
        catch (_: java.io.IOException) { null }
        catch (_: IllegalArgumentException) { null }
        catch (_: org.json.JSONException) { null }
    }

    private fun readSnapshot(entry: Generation): BackupSnapshot {
        val text = AtomicFile(File(directory, entry.name)).openRead().bufferedReader(Charsets.UTF_8).use { it.readBackupText() }
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES && bytes.checksum() == entry.checksum) { "Recovery checksum mismatch." }
        return serializer.deserialize(text).also(::validateBackup)
    }

    private fun generations(): List<Generation> {
        if (!manifest.baseFile.exists() && !File(directory, "recovery-index.json.bak").exists()) return emptyList()
        val entries = JSONObject(manifest.openRead().bufferedReader(Charsets.UTF_8).use { it.readBackupText(4096) })
        val array = entries.getJSONArray("generations")
        require(array.length() <= RETAINED) { "Invalid recovery manifest." }
        return List(array.length()) { index ->
            val entry = array.getJSONObject(index)
            val name = entry.getString("name")
            require(name.matches(FILE_NAME)) { "Invalid recovery filename." }
            Generation(name, entry.getString("checksum"), entry.getBoolean("completed"), entry.getLong("createdAt"),
                entry.optString("phase", "STAGED"), entry.optString("before").takeIf { it.isNotBlank() },
                entry.optString("expected").takeIf { it.isNotBlank() })
        }.also { require(it.map { entry -> entry.name }.distinct().size == it.size) }
    }

    private fun publish(entries: List<Generation>) {
        val json = JSONObject().put("version", 1).put("generations", JSONArray().apply {
            entries.forEach { entry -> put(JSONObject().put("name", entry.name).put("checksum", entry.checksum).put("completed", entry.completed).put("createdAt", entry.createdAt).put("phase", entry.phase).put("before", entry.before).put("expected", entry.expected)) }
        })
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        manifestWriter(manifest, bytes)
        check(manifest.openRead().use { it.readBytes() }.contentEquals(bytes)) { "Recovery manifest publication could not be verified." }
    }

    private fun ByteArray.checksum(): String = MessageDigest.getInstance("SHA-256")
        .digest(this).joinToString("") { "%02x".format(it) }

    private companion object {
        const val RETAINED = 2
        const val MAX_BYTES = 32 * 1024 * 1024
        val FILE_NAME = Regex("recovery-[0-9a-f-]{36}\\.json")
    }
}

private fun writeRecoveryFile(file: AtomicFile, bytes: ByteArray) {
    val output = file.startWrite()
    try {
        output.write(bytes)
        output.flush()
        output.fd.sync()
        file.finishWrite(output)
    } catch (failure: Exception) {
        file.failWrite(output)
        throw failure
    }
}
