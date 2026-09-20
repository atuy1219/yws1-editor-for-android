package com.atuy.yws1editor.yw2

import com.atuy.yws1editor.yokai.ShizukuFileGateway
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Yw2SaveFile(
    val fileName: String,
    val path: String,
)

data class Yw2SaveDiscovery(
    val directory: String,
    val gameFiles: List<Yw2SaveFile>,
    val headPath: String?,
)

data class Yw2LoadedSave(
    val file: Yw2SaveFile,
    val encrypted: ByteArray,
    val decoded: ByteArray,
    val keyMode: Yw2Crypto.KeyMode,
    val headBytes: ByteArray?,
)

data class Yw2SaveWriteResult(
    val encrypted: ByteArray,
    val backupPath: String,
)

class Yw2SaveStorage(
    private val gateway: ShizukuFileGateway,
    private val saveDirectory: String = DEFAULT_SAVE_DIRECTORY,
) {
    companion object {
        const val DEFAULT_SAVE_DIRECTORY =
            "/storage/emulated/0/Citra/sdmc/Nintendo 3DS/" +
                "00000000000000000000000000000000/" +
                "00000000000000000000000000000000/" +
                "title/00040000/00155100/data/00000001"

        private val GAME_FILE_REGEX = Regex("^game(?:\\d+)?\\.yw$", RegexOption.IGNORE_CASE)
        private val GAME_INDEX_REGEX = Regex("\\d+")
    }

    fun discover(): Yw2SaveDiscovery {
        val names = gateway.listFileNames(saveDirectory)
        val headName = names.firstOrNull { it.equals("head.yw", ignoreCase = true) }
        val games = names.asSequence()
            .filter { GAME_FILE_REGEX.matches(it) }
            .sortedWith(
                compareBy<String> { gameIndex(it) }
                    .thenBy { it.lowercase(Locale.ROOT) }
            )
            .map { Yw2SaveFile(fileName = it, path = "$saveDirectory/$it") }
            .toList()

        return Yw2SaveDiscovery(
            directory = saveDirectory,
            gameFiles = games,
            headPath = headName?.let { "$saveDirectory/$it" },
        )
    }

    fun load(file: Yw2SaveFile, headPath: String?): Yw2LoadedSave {
        val headBytes = headPath?.let(gateway::readBytes)
        val encrypted = gateway.readBytes(file.path)
        val decoded = Yw2Crypto.decrypt(encrypted, headBytes)

        // AES-CCM authentication + YW CRC validate the encrypted file.
        // Domain parsing is intentionally deferred to each editor tab so a
        // parser bug cannot prevent opening a valid save.

        return Yw2LoadedSave(
            file = file,
            encrypted = encrypted,
            decoded = decoded.data,
            keyMode = decoded.keyMode,
            headBytes = headBytes,
        )
    }

    fun save(
        file: Yw2SaveFile,
        decoded: ByteArray,
        keyMode: Yw2Crypto.KeyMode,
        headBytes: ByteArray?,
    ): Yw2SaveWriteResult {
        // Before overwriting anything, prove that our writer can reproduce the
        // current on-disk save byte-for-byte without edits. Because YW2 uses a
        // deterministic nonce/seed-preserving pipeline, a mismatch means our
        // serializer/encryption is not game-compatible yet.
        val currentEncrypted = gateway.readBytes(file.path)
        val currentDecoded = Yw2Crypto.decrypt(currentEncrypted, headBytes)
        val noEditRoundTrip = Yw2Crypto.encrypt(
            currentDecoded.data,
            currentDecoded.keyMode,
            headBytes,
        )
        if (!noEditRoundTrip.contentEquals(currentEncrypted)) {
            throw IOException(
                "安全のため保存を中止しました: 無編集再暗号化が元のgame*.ywと一致しません"
            )
        }

        val encrypted = Yw2Crypto.encrypt(decoded, keyMode, headBytes)

        // Verify the exact bytes before touching the emulator save.
        verifyEncrypted(encrypted, headBytes)

        val backupPath = createBackup(file)
        gateway.writeBytes(file.path, encrypted)

        // Verify the atomic write actually persisted and remains decryptable.
        val persisted = gateway.readBytes(file.path)
        if (!persisted.contentEquals(encrypted)) {
            throw IOException("保存後の game*.yw が書き込み内容と一致しません")
        }
        verifyEncrypted(persisted, headBytes)

        return Yw2SaveWriteResult(
            encrypted = encrypted,
            backupPath = backupPath,
        )
    }

    private fun verifyEncrypted(encrypted: ByteArray, headBytes: ByteArray?) {
        Yw2Crypto.decrypt(encrypted, headBytes)
    }

    private fun createBackup(file: Yw2SaveFile): String {
        val backupDir = "$saveDirectory/backups"
        gateway.createDirectories(backupDir)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupPath = "$backupDir/${file.fileName}.$stamp.bak"
        gateway.copyFile(file.path, backupPath)
        return backupPath
    }

    private fun gameIndex(name: String): Int =
        GAME_INDEX_REGEX.find(name)?.value?.toIntOrNull() ?: -1
}
