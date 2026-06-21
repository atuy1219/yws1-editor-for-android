package com.atuy.yws1editor.yokai

import android.content.pm.PackageManager
import com.atuy.yws1editor.shizuku.ShizukuFileServiceClient
import rikka.shizuku.Shizuku
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MainBinBackupInfo(
    val fileName: String,
    val displayName: String,
    val backupEpochMillis: Long,
    val filePath: String,
    val lastModifiedEpochMillis: Long,
)

class ShizukuFileGateway {

    private val managedBackupRegex = Regex("""^main_backup_(\d{12})_([A-Za-z0-9%._+-]{1,120})\.bin$""")
    private val backupTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    fun isShizukuRunning(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean {
        if (!isShizukuRunning()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: RuntimeException) {
            false
        }
    }

    fun requestPermission(requestCode: Int): Boolean {
        if (!isShizukuRunning()) return false
        return try {
            Shizuku.requestPermission(requestCode)
            true
        } catch (_: RuntimeException) {
            // Binder 未受信直後のレースを吸収し、UI から再試行できるようにする。
            false
        }
    }

    fun isPreV11(): Boolean = Shizuku.isPreV11()

    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (_: RuntimeException) {
            false
        }
    }

    fun serverUid(): Int? {
        return try {
            Shizuku.getUid()
        } catch (_: RuntimeException) {
            null
        }
    }

    fun readBytes(path: String): ByteArray {
        return ShizukuFileServiceClient.readFile(path)
    }

    fun backup(path: String): String {
        val backupPath = "$path.bak"
        ShizukuFileServiceClient.requireService().copyFile(path, backupPath)
        return backupPath
    }

    fun createManagedBackup(path: String, backupEpochMillis: Long, backupName: String): MainBinBackupInfo {
        val backupsDir = backupDir(path)
        val timestamp = buildBackupTimestamp(backupEpochMillis)
        val encodedName = encodeBackupName(backupName)
        val displayName = decodeBackupName(encodedName)
        val fileName = "main_backup_${timestamp}_${encodedName}.bin"
        val backupPath = "$backupsDir/$fileName"

        val service = ShizukuFileServiceClient.requireService()
        service.createDirectories(backupsDir)
        service.copyFile(path, backupPath)

        val modified = lastModifiedMillis(backupPath)
        return MainBinBackupInfo(
            fileName = fileName,
            displayName = displayName,
            backupEpochMillis = backupEpochMillis,
            filePath = backupPath,
            lastModifiedEpochMillis = modified,
        )
    }

    fun listManagedBackups(path: String): List<MainBinBackupInfo> {
        val backupsDir = backupDir(path)
        val service = ShizukuFileServiceClient.requireService()
        service.createDirectories(backupsDir)
        val files = service.listFileNames(backupsDir)

        return files.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { fileName ->
                val parsed = parseManagedBackup(fileName) ?: return@mapNotNull null
                val filePath = "$backupsDir/$fileName"
                val modified = runCatching { lastModifiedMillis(filePath) }.getOrDefault(parsed.first)
                MainBinBackupInfo(
                    fileName = fileName,
                    displayName = parsed.second,
                    backupEpochMillis = parsed.first,
                    filePath = filePath,
                    lastModifiedEpochMillis = modified,
                )
            }
            .sortedByDescending { it.backupEpochMillis }
            .toList()
    }

    fun readManagedBackup(path: String, backupFileName: String): ByteArray {
        val backup = parseManagedBackup(backupFileName)
            ?: throw IOException("不正なバックアップ名です: $backupFileName")
        if (backup.second.isBlank()) {
            throw IOException("バックアップ名の解析に失敗しました: $backupFileName")
        }

        val sourcePath = "${backupDir(path)}/$backupFileName"
        return readBytes(sourcePath)
    }

    fun writeBytes(path: String, data: ByteArray) {
        ShizukuFileServiceClient.writeFileAtomically(path, data)
    }

    fun lastModifiedMillis(path: String): Long {
        return ShizukuFileServiceClient.requireService().lastModified(path)
    }

    private fun backupDir(path: String): String {
        val normalized = path.replace('\\', '/')
        val slash = normalized.lastIndexOf('/')
        if (slash <= 0) return "./backups"
        return normalized.substring(0, slash) + "/backups"
    }

    private fun buildBackupTimestamp(epochMillis: Long): String {
        val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return local.format(backupTimestampFormatter)
    }

    private fun parseManagedBackup(fileName: String): Pair<Long, String>? {
        val match = managedBackupRegex.matchEntire(fileName) ?: return null
        val digits = match.groupValues[1]
        val local = runCatching { LocalDateTime.parse(digits, backupTimestampFormatter) }.getOrNull() ?: return null
        val epoch = local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val name = decodeBackupName(match.groupValues[2])
        return epoch to name
    }

    private fun encodeBackupName(input: String): String {
        val normalized = input.trim().ifBlank { "backup" }.take(40)
        return URLEncoder.encode(normalized, Charsets.UTF_8.name())
    }

    private fun decodeBackupName(input: String): String {
        return runCatching { URLDecoder.decode(input, Charsets.UTF_8.name()) }
            .getOrDefault(input)
    }

}
