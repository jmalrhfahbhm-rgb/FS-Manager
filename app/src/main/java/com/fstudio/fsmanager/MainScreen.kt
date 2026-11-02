package com.fstudio.fsmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Dlg { NEW, RENAME, DELETE }

fun formatSize(b: Long): String {
    if (b < 1024) return "$b B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = b.toDouble()
    var i = -1
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024
        i++
    }
    return String.format(Locale.US, "%.1f %s", v, units[i])
}

private fun formatDate(t: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(t))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    vm: MainViewModel,
    onOpenFile: (String) -> Unit,
    onShizuku: () -> Unit
) {
    val s by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    var searchOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Dlg?>(null) }

    val selecting = s.selected.isNotEmpty()
    val canGoUp = s.path != vm.rootOf(s.shizukuMode)

    LaunchedEffect(s.message) {
        s.message?.let {
            snack.showSnackbar(it)
            vm.clearMessage()
        }
    }

    BackHandler(enabled = selecting || canGoUp || s.shizukuMode) {
        when {
            selecting -> vm.clearSelection()
            canGoUp -> vm.up()
            s.shizukuMode -> vm.exitShizuku()
        }
    }

    val shown = remember(s.items, s.query) {
        s.items
            .filter { s.query.isBlank() || it.name.contains(s.query.trim(), ignoreCase = true) }
            .sortedWith(
                compareByDescending<FileItem> { it.isDir }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
    }

    val shortPath = s.path.removePrefix(vm.storageRoot).ifEmpty { "/" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val t = when {
                        selecting -> "${s.selected.size} محدد"
                        s.shizukuMode -> "Shizuku · $shortPath"
                        else -> shortPath
                    }
                    Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    if (selecting) {
                        IconButton(onClick = { vm.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                        }
                    }
                    if (!selecting && canGoUp) {
                        IconButton(onClick = { vm.up() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = { vm.copy() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ")
                        }
                        IconButton(onClick = { vm.cut() }) {
                            Icon(Icons.Default.ContentCut, contentDescription = "قص")
                        }
                        if (s.selected.size == 1) {
                            IconButton(onClick = { dialog = Dlg.RENAME }) {
                                Icon(Icons.Default.Edit, contentDescription = "إعادة تسمية")
                            }
                        }
                        IconButton(onClick = { dialog = Dlg.DELETE }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                    } else {
                        IconButton(onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) vm.setQuery("")
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "المزيد")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (s.showHidden) "إخفاء الملفات المخفية" else "إظهار الملفات المخفية") },
                                onClick = { menuOpen = false; vm.toggleHidden() }
                            )
                            if (s.shizukuMode) {
                                DropdownMenuItem(
                                    text = { Text("الخروج من وضع Shizuku") },
                                    onClick = { menuOpen = false; vm.exitShizuku() }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Android/data (عبر Shizuku)") },
                                    onClick = { menuOpen = false; onShizuku() }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (!s.shizukuMode && s.clipboard.isEmpty() && !selecting) {
                FloatingActionButton(onClick = { dialog = Dlg.NEW }) {
                    Icon(Icons.Default.Add, contentDescription = "مجلد جديد")
                }
            }
        },
        bottomBar = {
            if (s.clipboard.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${s.clipboard.size} عنصر (${if (s.cut) "قص" else "نسخ"})")
                        Row {
                            TextButton(onClick = { vm.clearClipboard() }) { Text("إلغاء") }
                            Button(onClick = { vm.paste() }) { Text("لصق هنا") }
                        }
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (searchOpen) {
                OutlinedTextField(
                    value = s.query,
                    onValueChange = { vm.setQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    singleLine = true,
                    placeholder = { Text("بحث في هذا المجلد") }
                )
            }
            if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (shown.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد عناصر")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.path }) { f ->
                        val picked = f.path in s.selected
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    when {
                                        selecting -> vm.toggleSelect(f.path)
                                        f.isDir -> vm.open(f.path)
                                        !s.shizukuMode -> onOpenFile(f.path)
                                    }
                                },
                                onLongClick = { vm.toggleSelect(f.path) }
                            ),
                            headlineContent = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(
                                    if (f.isDir) formatDate(f.modified)
                                    else "${formatSize(f.size)} · ${formatDate(f.modified)}"
                                )
                            },
                            leadingContent = {
                                Icon(
                                    if (f.isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (f.isDir) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (picked) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }

    when (dialog) {
        Dlg.NEW -> TextDialog("مجلد جديد", "", "إنشاء", { dialog = null }) {
            vm.newFolder(it); dialog = null
        }
        Dlg.RENAME -> {
            val old = s.selected.firstOrNull()?.let { File(it).name }.orEmpty()
            TextDialog("إعادة تسمية", old, "حفظ", { dialog = null }) {
                vm.rename(it); dialog = null
            }
        }
        Dlg.DELETE -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("حذف ${s.selected.size} عنصر؟") },
            text = { Text("لا يمكن التراجع عن هذه العملية.") },
            confirmButton = { TextButton(onClick = { vm.deleteSelected(); dialog = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("إلغاء") } }
        )
        null -> Unit
    }
}

@Composable
private fun TextDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onOk: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onOk(text.trim()) }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FS Manager", style = MaterialTheme.typography.headlineMedium)
        Text(
            "يحتاج التطبيق إلى صلاحية الوصول إلى جميع الملفات لإدارتها.",
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = onGrant) { Text("منح الصلاحية") }
    }
}
