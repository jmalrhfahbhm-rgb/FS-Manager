package com.fstudio.fsmanager

import java.io.File
import java.io.IOException

object LocalFiles {

    fun list(path: String, showHidden: Boolean): List<FileItem> {
        val files = File(path).listFiles() ?: throw IOException("تعذّر قراءة هذا المجلد")
        return files
            .filter { showHidden || !it.name.startsWith(".") }
            .map {
                FileItem(
                    name = it.name,
                    path = it.absolutePath,
                    isDir = it.isDirectory,
                    size = if (it.isDirectory) 0L else it.length(),
                    modified = it.lastModified()
                )
            }
    }

    fun delete(path: String): Boolean = File(path).deleteRecursively()

    fun rename(path: String, newName: String): Boolean {
        if (newName.contains('/')) return false
        val f = File(path)
        val target = File(f.parentFile, newName)
        if (target.exists()) return false
        return f.renameTo(target)
    }

    fun mkdir(parent: String, name: String): Boolean {
        if (name.contains('/')) return false
        val d = File(parent, name)
        return !d.exists() && d.mkdirs()
    }

    fun copy(src: String, destDir: String): Boolean {
        val s = File(src)
        if (s.isDirectory && File(destDir).absolutePath.startsWith(s.absolutePath)) return false
        val t = unique(File(destDir, s.name))
        return s.copyRecursively(t, overwrite = false)
    }

    fun move(src: String, destDir: String): Boolean {
        val s = File(src)
        if (s.parentFile?.absolutePath == File(destDir).absolutePath) return true
        if (s.isDirectory && File(destDir).absolutePath.startsWith(s.absolutePath)) return false
        val t = unique(File(destDir, s.name))
        if (s.renameTo(t)) return true
        return s.copyRecursively(t, overwrite = false) && s.deleteRecursively()
    }

    private fun unique(f: File): File {
        if (!f.exists()) return f
        val base = f.nameWithoutExtension
        val ext = if (f.extension.isEmpty()) "" else ".${f.extension}"
        var i = 1
        var c: File
        do {
            c = File(f.parentFile, "$base ($i)$ext")
            i++
        } while (c.exists())
        return c
    }
}
