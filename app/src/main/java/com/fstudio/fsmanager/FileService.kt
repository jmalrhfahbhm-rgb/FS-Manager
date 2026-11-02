package com.fstudio.fsmanager

import java.io.File
import kotlin.system.exitProcess

/** تعمل هذه الخدمة داخل عملية Shizuku (صلاحيات shell) عبر UserService الرسمية. */
class FileService : IFileService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun listDir(path: String): Array<String> {
        val files = File(path).listFiles() ?: return emptyArray()
        return files.map {
            val size = if (it.isDirectory) 0L else it.length()
            "${it.name}\u0001${if (it.isDirectory) 1 else 0}\u0001$size\u0001${it.lastModified()}"
        }.toTypedArray()
    }

    override fun delete(path: String): Boolean = File(path).deleteRecursively()
}
