package com.example.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.CapturedDocumentPhoto
import com.example.data.local.LocalWorkspaceStore
import com.example.data.local.PrivateDocumentVaultStore
import com.example.data.model.Category
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteSyncSnapshot
import com.example.data.model.NoteVersionEntry
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.NoteRepository
import com.example.data.util.NoteVaultCrypto
import com.example.notification.ReminderNotificationHelper
import com.example.widget.NoteAppWidgetProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class NoteViewModel(
    private val repository: NoteRepository,
    private val localStore: LocalWorkspaceStore,
    private val preferencesRepository: UserPreferencesRepository,
    private val db: FirebaseFirestore,
    val currentUserId: String
) : ViewModel() {

    val isLocalGuestMode: Boolean = currentUserId == LocalWorkspaceStore.LOCAL_USER_ID

    val notesState: StateFlow<UiState<NoteSyncSnapshot>> = (
        if (isLocalGuestMode) {
            localStore.notesSnapshotFlow
                .map<NoteSyncSnapshot, UiState<NoteSyncSnapshot>> { UiState.Success(it) }
        } else {
            repository
                .observeNotesWithMetadata(currentUserId)
                .map<NoteSyncSnapshot, UiState<NoteSyncSnapshot>> { UiState.Success(it) }
                .catch { error ->
                    Log.w(TAG, "Error observing notes", error)
                    emit(UiState.Error(error.message ?: "Failed to load notes"))
                }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = if (isLocalGuestMode) {
            UiState.Success(localStore.notesSnapshotFlow.value)
        } else {
            UiState.Loading
        }
    )

    val categoriesState: StateFlow<List<Category>> = (
        if (isLocalGuestMode) {
            localStore.categoriesFlow
        } else {
            repository
                .observeCategories(currentUserId)
                .catch { error ->
                    Log.w(TAG, "Error observing categories", error)
                    emit(emptyList())
                }
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = if (isLocalGuestMode) localStore.categoriesFlow.value else emptyList()
    )

    val preferencesState: StateFlow<UserPreferences> = preferencesRepository
        .preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserPreferences()
        )

    val foldersState: StateFlow<List<Folder>> = combine(
        if (isLocalGuestMode) {
            localStore.foldersFlow
        } else {
            repository
                .observeFolders(currentUserId)
                .catch { error ->
                    Log.w(TAG, "Error observing folders", error)
                    emit(emptyList())
                }
        },
        preferencesState
    ) { rawFolders, prefs ->
        val orderMap = prefs.folderOrderIds.withIndex().associate { (idx, id) -> id to idx }
        rawFolders.sortedWith(
            compareBy<Folder> { orderMap[it.id] ?: Int.MAX_VALUE }
                .thenBy { it.name.lowercase() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = if (isLocalGuestMode) localStore.foldersFlow.value else emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _unlockedNotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val unlockedNotes: StateFlow<Map<String, String>> = _unlockedNotes.asStateFlow()

    private val _unlockedNotePasswords = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _statusUndoNote = MutableStateFlow<Note?>(null)
    val statusUndoNote: StateFlow<Note?> = _statusUndoNote.asStateFlow()

    private val _pinnedNotificationNotes = MutableStateFlow<Set<String>>(emptySet())
    val pinnedNotificationNotes: StateFlow<Set<String>> = _pinnedNotificationNotes.asStateFlow()

    init {
        if (!isLocalGuestMode) {
            migrateLocalGuestDataToCloudIfNeeded()
        }
    }

    private fun migrateLocalGuestDataToCloudIfNeeded() {
        val localNotes = localStore.notesSnapshotFlow.value.notes
        val localFolders = localStore.foldersFlow.value
        val localCategories = localStore.categoriesFlow.value
        if (localNotes.isEmpty() && localFolders.isEmpty() && localCategories.isEmpty()) return

        viewModelScope.launch {
            try {
                // 1. Migrate custom categories
                localCategories.forEach { cat ->
                    repository.createCategory(cat.name, cat.colorHex).onSuccess {
                        localStore.deleteCategory(cat.id)
                    }
                }

                // 2. Migrate folders (root folders first, then nested folders, mapping old IDs to new cloud IDs)
                val folderIdMap = mutableMapOf<String, String>()
                val remainingFolders = localFolders.toMutableList()
                var progress = true
                while (remainingFolders.isNotEmpty() && progress) {
                    progress = false
                    val iterator = remainingFolders.iterator()
                    while (iterator.hasNext()) {
                        val folder = iterator.next()
                        val parentReady = folder.parentFolderId.isBlank() || folderIdMap.containsKey(folder.parentFolderId)
                        if (parentReady) {
                            val mappedParentId = if (folder.parentFolderId.isBlank()) "" else folderIdMap[folder.parentFolderId].orEmpty()
                            val createRes = repository.createFolder(
                                name = folder.name,
                                parentFolderId = mappedParentId,
                                colorHex = folder.colorHex
                            )
                            createRes.onSuccess { cloudFolderId ->
                                folderIdMap[folder.id] = cloudFolderId
                                localStore.deleteFolder(folder.id)
                                iterator.remove()
                                progress = true
                            }
                        }
                    }
                }

                // 3. Migrate notes with mapped folder IDs
                var migratedNotesCount = 0
                localNotes.forEach { note ->
                    val mappedFolderId = if (note.folderId.isBlank()) {
                        ""
                    } else {
                        folderIdMap[note.folderId] ?: ""
                    }
                    val createRes = repository.createNote(
                        title = note.title,
                        content = note.content,
                        folderId = mappedFolderId,
                        category = note.category,
                        tags = note.tags,
                        colorHex = note.colorHex,
                        isPinned = note.isPinned,
                        isArchived = note.isArchived,
                        isTrashed = note.isTrashed,
                        trashedAtEpochMillis = note.trashedAtEpochMillis,
                        isLocked = note.isLocked,
                        passwordHash = note.passwordHash,
                        encryptedPayload = note.encryptedPayload,
                        reminderEpochMillis = note.reminderEpochMillis,
                        reminderLabel = note.reminderLabel,
                        versionHistory = note.versionHistory
                    )
                    createRes.onSuccess {
                        localStore.deleteNote(note.id)
                        migratedNotesCount++
                    }
                }

                if (migratedNotesCount > 0 || folderIdMap.isNotEmpty()) {
                    _statusBanner.value = "Synced $migratedNotesCount local note(s) and ${folderIdMap.size} folder(s) to your Google Cloud account."
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to migrate local guest data to cloud", e)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSelectedFolderId(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
        _statusUndoNote.value = null
    }

    fun postStatusMessage(message: String) {
        _statusUndoNote.value = null
        _statusBanner.value = message
    }

    fun saveDocumentCameraCaptureNote(
        folderId: String,
        keptPhotos: List<CapturedDocumentPhoto>,
        vaultStore: PrivateDocumentVaultStore,
        manualTitle: String = "",
        manualContent: String = "",
        onComplete: (String?) -> Unit = {}
    ) {
        if (keptPhotos.isEmpty()) {
            onComplete(null)
            return
        }
        viewModelScope.launch {
            val count = keptPhotos.size
            val finalTitle = manualTitle.trim()
            val storedContent = manualContent.trim()
            val finalCategory = "General"
            val tags = emptyList<String>()
            val initialVersion = NoteVersionEntry(
                savedAtMillis = System.currentTimeMillis(),
                title = finalTitle,
                content = storedContent,
                category = finalCategory
            ).toSerializedString()

            val res = if (isLocalGuestMode) {
                localStore.createNote(
                    title = finalTitle,
                    content = storedContent,
                    folderId = folderId,
                    category = finalCategory,
                    tags = tags,
                    colorHex = "#E0F2FE",
                    isPinned = false,
                    isArchived = false,
                    isLocked = false,
                    passwordHash = "",
                    encryptedPayload = "",
                    reminderEpochMillis = 0L,
                    reminderLabel = "",
                    versionHistory = listOf(initialVersion)
                )
            } else {
                repository.createNote(
                    title = finalTitle,
                    content = storedContent,
                    folderId = folderId,
                    category = finalCategory,
                    tags = tags,
                    colorHex = "#E0F2FE",
                    isPinned = false,
                    isArchived = false,
                    isLocked = false,
                    passwordHash = "",
                    encryptedPayload = "",
                    reminderEpochMillis = 0L,
                    reminderLabel = "",
                    versionHistory = listOf(initialVersion)
                )
            }

            res.onSuccess { newNoteId ->
                vaultStore.linkPhotosToNote(keptPhotos.map { it.id }, newNoteId)
                _statusUndoNote.value = null
                _statusBanner.value = "Saved $count private document picture(s) • Tap note anytime to add or edit text."
                onComplete(newNoteId)
            }.onFailure { err ->
                _statusUndoNote.value = null
                _statusBanner.value = err.localizedMessage ?: "Saved pictures to Private Camera Vault."
                onComplete(null)
            }
        }
    }

    fun undoLastStatusAction() {
        val previousNote = _statusUndoNote.value ?: run {
            clearStatusBanner()
            return
        }
        _statusUndoNote.value = null
        _statusBanner.value = null
        viewModelScope.launch {
            val res = if (isLocalGuestMode) {
                localStore.updateNote(previousNote)
            } else {
                repository.updateNote(previousNote)
            }
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to undo archive action"
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun setGridView(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setGridView(enabled)
        }
    }

    fun toggleOfflineMode(enableOffline: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSimulateOfflineMode(enableOffline)
            if (isLocalGuestMode) {
                _statusBanner.value = "Using Local Mode without sign-in: all notes and folders are saved on your device."
            } else if (enableOffline) {
                db.disableNetwork()
                _statusBanner.value = "Offline mode active: changes are saved to local cache and will sync automatically when online."
            } else {
                db.enableNetwork()
                _statusBanner.value = "Cloud synchronization active: syncing cached changes with Firestore."
            }
        }
    }

    fun unlockSensitiveNote(note: Note, passwordAttempt: String): Boolean {
        if (!note.isLocked) return true
        val matchesPassword = NoteVaultCrypto.verifyPassword(passwordAttempt, note.passwordHash)
        if (!matchesPassword) {
            return false
        }
        val decrypted = if (note.encryptedPayload.isNotBlank()) {
            NoteVaultCrypto.decryptContent(note.encryptedPayload, passwordAttempt)
                .getOrDefault(note.content)
        } else {
            note.content
        }
        _unlockedNotes.value = _unlockedNotes.value + (note.id to decrypted)
        _unlockedNotePasswords.value = _unlockedNotePasswords.value + (note.id to passwordAttempt)
        return true
    }

    fun lockNoteInSession(noteId: String) {
        _unlockedNotes.value = _unlockedNotes.value - noteId
        _unlockedNotePasswords.value = _unlockedNotePasswords.value - noteId
    }

    fun saveNote(
        context: Context,
        existingNote: Note?,
        title: String,
        plainContent: String,
        folderId: String,
        category: String,
        tags: List<String>,
        colorHex: String,
        isPinned: Boolean,
        isArchived: Boolean = existingNote?.isArchived ?: false,
        isLocked: Boolean,
        newPassword: String,
        reminderEpochMillis: Long,
        reminderLabel: String,
        draftNoteId: String = "",
        pushPermanentNotification: Boolean = false,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val passwordHash: String
            val encryptedPayload: String
            val storedContent: String
            var activePasswordForSession = newPassword

            if (isLocked) {
                if (newPassword.isNotBlank()) {
                    passwordHash = NoteVaultCrypto.hashPassword(newPassword)
                    encryptedPayload = NoteVaultCrypto.encryptContent(plainContent, newPassword)
                    storedContent = "[Encrypted Vault Note]"
                    preferencesRepository.setMasterVaultPasswordHash(passwordHash)
                } else if (existingNote != null && existingNote.isLocked && existingNote.passwordHash.isNotBlank()) {
                    passwordHash = existingNote.passwordHash
                    val sessionPassword = _unlockedNotePasswords.value[existingNote.id]
                        ?.takeIf { it.isNotBlank() }
                        ?: NoteVaultCrypto.MASTER_PASSWORD
                    activePasswordForSession = sessionPassword
                    encryptedPayload = NoteVaultCrypto.encryptContent(plainContent, sessionPassword)
                    storedContent = "[Encrypted Vault Note]"
                } else {
                    val masterPwd = NoteVaultCrypto.MASTER_PASSWORD
                    activePasswordForSession = masterPwd
                    passwordHash = NoteVaultCrypto.hashPassword(masterPwd)
                    encryptedPayload = NoteVaultCrypto.encryptContent(plainContent, masterPwd)
                    storedContent = "[Encrypted Vault Note]"
                    preferencesRepository.setMasterVaultPasswordHash(passwordHash)
                }
            } else {
                passwordHash = ""
                encryptedPayload = ""
                storedContent = plainContent
            }

            val finalTitle = title.trim()
            val finalCategory = category.ifBlank { "General" }

            val result = if (existingNote == null || existingNote.id.isBlank()) {
                val initialVersion = NoteVersionEntry(
                    savedAtMillis = System.currentTimeMillis(),
                    title = finalTitle,
                    content = storedContent,
                    category = finalCategory
                ).toSerializedString()

                if (isLocalGuestMode) {
                    localStore.createNote(
                        title = finalTitle,
                        content = storedContent,
                        folderId = folderId,
                        category = finalCategory,
                        tags = tags,
                        colorHex = colorHex,
                        isPinned = isPinned,
                        isArchived = isArchived,
                        isLocked = isLocked,
                        passwordHash = passwordHash,
                        encryptedPayload = encryptedPayload,
                        reminderEpochMillis = reminderEpochMillis,
                        reminderLabel = reminderLabel,
                        versionHistory = listOf(initialVersion)
                    )
                } else {
                    repository.createNote(
                        title = finalTitle,
                        content = storedContent,
                        folderId = folderId,
                        category = finalCategory,
                        tags = tags,
                        colorHex = colorHex,
                        isPinned = isPinned,
                        isArchived = isArchived,
                        isLocked = isLocked,
                        passwordHash = passwordHash,
                        encryptedPayload = encryptedPayload,
                        reminderEpochMillis = reminderEpochMillis,
                        reminderLabel = reminderLabel,
                        versionHistory = listOf(initialVersion)
                    )
                }
            } else {
                val updatedHistory = appendVersionSnapshotIfNeeded(
                    existingNote = existingNote,
                    newTitle = finalTitle,
                    newStoredContent = storedContent,
                    newCategory = finalCategory
                )

                val updated = existingNote.copy(
                    title = finalTitle,
                    content = storedContent,
                    folderId = folderId,
                    category = finalCategory,
                    tags = tags,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isLocked = isLocked,
                    passwordHash = passwordHash,
                    encryptedPayload = encryptedPayload,
                    reminderEpochMillis = reminderEpochMillis,
                    reminderLabel = reminderLabel,
                    versionHistory = updatedHistory
                )
                if (isLocalGuestMode) {
                    localStore.updateNote(updated).map { existingNote.id }
                } else {
                    repository.updateNote(updated).map { existingNote.id }
                }
            }

            result.onSuccess { noteId ->
                if ((existingNote == null || existingNote.id.isBlank()) && draftNoteId.isNotBlank() && draftNoteId != noteId) {
                    PrivateDocumentVaultStore.getInstance(context)
                        .relinkPhotosFromOldNoteToNewNote(draftNoteId, noteId)
                }
                if (isLocked) {
                    _unlockedNotes.value = _unlockedNotes.value + (noteId to plainContent)
                    if (activePasswordForSession.isNotBlank()) {
                        _unlockedNotePasswords.value = _unlockedNotePasswords.value + (noteId to activePasswordForSession)
                    }
                } else {
                    _unlockedNotes.value = _unlockedNotes.value - noteId
                    _unlockedNotePasswords.value = _unlockedNotePasswords.value - noteId
                }
                if (reminderEpochMillis > System.currentTimeMillis() && !isArchived) {
                    ReminderNotificationHelper.scheduleNoteReminder(
                        context = context,
                        noteId = noteId,
                        noteTitle = finalTitle,
                        reminderLabel = reminderLabel.ifBlank { if (!isLocked) plainContent.trim() else "" },
                        isLocked = isLocked,
                        triggerAtMillis = reminderEpochMillis
                    )
                } else if (existingNote != null && (reminderEpochMillis == 0L || isArchived)) {
                    ReminderNotificationHelper.cancelNoteReminder(context, noteId)
                }
                if (pushPermanentNotification) {
                    if (draftNoteId.isNotBlank() && draftNoteId != noteId) {
                        ReminderNotificationHelper.cancelPermanentNoteNotification(context, draftNoteId)
                    }
                    ReminderNotificationHelper.showPermanentNoteNotification(
                        context = context,
                        noteId = noteId,
                        noteTitle = finalTitle,
                        noteContent = plainContent,
                        isLocked = false
                    )
                    _pinnedNotificationNotes.value = _pinnedNotificationNotes.value + noteId
                    val noteLabel = finalTitle.ifBlank { "Note" }
                    _statusUndoNote.value = null
                    _statusBanner.value = "Saved & pinned permanent notification for “$noteLabel”."
                }
                onComplete(true)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to save note"
                onComplete(false)
            }
        }
    }

    fun assignNoteToFolder(note: Note, targetFolderId: String) {
        if (note.id.isBlank() || note.createdAt == null) return
        viewModelScope.launch {
            val updated = note.copy(folderId = targetFolderId)
            val res = if (isLocalGuestMode) localStore.updateNote(updated) else repository.updateNote(updated)
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to move note to folder"
            }
        }
    }

    fun togglePin(note: Note) {
        if (note.id.isBlank() || note.createdAt == null) return
        viewModelScope.launch {
            val nextPinned = !note.isPinned
            val updated = note.copy(isPinned = nextPinned)
            val res = if (isLocalGuestMode) localStore.updateNote(updated) else repository.updateNote(updated)
            res.onSuccess {
                _statusBanner.value = if (nextPinned) {
                    "“${note.title.ifBlank { "Note" }}” pinned to top."
                } else {
                    "“${note.title.ifBlank { "Note" }}” unpinned."
                }
            }
        }
    }

    fun copyNoteToClipboard(
        context: Context,
        note: Note,
        isUnlocked: Boolean,
        decryptedContent: String
    ): String {
        if (note.isLocked && !isUnlocked) {
            _statusUndoNote.value = null
            _statusBanner.value = "Unlock “${note.title.ifBlank { "Vault Note" }}” to copy note content."
            return ""
        }
        val clipboardText = formatNoteForClipboard(
            note = note,
            isUnlocked = isUnlocked,
            decryptedContent = decryptedContent
        )
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(
            "Note Content",
            clipboardText
        )
        clipboard?.setPrimaryClip(clip)
        _statusUndoNote.value = null
        _statusBanner.value = "Copied note content to clipboard."
        return clipboardText
    }

    fun togglePermanentNotification(
        context: Context,
        note: Note,
        isUnlocked: Boolean,
        decryptedContent: String
    ) {
        if (note.id.isBlank()) return
        val noteLabel = note.title.ifBlank { "Note" }
        if (_pinnedNotificationNotes.value.contains(note.id)) {
            ReminderNotificationHelper.cancelPermanentNoteNotification(context, note.id)
            _pinnedNotificationNotes.value = _pinnedNotificationNotes.value - note.id
            _statusUndoNote.value = null
            _statusBanner.value = "Removed permanent notification for “$noteLabel”."
        } else {
            ReminderNotificationHelper.showPermanentNoteNotification(
                context = context,
                noteId = note.id,
                noteTitle = note.title,
                noteContent = decryptedContent,
                isLocked = note.isLocked && !isUnlocked
            )
            _pinnedNotificationNotes.value = _pinnedNotificationNotes.value + note.id
            _statusUndoNote.value = null
            _statusBanner.value = "Permanent notification pinned for “$noteLabel”."
        }
    }

    fun toggleArchive(note: Note) {
        if (note.id.isBlank() || note.createdAt == null) return
        viewModelScope.launch {
            val nextArchived = !note.isArchived
            val updated = note.copy(
                isArchived = nextArchived,
                isPinned = if (nextArchived) false else note.isPinned,
                isTrashed = false,
                trashedAtEpochMillis = 0L
            )
            val res = if (isLocalGuestMode) localStore.updateNote(updated) else repository.updateNote(updated)
            res.onSuccess {
                _statusUndoNote.value = note
                _statusBanner.value = if (nextArchived) {
                    "“${note.title.ifBlank { "Note" }}” moved to Archive."
                } else {
                    "“${note.title.ifBlank { "Note" }}” restored from Archive."
                }
            }.onFailure { err ->
                _statusUndoNote.value = null
                _statusBanner.value = err.localizedMessage ?: "Failed to update archive status"
            }
        }
    }

    fun moveToTrash(context: Context, note: Note) {
        if (note.id.isBlank() || note.createdAt == null) return
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNoteReminder(context, note.id)
            val updated = note.copy(
                isTrashed = true,
                trashedAtEpochMillis = System.currentTimeMillis(),
                isPinned = false,
                isArchived = false
            )
            val res = if (isLocalGuestMode) localStore.updateNote(updated) else repository.updateNote(updated)
            res.onSuccess {
                _statusBanner.value = "“${note.title.ifBlank { "Note" }}” moved to Trash. Filter by 'Trash' to restore or delete forever."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to move note to Trash"
            }
        }
    }

    fun restoreFromTrash(note: Note) {
        if (note.id.isBlank() || note.createdAt == null) return
        viewModelScope.launch {
            val updated = note.copy(
                isTrashed = false,
                trashedAtEpochMillis = 0L
            )
            val res = if (isLocalGuestMode) localStore.updateNote(updated) else repository.updateNote(updated)
            res.onSuccess {
                _statusBanner.value = "“${note.title.ifBlank { "Note" }}” restored to your notes."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to restore note"
            }
        }
    }

    fun permanentlyDeleteNote(context: Context, noteId: String) {
        if (noteId.isBlank()) return
        viewModelScope.launch {
            ReminderNotificationHelper.cancelNoteReminder(context, noteId)
            val res = if (isLocalGuestMode) localStore.deleteNote(noteId) else repository.deleteNote(noteId)
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to permanently delete note"
            }
        }
    }

    fun deleteNote(context: Context, noteId: String) {
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val target = currentNotes.find { it.id == noteId }
        if (target != null && !target.isTrashed) {
            moveToTrash(context, target)
        } else {
            permanentlyDeleteNote(context, noteId)
        }
    }

    fun emptyTrash(context: Context) {
        val trashedNotes = (notesState.value as? UiState.Success)?.data?.notes
            .orEmpty()
            .filter { it.isTrashed }
        if (trashedNotes.isEmpty()) return
        viewModelScope.launch {
            trashedNotes.forEach { note ->
                ReminderNotificationHelper.cancelNoteReminder(context, note.id)
            }
            if (isLocalGuestMode) {
                localStore.deleteNotesBatch(trashedNotes.map { it.id }.toSet())
            } else {
                trashedNotes.forEach { repository.deleteNote(it.id) }
            }
            _statusBanner.value = "Emptied Trash (${trashedNotes.size} note(s) permanently deleted)."
        }
    }

    fun bulkMoveNotesToFolder(noteIds: Set<String>, targetFolderId: String) {
        if (noteIds.isEmpty()) return
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val targets = currentNotes.filter { noteIds.contains(it.id) && it.createdAt != null }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val updatedTargets = targets.map { it.copy(folderId = targetFolderId) }
            if (isLocalGuestMode) {
                localStore.updateNotesBatch(updatedTargets)
            } else {
                updatedTargets.forEach { repository.updateNote(it) }
            }
            val folderName = foldersState.value.find { it.id == targetFolderId }?.name
            _statusUndoNote.value = null
            _statusBanner.value = if (targetFolderId.isBlank() || folderName == null) {
                "Moved ${targets.size} note(s) to Unfiled."
            } else {
                "Moved ${targets.size} note(s) to folder “$folderName”."
            }
        }
    }

    fun bulkChangeCategory(noteIds: Set<String>, targetCategory: String) {
        val cleanCategory = targetCategory.trim()
        if (noteIds.isEmpty() || cleanCategory.isBlank()) return
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val targets = currentNotes.filter { noteIds.contains(it.id) && it.createdAt != null }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val updatedTargets = targets.map { it.copy(category = cleanCategory) }
            if (isLocalGuestMode) {
                localStore.updateNotesBatch(updatedTargets)
            } else {
                updatedTargets.forEach { repository.updateNote(it) }
            }
            _statusUndoNote.value = null
            _statusBanner.value = "Assigned ${targets.size} note(s) to category “$cleanCategory”."
        }
    }

    fun bulkTogglePin(noteIds: Set<String>) {
        if (noteIds.isEmpty()) return
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val targets = currentNotes.filter { noteIds.contains(it.id) && !it.isTrashed && !it.isArchived && it.createdAt != null }
        if (targets.isEmpty()) return
        val shouldPin = targets.any { !it.isPinned }
        viewModelScope.launch {
            val updatedTargets = targets.map { it.copy(isPinned = shouldPin) }
            if (isLocalGuestMode) {
                localStore.updateNotesBatch(updatedTargets)
            } else {
                updatedTargets.forEach { repository.updateNote(it) }
            }
            _statusUndoNote.value = null
            _statusBanner.value = if (shouldPin) {
                "Pinned ${targets.size} note(s)."
            } else {
                "Unpinned ${targets.size} note(s)."
            }
        }
    }

    fun bulkArchiveNotes(noteIds: Set<String>, archive: Boolean) {
        if (noteIds.isEmpty()) return
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val targets = currentNotes.filter { noteIds.contains(it.id) && !it.isTrashed && it.createdAt != null }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val updatedTargets = targets.map { note ->
                note.copy(
                    isArchived = archive,
                    isPinned = if (archive) false else note.isPinned,
                    isTrashed = false,
                    trashedAtEpochMillis = 0L
                )
            }
            if (isLocalGuestMode) {
                localStore.updateNotesBatch(updatedTargets)
            } else {
                updatedTargets.forEach { repository.updateNote(it) }
            }
            _statusUndoNote.value = null
            _statusBanner.value = if (archive) {
                "Archived ${targets.size} note(s)."
            } else {
                "Unarchived ${targets.size} note(s)."
            }
        }
    }

    fun bulkRestoreNotesFromTrash(noteIds: Set<String>) {
        if (noteIds.isEmpty()) return
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val targets = currentNotes.filter { noteIds.contains(it.id) && it.isTrashed && it.createdAt != null }
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val updatedTargets = targets.map { it.copy(isTrashed = false, trashedAtEpochMillis = 0L) }
            if (isLocalGuestMode) {
                localStore.updateNotesBatch(updatedTargets)
            } else {
                updatedTargets.forEach { repository.updateNote(it) }
            }
            _statusUndoNote.value = null
            _statusBanner.value = "Restored ${targets.size} note(s) from Trash."
        }
    }

    fun bulkDeleteNotes(context: Context, noteIds: Set<String>) {
        if (noteIds.isEmpty()) return
        val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
        val targets = currentNotes.filter { noteIds.contains(it.id) }
        if (targets.isEmpty()) return
        val trashedTargets = targets.filter { it.isTrashed }
        val activeTargets = targets.filter { !it.isTrashed && it.createdAt != null }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedActive = activeTargets.map { note ->
                ReminderNotificationHelper.cancelNoteReminder(context, note.id)
                note.copy(
                    isTrashed = true,
                    trashedAtEpochMillis = now,
                    isPinned = false,
                    isArchived = false
                )
            }
            if (updatedActive.isNotEmpty()) {
                if (isLocalGuestMode) {
                    localStore.updateNotesBatch(updatedActive)
                } else {
                    updatedActive.forEach { repository.updateNote(it) }
                }
            }
            if (trashedTargets.isNotEmpty()) {
                trashedTargets.forEach { note ->
                    ReminderNotificationHelper.cancelNoteReminder(context, note.id)
                }
                if (isLocalGuestMode) {
                    localStore.deleteNotesBatch(trashedTargets.map { it.id }.toSet())
                } else {
                    trashedTargets.forEach { repository.deleteNote(it.id) }
                }
            }
            _statusUndoNote.value = null
            _statusBanner.value = when {
                activeTargets.isNotEmpty() && trashedTargets.isNotEmpty() ->
                    "Moved ${activeTargets.size} note(s) to Trash and permanently deleted ${trashedTargets.size} note(s)."
                trashedTargets.isNotEmpty() ->
                    "Permanently deleted ${trashedTargets.size} note(s)."
                else ->
                    "Moved ${activeTargets.size} note(s) to Trash."
            }
        }
    }

    fun createCategory(name: String, colorHex: String = "#F59E0B") {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val currentHidden = preferencesState.value.hiddenDefaultCategories
            if (currentHidden.any { it.equals(clean, ignoreCase = true) }) {
                preferencesRepository.setHiddenDefaultCategories(
                    currentHidden.filterNot { it.equals(clean, ignoreCase = true) }
                )
            }
            val alreadyExists = categoriesState.value.any { it.name.equals(clean, ignoreCase = true) }
            if (!alreadyExists) {
                val res = if (isLocalGuestMode) {
                    localStore.createCategory(clean, colorHex)
                } else {
                    repository.createCategory(clean, colorHex)
                }
                res.onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to create category"
                }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        if (categoryId.isBlank()) return
        viewModelScope.launch {
            if (isLocalGuestMode) localStore.deleteCategory(categoryId) else repository.deleteCategory(categoryId)
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        val cleanNew = newName.trim()
        val cleanOld = oldName.trim()
        if (cleanNew.isBlank() || cleanOld.isBlank() || cleanNew.equals(cleanOld, ignoreCase = true)) return
        viewModelScope.launch {
            val matchingCats = categoriesState.value.filter { it.name.equals(cleanOld, ignoreCase = true) }
            val preservedColor = matchingCats.firstOrNull()?.colorHex ?: "#F59E0B"
            matchingCats.forEach { cat ->
                if (isLocalGuestMode) localStore.deleteCategory(cat.id) else repository.deleteCategory(cat.id)
            }

            if (categoriesState.value.none { it.name.equals(cleanNew, ignoreCase = true) }) {
                if (isLocalGuestMode) {
                    localStore.createCategory(cleanNew, preservedColor)
                } else {
                    repository.createCategory(cleanNew, preservedColor)
                }
            }

            val defaultCats = listOf("General", "Work", "Personal", "Ideas")
            var updatedHidden = preferencesState.value.hiddenDefaultCategories
            if (defaultCats.any { it.equals(cleanOld, ignoreCase = true) }) {
                updatedHidden = (updatedHidden + cleanOld).distinct()
            }
            updatedHidden = updatedHidden.filterNot { it.equals(cleanNew, ignoreCase = true) }
            preferencesRepository.setHiddenDefaultCategories(updatedHidden)

            val currentOrder = preferencesState.value.categoryOrderNames
            if (currentOrder.any { it.equals(cleanOld, ignoreCase = true) }) {
                preferencesRepository.setCategoryOrderNames(
                    currentOrder.map { if (it.equals(cleanOld, ignoreCase = true)) cleanNew else it }.distinct()
                )
            }

            val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
            val updatedCategoryNotes = currentNotes
                .filter { it.category.equals(cleanOld, ignoreCase = true) && it.createdAt != null }
                .map { it.copy(category = cleanNew) }
            if (updatedCategoryNotes.isNotEmpty()) {
                if (isLocalGuestMode) {
                    localStore.updateNotesBatch(updatedCategoryNotes)
                } else {
                    updatedCategoryNotes.forEach { repository.updateNote(it) }
                }
            }

            if (_selectedFilter.value.equals(cleanOld, ignoreCase = true)) {
                _selectedFilter.value = cleanNew
            }
        }
    }

    fun deleteCategoryByName(categoryName: String) {
        val clean = categoryName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val matchingCats = categoriesState.value.filter { it.name.equals(clean, ignoreCase = true) }
            matchingCats.forEach { cat ->
                if (isLocalGuestMode) localStore.deleteCategory(cat.id) else repository.deleteCategory(cat.id)
            }

            val defaultCats = listOf("General", "Work", "Personal", "Ideas")
            if (defaultCats.any { it.equals(clean, ignoreCase = true) }) {
                val updatedHidden = (preferencesState.value.hiddenDefaultCategories + clean).distinct()
                preferencesRepository.setHiddenDefaultCategories(updatedHidden)
            }

            val currentOrder = preferencesState.value.categoryOrderNames
            if (currentOrder.any { it.equals(clean, ignoreCase = true) }) {
                preferencesRepository.setCategoryOrderNames(
                    currentOrder.filterNot { it.equals(clean, ignoreCase = true) }
                )
            }

            val fallbackCategory = if (clean.equals("General", ignoreCase = true)) "Personal" else "General"
            val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()
            val fallbackNotes = currentNotes
                .filter { it.category.equals(clean, ignoreCase = true) && it.createdAt != null }
                .map { it.copy(category = fallbackCategory) }
            if (fallbackNotes.isNotEmpty()) {
                if (isLocalGuestMode) {
                    localStore.updateNotesBatch(fallbackNotes)
                } else {
                    fallbackNotes.forEach { repository.updateNote(it) }
                }
            }

            if (_selectedFilter.value.equals(clean, ignoreCase = true)) {
                _selectedFilter.value = "All"
            }
        }
    }

    fun reorderFolder(folderId: String, direction: Int, currentOrderedFolders: List<Folder> = foldersState.value) {
        val ids = currentOrderedFolders.map { it.id }.toMutableList()
        val index = ids.indexOf(folderId)
        if (index == -1) return
        val targetIndex = (index + direction).coerceIn(0, ids.lastIndex)
        if (targetIndex == index) return
        val item = ids.removeAt(index)
        ids.add(targetIndex, item)
        viewModelScope.launch {
            preferencesRepository.setFolderOrderIds(ids)
        }
    }

    fun reorderCategory(categoryName: String, direction: Int, currentOrderedCategories: List<String>) {
        val names = currentOrderedCategories.toMutableList()
        val index = names.indexOfFirst { it.equals(categoryName, ignoreCase = true) }
        if (index == -1) return
        val targetIndex = (index + direction).coerceIn(0, names.lastIndex)
        if (targetIndex == index) return
        val item = names.removeAt(index)
        names.add(targetIndex, item)
        viewModelScope.launch {
            preferencesRepository.setCategoryOrderNames(names)
        }
    }

    fun createFolder(
        name: String,
        parentFolderId: String = "",
        colorHex: String = "#F59E0B",
        onCreated: (String) -> Unit = {}
    ) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val res = if (isLocalGuestMode) {
                localStore.createFolder(clean, parentFolderId, colorHex)
            } else {
                repository.createFolder(clean, parentFolderId, colorHex)
            }
            res.onSuccess { newId ->
                onCreated(newId)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to create folder"
            }
        }
    }

    fun renameFolder(folder: Folder, newName: String, newColorHex: String = folder.colorHex) {
        val clean = newName.trim()
        if (folder.id.isBlank() || folder.createdAt == null || clean.isBlank()) return
        viewModelScope.launch {
            val updated = folder.copy(name = clean, colorHex = newColorHex)
            val res = if (isLocalGuestMode) localStore.updateFolder(updated) else repository.updateFolder(updated)
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to rename folder"
            }
        }
    }

    fun moveFolder(folder: Folder, newParentFolderId: String) {
        if (folder.id.isBlank() || folder.createdAt == null) return
        if (folder.id == newParentFolderId) {
            _statusBanner.value = "A folder cannot be moved inside itself."
            return
        }
        val allFolders = foldersState.value
        if (isDescendantFolder(targetParentId = newParentFolderId, sourceFolderId = folder.id, allFolders = allFolders)) {
            _statusBanner.value = "Cannot move a folder into one of its own subfolders."
            return
        }
        viewModelScope.launch {
            val updated = folder.copy(parentFolderId = newParentFolderId)
            val res = if (isLocalGuestMode) localStore.updateFolder(updated) else repository.updateFolder(updated)
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to move folder"
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        if (folder.id.isBlank()) return
        viewModelScope.launch {
            val allFolders = foldersState.value
            val currentNotes = (notesState.value as? UiState.Success)?.data?.notes.orEmpty()

            allFolders.filter { it.parentFolderId == folder.id && it.createdAt != null }.forEach { child ->
                val updatedChild = child.copy(parentFolderId = folder.parentFolderId)
                if (isLocalGuestMode) localStore.updateFolder(updatedChild) else repository.updateFolder(updatedChild)
            }

            currentNotes.filter { it.folderId == folder.id && it.createdAt != null }.forEach { note ->
                val updatedNote = note.copy(folderId = folder.parentFolderId)
                if (isLocalGuestMode) localStore.updateNote(updatedNote) else repository.updateNote(updatedNote)
            }

            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }

            val res = if (isLocalGuestMode) localStore.deleteFolder(folder.id) else repository.deleteFolder(folder.id)
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to delete folder"
            }
        }
    }

    fun syncHomeWidget(context: Context, snapshot: NoteSyncSnapshot) {
        val activeNotes = snapshot.notes.filter { !it.isTrashed && !it.isArchived }
        NoteAppWidgetProvider.syncWidgetCache(
            context = context,
            notes = activeNotes,
            isOfflineOrPending = snapshot.isFromCache || snapshot.hasPendingWrites
        )
    }

    companion object {
        private const val TAG = "NoteViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        internal fun appendVersionSnapshotIfNeeded(
            existingNote: Note,
            newTitle: String,
            newStoredContent: String,
            newCategory: String
        ): List<String> {
            val hasChanged = existingNote.title != newTitle ||
                existingNote.content != newStoredContent ||
                existingNote.category != newCategory

            if (!hasChanged && existingNote.versionHistory.isNotEmpty()) {
                return existingNote.versionHistory.take(10)
            }

            val previousTimestampMillis = ((existingNote.updatedAt ?: existingNote.createdAt)?.seconds ?: 0L) * 1000L
            val snapshotEntry = NoteVersionEntry(
                savedAtMillis = if (previousTimestampMillis > 0L) previousTimestampMillis else System.currentTimeMillis() - 1000L,
                title = existingNote.title,
                content = existingNote.content,
                category = existingNote.category
            ).toSerializedString()

            val latestSnapshot = NoteVersionEntry(
                savedAtMillis = System.currentTimeMillis(),
                title = newTitle,
                content = newStoredContent,
                category = newCategory
            ).toSerializedString()

            val existingTop = existingNote.versionHistory.firstOrNull()?.let { NoteVersionEntry.fromSerializedString(it) }
            val shouldIncludePreviousSnapshot = existingTop == null ||
                existingTop.title != existingNote.title ||
                existingTop.content != existingNote.content ||
                existingTop.category != existingNote.category

            val baseHistory = if (shouldIncludePreviousSnapshot) {
                listOf(snapshotEntry) + existingNote.versionHistory
            } else {
                existingNote.versionHistory
            }

            return (listOf(latestSnapshot) + baseHistory).distinct().take(10)
        }

        internal fun formatNoteForClipboard(
            note: Note,
            isUnlocked: Boolean,
            decryptedContent: String
        ): String {
            return if (note.isLocked && !isUnlocked) {
                ""
            } else {
                decryptedContent.trim()
            }
        }

        internal fun isDescendantFolder(
            targetParentId: String,
            sourceFolderId: String,
            allFolders: List<Folder>
        ): Boolean {
            if (targetParentId.isBlank()) return false
            val folderById = allFolders.associateBy { it.id }
            var currentId: String? = targetParentId
            val visited = mutableSetOf<String>()
            while (!currentId.isNullOrBlank() && visited.add(currentId)) {
                if (currentId == sourceFolderId) return true
                currentId = folderById[currentId]?.parentFolderId
            }
            return false
        }

        fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val noteRepository = NoteRepository(db)
                val localStore = LocalWorkspaceStore(app)
                val prefsRepository = UserPreferencesRepository(app)
                NoteViewModel(
                    repository = noteRepository,
                    localStore = localStore,
                    preferencesRepository = prefsRepository,
                    db = db,
                    currentUserId = currentUserId
                )
            }
        }
    }
}
