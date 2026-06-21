package com.atuy.yws1editor.shizuku

import android.os.ParcelFileDescriptor
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

class ShizukuFileService : IShizukuFileService.Stub() {

    override fun openFileForRead(path: String): ParcelFileDescriptor {
        return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun writeFileAtomically(
        path: String,
        source: ParcelFileDescriptor,
        expectedLength: Long,
        expectedSha256: String,
    ) {
        val target = File(path)
        val parent = target.parentFile ?: throw IOException("親ディレクトリがありません: $path")
        val temporary = File(parent, "${target.name}.yws1editor.tmp")
        val originalStat = Os.stat(target.path)

        temporary.delete()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            ParcelFileDescriptor.AutoCloseInputStream(source).use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        written += count
                    }
                    output.fd.sync()
                }
            }
            val actualSha256 = digest.digest()
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            if (written != expectedLength || actualSha256 != expectedSha256) {
                throw IOException("一時ファイルの書き込み検証に失敗しました")
            }

            Os.chown(temporary.path, originalStat.st_uid, originalStat.st_gid)
            Os.chmod(temporary.path, originalStat.st_mode and 0xFFF)
            Os.rename(temporary.path, target.path)
        } finally {
            temporary.delete()
        }
    }

    override fun copyFile(sourcePath: String, targetPath: String) {
        File(sourcePath).copyTo(File(targetPath), overwrite = true)
    }

    override fun createDirectories(path: String) {
        val directory = File(path)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("ディレクトリを作成できません: $path")
        }
    }

    override fun listFileNames(path: String): Array<String> {
        val directory = File(path)
        return directory.list() ?: throw IOException("ディレクトリを読み取れません: $path")
    }

    override fun lastModified(path: String): Long {
        val file = File(path)
        if (!file.exists()) throw IOException("ファイルが見つかりません: $path")
        return file.lastModified()
    }
}
