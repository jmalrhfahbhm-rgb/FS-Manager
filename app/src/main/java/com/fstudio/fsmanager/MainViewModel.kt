package com.fstudio.fsmanager

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class UiState(
    val path: String = "",
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val showHidden: Boolean = false,
    val shizukuMode: Boolean = false,
    val clipboard: List<String> = emptyList(),
    val cut: Boolean = false,
    val message: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val storageRoot: String = Environment.getExternalStorageDirectory().absolutePath
    private val shizukuRoot = "$storageRoot/Android/data"

    private val _state = MutableStateFlow(UiState(path = storageRoot))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var started = false

    fun rootOf(shizuku: Boolean) = if (shizuku) shizukuRoot else storageRoot

    fun initIfNeeded() {
        if (!started) {
            started = true
            load(storageRoot)
        }
    }

    fun load(path: String) {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val shz = _state.value.shizukuMode
            val hidden = _state.value.showHidden
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (shz) ShizukuHelper.list(path) else LocalFiles.list(path, hidden)
                }
            }
            result.onSuccess { items ->
                _state.update {
                    it.copy(path = path, items = items, loading = false, selected = emptySet(), query = "")
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, message = e.message ?: "تعذّر فتح المجلد") }
            }
        }
    }

    fun refresh() = load(_state.value.path)
    fun open(path: String) = load(path)

    fun up() {
        val p = _state.value.path
        if (p != rootOf(_state.value.shizukuMode)) File(p).parent?.let { load(it) }
    }

    fun toggleSelect(path: String) = _state.update {
        it.copy(selected = if (path in it.selected) it.selected - path else it.selected + path)
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    fun toggleHidden() {
        _state.update { it.copy(showHidden = !it.showHidden) }
        refresh()
    }

    fun startShizuku() {
        _state.update { it.copy(shizukuMode = true, clipboard = emptyList(), selected = emptySet()) }
        load(shizukuRoot)
    }

    fun exitShizuku() {
        _state.update { it.copy(shizukuMode = false) }
        load(storageRoot)
    }

    private fun blockedInShizuku(): Boolean {
        if (_state.value.shizukuMode) {
            _state.update { it.copy(message = "هذه العملية غير مدعومة في وضع Shizuku (القراءة والحذف فقط)") }
            return true
        }
        return false
    }

    private fun runOp(okMsg: String, block: () -> Boolean) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching(block).getOrDefault(false) }
            _state.update { it.copy(message = if (ok) okMsg else "فشلت العملية") }
            refresh()
        }
    }

    fun deleteSelected() {
        val shz = _state.value.shizukuMode
        val paths = _state.value.selected.toList()
        runOp("تم الحذف") {
            paths.all { if (shz) ShizukuHelper.delete(it) else LocalFiles.delete(it) }
        }
    }

    fun rename(newName: String) {
        if (blockedInShizuku()) return
        val src = _state.value.selected.firstOrNull() ?: return
        runOp("تمت إعادة التسمية") { LocalFiles.rename(src, newName) }
    }

    fun newFolder(name: String) {
        if (blockedInShizuku()) return
        val dir = _state.value.path
        runOp("تم إنشاء المجلد") { LocalFiles.mkdir(dir, name) }
    }

    fun copy() {
        if (blockedInShizuku()) return
        _state.update { it.copy(clipboard = it.selected.toList(), cut = false, selected = emptySet()) }
    }

    fun cut() {
        if (blockedInShizuku()) return
        _state.update { it.copy(clipboard = it.selected.toList(), cut = true, selected = emptySet()) }
    }

    fun clearClipboard() = _state.update { it.copy(clipboard = emptyList()) }

    fun paste() {
        if (blockedInShizuku()) return
        val clip = _state.value.clipboard
        val isCut = _state.value.cut
        val dest = _state.value.path
        runOp(if (isCut) "تم النقل" else "تم النسخ") {
            clip.all { if (isCut) LocalFiles.move(it, dest) else LocalFiles.copy(it, dest) }
        }
        clearClipboard()
    }
}
