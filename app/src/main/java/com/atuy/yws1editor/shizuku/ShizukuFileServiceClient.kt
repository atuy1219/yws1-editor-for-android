package com.atuy.yws1editor.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import rikka.shizuku.Shizuku

object ShizukuFileServiceClient {
    @Volatile
    private var service: IShizukuFileService? = null
    private var binding = false
    private var applicationContext: Context? = null
    private var stateListener: ((Boolean) -> Unit)? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IShizukuFileService.Stub.asInterface(binder)
            binding = false
            stateListener?.invoke(service != null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            reset()
            stateListener?.invoke(false)
        }

        override fun onBindingDied(name: ComponentName?) {
            reset()
            stateListener?.invoke(false)
        }

        override fun onNullBinding(name: ComponentName?) {
            reset()
            stateListener?.invoke(false)
        }
    }

    fun setStateListener(listener: ((Boolean) -> Unit)?) {
        stateListener = listener
        if (service != null) listener?.invoke(true)
    }

    fun bind(context: Context) {
        applicationContext = context.applicationContext
        if (service != null) {
            stateListener?.invoke(true)
            return
        }
        if (binding) return

        binding = true
        val versionCode = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            .longVersionCode
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuFileService::class.java))
            .processNameSuffix("file_service")
            .tag("yws1_file_service")
            .version(versionCode)
            .daemon(false)
        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: RuntimeException) {
            binding = false
            stateListener?.invoke(false)
            throw e
        }
    }

    fun reset() {
        service = null
        binding = false
    }

    fun requireService(): IShizukuFileService {
        return service ?: throw IOException("Shizukuファイルサービスが未接続です")
    }

    fun readFile(path: String): ByteArray {
        val descriptor = requireService().openFileForRead(path)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    fun writeFileAtomically(path: String, data: ByteArray) {
        val context = applicationContext ?: throw IOException("アプリコンテキストが未設定です")
        val stagingFile = File.createTempFile("yws1-write-", ".bin", context.cacheDir)
        try {
            stagingFile.writeBytes(data)
            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(data)
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            ParcelFileDescriptor.open(stagingFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                requireService().writeFileAtomically(path, descriptor, data.size.toLong(), sha256)
            }
        } finally {
            stagingFile.delete()
        }
    }
}
