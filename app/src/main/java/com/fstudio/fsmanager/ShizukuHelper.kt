package com.fstudio.fsmanager

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/** تكامل Shizuku الرسمي (UserService) للوصول إلى Android/data. */
object ShizukuHelper {
    private var service: IFileService? = null
    private var pending: (() -> Unit)? = null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                service = IFileService.Stub.asInterface(binder)
                pending?.invoke()
                pending = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermission(code: Int) {
        runCatching { Shizuku.requestPermission(code) }
    }

    fun ensureService(onReady: () -> Unit) {
        if (service != null) {
            onReady(); return
        }
        pending = onReady
        runCatching {
            val args = Shizuku.UserServiceArgs(
                ComponentName(BuildConfig.APPLICATION_ID, FileService::class.java.name)
            )
                .daemon(false)
                .processNameSuffix("fs")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
            Shizuku.bindUserService(args, conn)
        }
    }

    fun list(path: String): List<FileItem> {
        val s = service ?: error("خدمة Shizuku غير متصلة")
        return s.listDir(path).map {
            val p = it.split('\u0001')
            FileItem(p[0], "$path/${p[0]}", p[1] == "1", p[2].toLong(), p[3].toLong())
        }
    }

    fun delete(path: String): Boolean = service?.delete(path) ?: false
}
