package com.fstudio.fsmanager

data class FileItem(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long
)
