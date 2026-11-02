package com.fstudio.fsmanager

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var granted by mutableStateOf(false)

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) startShizukuMode()
        else toast("تم رفض صلاحية Shizuku")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        granted = hasAccess()
        runCatching { Shizuku.addRequestPermissionResultListener(shizukuListener) }
        setContent {
            FSTheme {
                LaunchedEffect(granted) { if (granted) vm.initIfNeeded() }
                if (granted) {
                    FileManagerScreen(vm, ::openFile, ::onShizukuClick)
                } else {
                    PermissionScreen(::requestAccess)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        granted = hasAccess()
    }

    override fun onDestroy() {
        runCatching { Shizuku.removeRequestPermissionResultListener(shizukuListener) }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        granted = hasAccess()
    }

    private fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    private fun onShizukuClick() {
        if (!ShizukuHelper.isAvailable()) {
            toast("Shizuku غير مشغّل. افتح تطبيق Shizuku وشغّله أولاً")
            return
        }
        if (!ShizukuHelper.hasPermission()) {
            ShizukuHelper.requestPermission(100)
            return
        }
        startShizukuMode()
    }

    private fun startShizukuMode() {
        ShizukuHelper.ensureService { vm.startShizuku() }
    }

    private fun openFile(path: String) {
        val f = File(path)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, null))
        } catch (e: ActivityNotFoundException) {
            toast("لا يوجد تطبيق لفتح هذا الملف")
        }
    }

    private fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
