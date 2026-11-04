package com.example.data.local

import android.content.Context
import com.example.data.model.Category
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteSyncSnapshot
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class LocalWorkspaceStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _notesSnapshotFlow = MutableStateFlow(
        NoteSyncSnapshot(
            notes = loadNotesFromPrefs(),
            hasPendingWrites = false,
            isFromCache = true
        )
    )
    val notesSnapshotFlow: StateFlow<NoteSyncSnapshot> = _notesSnapshotFlow.asStateFlow()

    private val _foldersFlow = MutableStateFlow(loadFoldersFromPrefs())
    val foldersFlow: StateFlow<List<Folder>> = _foldersFlow.asStateFlow()

    private val _categoriesFlow = MutableStateFlow(loadCategoriesFromPrefs())
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow.asStateFlow()

    fun createNote(
        title: String,
        content: String,
        folderId: String = "",
        category: String = "General",
        tags: List<String> = emptyList(),
        colorHex: String = "#1E293B",
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        isTrashed: Boolean = false,
        trashedAtEpochMillis: Long = 0L,
        isLocked: Boolean = false,
        passwordHash: String = "",
        encryptedPayload: String = "",
        reminderEpochMillis: Long = 0L,
        reminderLabel: String = "",
        versionHistory: List<String> = emptyList()
    ): Result<String> {
        val now = Timestamp(Date())
        val noteId = "local_note_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val boundedTags = tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10)
        val boundedHistory = versionHistory.map { it.take(5000) }.take(10)
        val newNote = Note(
            id = noteId,
            userId = LOCAL_USER_ID,
            title = title.take(200),
            content = content.take(20000),
            folderId = folderId.trim().take(128),
            category = category.ifBlank { "General" }.take(50),
            tags = boundedTags,
            colorHex = colorHex.take(20),
            isPinned = isPinned,
            isArchived = isArchived,
            isTrashed = isTrashed,
            trashedAtEpochMillis = trashedAtEpochMillis.coerceAtLeast(0L),
            isLocked = isLocked,
            passwordHash = passwordHash.take(256),
            encryptedPayload = encryptedPayload.take(30000),
            reminderEpochMillis = reminderEpochMillis.coerceAtLeast(0L),
            reminderLabel = reminderLabel.take(100),
            versionHistory = boundedHistory,
            createdAt = now,
            updatedAt = now
        )
        val updatedList = sortNotes(_notesSnapshotFlow.value.notes + newNote)
        saveNotesToPrefs(updatedList)
        _notesSnapshotFlow.value = NoteSyncSnapshot(
            notes = updatedList,
            hasPendingWrites = false,
            isFromCache = true
        )
        return Result.success(noteId)
    }

    private fun sanitizeNoteForUpdate(note: Note, now: Timestamp): Note {
        val boundedTags = note.tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10)
        val boundedHistory = note.versionHistory.map { it.take(5000) }.take(10)
        return note.copy(
            title = note.title.take(200),
            content = note.content.take(20000),
            folderId = note.folderId.trim().take(128),
            category = note.category.ifBlank { "General" }.take(50),
            tags = boundedTags,
            colorHex = note.colorHex.take(20),
            trashedAtEpochMillis = note.trashedAtEpochMillis.coerceAtLeast(0L),
            passwordHash = note.passwordHash.take(256),
            encryptedPayload = note.encryptedPayload.take(30000),
            reminderEpochMillis = note.reminderEpochMillis.coerceAtLeast(0L),
            reminderLabel = note.reminderLabel.take(100),
            versionHistory = boundedHistory,
            createdAt = note.createdAt ?: now,
            updatedAt = now
        )
    }

    fun updateNote(note: Note): Result<Unit> {
        val now = Timestamp(Date())
        val updatedNote = sanitizeNoteForUpdate(note, now)
        val updatedList = sortNotes(
            _notesSnapshotFlow.value.notes.map { existing ->
                if (existing.id == note.id) updatedNote else existing
            }
        )
        saveNotesToPrefs(updatedList)
        _notesSnapshotFlow.value = NoteSyncSnapshot(
            notes = updatedList,
            hasPendingWrites = false,
            isFromCache = true
        )
        return Result.success(Unit)
    }

    fun updateNotesBatch(notes: List<Note>): Result<Unit> {
        if (notes.isEmpty()) return Result.success(Unit)
        val now = Timestamp(Date())
        val updatesById = notes.associate { it.id to sanitizeNoteForUpdate(it, now) }
        val updatedList = sortNotes(
            _notesSnapshotFlow.value.notes.map { existing ->
                updatesById[existing.id] ?: existing
            }
        )
        saveNotesToPrefs(updatedList)
        _notesSnapshotFlow.value = NoteSyncSnapshot(
            notes = updatedList,
            hasPendingWrites = false,
            isFromCache = true
        )
        return Result.success(Unit)
    }

    fun deleteNote(noteId: String): Result<Unit> {
        val updatedList = _notesSnapshotFlow.value.notes.filterNot { it.id == noteId }
        saveNotesToPrefs(updatedList)
        _notesSnapshotFlow.value = NoteSyncSnapshot(
            notes = updatedList,
            hasPendingWrites = false,
            isFromCache = true
        )
        return Result.success(Unit)
    }

    fun deleteNotesBatch(noteIds: Set<String>): Result<Unit> {
        if (noteIds.isEmpty()) return Result.success(Unit)
        val updatedList = _notesSnapshotFlow.value.notes.filterNot { noteIds.contains(it.id) }
        saveNotesToPrefs(updatedList)
        _notesSnapshotFlow.value = NoteSyncSnapshot(
            notes = updatedList,
            hasPendingWrites = false,
            isFromCache = true
        )
        return Result.success(Unit)
    }

    fun createFolder(
        name: String,
        parentFolderId: String = "",
        colorHex: String = "#F59E0B"
    ): Result<String> {
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Folder name cannot be blank"))
        }
        val now = Timestamp(Date())
        val folderId = "local_folder_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val newFolder = Folder(
            id = folderId,
            userId = LOCAL_USER_ID,
            name = cleanName,
            parentFolderId = parentFolderId.trim().take(128),
            colorHex = colorHex.ifBlank { "#F59E0B" }.take(20),
            createdAt = now,
            updatedAt = now
        )
        val updated = (_foldersFlow.value + newFolder).sortedBy { it.name.lowercase() }
        saveFoldersToPrefs(updated)
        _foldersFlow.value = updated
        return Result.success(folderId)
    }

    fun updateFolder(folder: Folder): Result<Unit> {
        val now = Timestamp(Date())
        val updatedFolder = folder.copy(
            name = folder.name.trim().ifBlank { "Untitled Folder" }.take(60),
            parentFolderId = folder.parentFolderId.trim().take(128),
            colorHex = folder.colorHex.ifBlank { "#F59E0B" }.take(20),
            createdAt = folder.createdAt ?: now,
            updatedAt = now
        )
        val updated = _foldersFlow.value
            .map { if (it.id == folder.id) updatedFolder else it }
            .sortedBy { it.name.lowercase() }
        saveFoldersToPrefs(updated)
        _foldersFlow.value = updated
        return Result.success(Unit)
    }

    fun deleteFolder(folderId: String): Result<Unit> {
        val updated = _foldersFlow.value.filterNot { it.id == folderId }
        saveFoldersToPrefs(updated)
        _foldersFlow.value = updated
        return Result.success(Unit)
    }

    fun createCategory(name: String, colorHex: String = "#F59E0B"): Result<String> {
        val cleanName = name.trim().take(40)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Category name cannot be blank"))
        }
        val now = Timestamp(Date())
        val categoryId = "local_cat_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val newCategory = Category(
            id = categoryId,
            userId = LOCAL_USER_ID,
            name = cleanName,
            colorHex = colorHex.take(20),
            createdAt = now
        )
        val updated = (_categoriesFlow.value + newCategory).sortedBy { it.name.lowercase() }
        saveCategoriesToPrefs(updated)
        _categoriesFlow.value = updated
        return Result.success(categoryId)
    }

    fun deleteCategory(categoryId: String): Result<Unit> {
        val updated = _categoriesFlow.value.filterNot { it.id == categoryId }
        saveCategoriesToPrefs(updated)
        _categoriesFlow.value = updated
        return Result.success(Unit)
    }

    private fun sortNotes(notes: List<Note>): List<Note> {
        return notes.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
        )
    }

    private fun loadNotesFromPrefs(): List<Note> {
        val raw = prefs.getString(KEY_NOTES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Note>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                val tags = mutableListOf<String>()
                for (j in 0 until tagsArr.length()) {
                    tags.add(tagsArr.getString(j))
                }
                val historyArr = obj.optJSONArray("versionHistory") ?: JSONArray()
                val history = mutableListOf<String>()
                for (k in 0 until historyArr.length()) {
                    history.add(historyArr.getString(k))
                }
                val createdSec = obj.optLong("createdAtSec", System.currentTimeMillis() / 1000)
                val updatedSec = obj.optLong("updatedAtSec", createdSec)
                result.add(
                    Note(
                        id = obj.optString("id"),
                        userId = obj.optString("userId", LOCAL_USER_ID),
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        folderId = obj.optString("folderId", ""),
                        category = obj.optString("category", "General"),
                        tags = tags,
                        colorHex = obj.optString("colorHex", "#1E293B"),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isTrashed = obj.optBoolean("isTrashed", false),
                        trashedAtEpochMillis = obj.optLong("trashedAtEpochMillis", 0L),
                        isLocked = obj.optBoolean("isLocked", false),
                        passwordHash = obj.optString("passwordHash", ""),
                        encryptedPayload = obj.optString("encryptedPayload", ""),
                        reminderEpochMillis = obj.optLong("reminderEpochMillis", 0L),
                        reminderLabel = obj.optString("reminderLabel", ""),
                        versionHistory = history,
                        createdAt = Timestamp(createdSec, 0),
                        updatedAt = Timestamp(updatedSec, 0)
                    )
                )
            }
            sortNotes(result)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveNotesToPrefs(notes: List<Note>) {
        val arr = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("userId", note.userId)
                put("title", note.title)
                put("content", note.content)
                put("folderId", note.folderId)
                put("category", note.category)
                put("tags", JSONArray(note.tags))
                put("colorHex", note.colorHex)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("isTrashed", note.isTrashed)
                put("trashedAtEpochMillis", note.trashedAtEpochMillis)
                put("isLocked", note.isLocked)
                put("passwordHash", note.passwordHash)
                put("encryptedPayload", note.encryptedPayload)
                put("reminderEpochMillis", note.reminderEpochMillis)
                put("reminderLabel", note.reminderLabel)
                put("versionHistory", JSONArray(note.versionHistory))
                put("createdAtSec", note.createdAt?.seconds ?: (System.currentTimeMillis() / 1000))
                put("updatedAtSec", note.updatedAt?.seconds ?: (System.currentTimeMillis() / 1000))
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_NOTES_JSON, arr.toString()).apply()
    }

    private fun loadFoldersFromPrefs(): List<Folder> {
        val raw = prefs.getString(KEY_FOLDERS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Folder>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val createdSec = obj.optLong("createdAtSec", System.currentTimeMillis() / 1000)
                val updatedSec = obj.optLong("updatedAtSec", createdSec)
                result.add(
                    Folder(
                        id = obj.optString("id"),
                        userId = obj.optString("userId", LOCAL_USER_ID),
                        name = obj.optString("name"),
                        parentFolderId = obj.optString("parentFolderId", ""),
                        colorHex = obj.optString("colorHex", "#F59E0B"),
                        createdAt = Timestamp(createdSec, 0),
                        updatedAt = Timestamp(updatedSec, 0)
                    )
                )
            }
            result.sortedBy { it.name.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveFoldersToPrefs(folders: List<Folder>) {
        val arr = JSONArray()
        folders.forEach { folder ->
            val obj = JSONObject().apply {
                put("id", folder.id)
                put("userId", folder.userId)
                put("name", folder.name)
                put("parentFolderId", folder.parentFolderId)
                put("colorHex", folder.colorHex)
                put("createdAtSec", folder.createdAt?.seconds ?: (System.currentTimeMillis() / 1000))
                put("updatedAtSec", folder.updatedAt?.seconds ?: (System.currentTimeMillis() / 1000))
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_FOLDERS_JSON, arr.toString()).apply()
    }

    private fun loadCategoriesFromPrefs(): List<Category> {
        val raw = prefs.getString(KEY_CATEGORIES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Category>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val createdSec = obj.optLong("createdAtSec", System.currentTimeMillis() / 1000)
                result.add(
                    Category(
                        id = obj.optString("id"),
                        userId = obj.optString("userId", LOCAL_USER_ID),
                        name = obj.optString("name"),
                        colorHex = obj.optString("colorHex", "#F59E0B"),
                        createdAt = Timestamp(createdSec, 0)
                    )
                )
            }
            result.sortedBy { it.name.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCategoriesToPrefs(categories: List<Category>) {
        val arr = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("userId", cat.userId)
                put("name", cat.name)
                put("colorHex", cat.colorHex)
                put("createdAtSec", cat.createdAt?.seconds ?: (System.currentTimeMillis() / 1000))
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CATEGORIES_JSON, arr.toString()).apply()
    }

    companion object {
        const val LOCAL_USER_ID = "local_guest"
        private const val PREFS_NAME = "noted_local_workspace_store"
        private const val KEY_NOTES_JSON = "local_notes_json"
        private const val KEY_FOLDERS_JSON = "local_folders_json"
        private const val KEY_CATEGORIES_JSON = "local_categories_json"
    }
}
