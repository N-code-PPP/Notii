package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.CapturedDocumentPhoto
import com.example.data.local.PrivateDocumentVaultStore
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteSyncSnapshot
import com.example.data.util.NoteVaultCrypto
import com.example.notification.ReminderNotificationHelper
import com.example.ui.theme.HighlightYellowOutlineDark
import com.example.ui.theme.HighlightYellowOutlineLight
import com.example.ui.theme.NoteColorPalette
import com.example.ui.theme.ProvideSafeTextToolbar
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WorkspaceTab {
    NOTES,
    FOLDERS,
    CALENDAR
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(
    viewModel: NoteViewModel,
    initialQuickAction: String? = null,
    onConsumeQuickAction: () -> Unit = {},
    onRequestSignIn: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val notesUiState by viewModel.notesState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val folders by viewModel.foldersState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val unlockedNotes by viewModel.unlockedNotes.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val statusUndoNote by viewModel.statusUndoNote.collectAsStateWithLifecycle()
    val pinnedNotificationNotes by viewModel.pinnedNotificationNotes.collectAsStateWithLifecycle()

    var editingNote by remember { mutableStateOf<Note?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var editorAutoVoice by remember { mutableStateOf(false) }
    var editorAutoLock by remember { mutableStateOf(false) }
    var editorInitialReminderMillis by remember { mutableLongStateOf(0L) }
    var editorInitialFolderId by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(WorkspaceTab.NOTES) }

    var notePendingUnlock by remember { mutableStateOf<Note?>(null) }
    var unlockPasswordInput by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }

    var notePendingFolderMove by remember { mutableStateOf<Note?>(null) }
    var showQuickCreateFolderDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    var highlightedNoteId by remember { mutableStateOf<String?>(null) }
    var tooltipNoteId by remember { mutableStateOf<String?>(null) }
    var lastCopiedNoteId by remember { mutableStateOf<String?>(null) }
    var pendingPermanentNotificationNote by remember { mutableStateOf<Note?>(null) }

    val permanentNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val targetNote = pendingPermanentNotificationNote
        pendingPermanentNotificationNote = null
        if (granted && targetNote != null) {
            val isUnlockedInSession = !targetNote.isLocked || unlockedNotes.containsKey(targetNote.id)
            val displayContent = if (targetNote.isLocked) {
                unlockedNotes[targetNote.id] ?: ""
            } else {
                targetNote.content
            }
            viewModel.togglePermanentNotification(
                context = context,
                note = targetNote,
                isUnlocked = isUnlockedInSession,
                decryptedContent = displayContent
            )
        }
    }

    var showSyncDialog by remember { mutableStateOf(false) }
    var showWidgetPreviewDialog by remember { mutableStateOf(false) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }
    var showMasterPasswordDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showManageFoldersCategoriesDialog by remember { mutableStateOf(false) }
    var manageDialogTab by remember { mutableIntStateOf(0) }
    var folderPendingRenameInManager by remember { mutableStateOf<Folder?>(null) }
    var categoryPendingRename by remember { mutableStateOf<String?>(null) }
    var renameCategoryInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("") }
    var appIconTapCount by remember { mutableIntStateOf(0) }
    var lastAppIconTapMillis by remember { mutableLongStateOf(0L) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(emptySet<String>()) }
    var showBulkMoveFolderDialog by remember { mutableStateOf(false) }
    var showBulkCategoryDialog by remember { mutableStateOf(false) }

    val cameraVaultStore = remember(context) { PrivateDocumentVaultStore.getInstance(context) }
    val allVaultPhotos by cameraVaultStore.photosFlow.collectAsStateWithLifecycle()
    val notePhotosByNoteId = remember(allVaultPhotos) {
        allVaultPhotos
            .filter { it.noteId.isNotBlank() }
            .groupBy { it.noteId }
            .mapValues { (_, list) ->
                list.sortedWith(compareBy<CapturedDocumentPhoto> { it.pageNumber }.thenBy { it.capturedAtMillis })
            }
    }
    var isQuickCameraOpen by remember { mutableStateOf(false) }
    var noteIdPendingCameraAppend by remember { mutableStateOf<String?>(null) }
    var workspacePhotoPendingEdit by remember { mutableStateOf<CapturedDocumentPhoto?>(null) }
    var showPrivateCameraVaultDialog by remember { mutableStateOf(false) }
    var notePhotosPendingExport by remember { mutableStateOf<Pair<String, List<CapturedDocumentPhoto>>?>(null) }

    val workspaceFocusManager = LocalFocusManager.current
    val workspaceTextToolbar = LocalTextToolbar.current
    val workspaceKeyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isQuickCameraOpen, noteIdPendingCameraAppend, isEditorOpen) {
        if (isQuickCameraOpen || noteIdPendingCameraAppend != null || isEditorOpen) {
            runCatching { workspaceTextToolbar.hide() }
            runCatching { workspaceFocusManager.clearFocus(force = true) }
            runCatching { workspaceKeyboardController?.hide() }
        }
    }

    BackHandler(
        enabled = !isEditorOpen &&
            !isQuickCameraOpen &&
            noteIdPendingCameraAppend == null &&
            (isSelectionMode || tooltipNoteId != null || highlightedNoteId != null)
    ) {
        when {
            isSelectionMode -> {
                isSelectionMode = false
                selectedNoteIds = emptySet()
            }
            tooltipNoteId != null -> {
                tooltipNoteId = null
            }
            else -> {
                highlightedNoteId = null
            }
        }
    }

    LaunchedEffect(initialQuickAction, notesUiState) {
        when {
            initialQuickAction == "QUICK_NOTE" -> {
                editingNote = null
                editorAutoVoice = false
                editorAutoLock = false
                editorInitialReminderMillis = 0L
                editorInitialFolderId = selectedFolderId.orEmpty()
                isEditorOpen = true
                onConsumeQuickAction()
            }
            initialQuickAction == "VOICE_NOTE" -> {
                editingNote = null
                editorAutoVoice = true
                editorAutoLock = false
                editorInitialReminderMillis = 0L
                editorInitialFolderId = selectedFolderId.orEmpty()
                isEditorOpen = true
                onConsumeQuickAction()
            }
            initialQuickAction == "QUICK_CAMERA" -> {
                isQuickCameraOpen = true
                onConsumeQuickAction()
            }
            initialQuickAction != null && initialQuickAction.startsWith("OPEN_NOTE:") -> {
                val targetId = initialQuickAction.removePrefix("OPEN_NOTE:")
                val currentList = (notesUiState as? UiState.Success)?.data?.notes.orEmpty()
                val targetNote = currentList.find { it.id == targetId }
                if (targetNote != null) {
                    val isUnlockedInSession = !targetNote.isLocked || unlockedNotes.containsKey(targetNote.id)
                    if (targetNote.isLocked && !isUnlockedInSession) {
                        notePendingUnlock = targetNote
                        unlockPasswordInput = ""
                        unlockError = null
                    } else {
                        editingNote = targetNote
                        editorAutoVoice = false
                        editorAutoLock = targetNote.isLocked
                        editorInitialReminderMillis = targetNote.reminderEpochMillis
                        editorInitialFolderId = targetNote.folderId
                        isEditorOpen = true
                    }
                    onConsumeQuickAction()
                } else if (targetId == "preview_note") {
                    onConsumeQuickAction()
                }
            }
        }
    }

    LaunchedEffect(notesUiState) {
        val state = notesUiState
        if (state is UiState.Success) {
            viewModel.syncHomeWidget(context, state.data)
        }
    }

    LaunchedEffect(statusBanner, statusUndoNote) {
        if (statusBanner != null) {
            kotlinx.coroutines.delay(if (statusUndoNote != null) 4000L else 2800L)
            viewModel.clearStatusBanner()
        }
    }

    val snapshot = (notesUiState as? UiState.Success)?.data ?: NoteSyncSnapshot()
    val systemSmartFilters = remember {
        listOf("All", "Pinned", "Reminders", "Archive")
    }
    val hiddenDefaultSet = remember(preferences.hiddenDefaultCategories) {
        (preferences.hiddenDefaultCategories.map { it.lowercase() } + listOf("vault", "trash")).toSet()
    }
    val defaultContentCategories = remember(hiddenDefaultSet) {
        listOf("General", "Work", "Personal", "Ideas").filterNot { hiddenDefaultSet.contains(it.lowercase()) }
    }
    val rawAllCategories = remember(categories, snapshot.notes, defaultContentCategories, hiddenDefaultSet) {
        (systemSmartFilters +
            defaultContentCategories +
            categories.map { it.name }.filterNot { hiddenDefaultSet.contains(it.lowercase()) } +
            snapshot.notes.map { it.category }.filterNot { hiddenDefaultSet.contains(it.lowercase()) })
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
    val allCategories = remember(rawAllCategories, preferences.categoryOrderNames) {
        val orderMap = preferences.categoryOrderNames
            .mapIndexed { idx, name -> name.lowercase() to idx }
            .toMap()
        rawAllCategories.sortedWith(
            compareBy<String> { orderMap[it.lowercase()] ?: Int.MAX_VALUE }
        )
    }
    val editableContentCategories = remember(allCategories, systemSmartFilters) {
        val smartLower = systemSmartFilters.map { it.lowercase() }.toSet()
        allCategories.filterNot { smartLower.contains(it.lowercase()) }
    }

    val appendTargetNoteId = noteIdPendingCameraAppend
    if (appendTargetNoteId != null) {
        QuickDocumentCameraScreen(
            vaultStore = cameraVaultStore,
            isAppendingToNote = true,
            onCompleteWithKeptPhotos = { keptPhotos, _, _ ->
                noteIdPendingCameraAppend = null
                if (keptPhotos.isNotEmpty()) {
                    cameraVaultStore.appendPhotosToNote(
                        photoIds = keptPhotos.map { it.id },
                        noteId = appendTargetNoteId
                    )
                    viewModel.postStatusMessage(
                        "Added ${keptPhotos.size} picture(s) to note • Tap any picture to rotate, crop, filter, or edit."
                    )
                }
            },
            onCancelOrClose = {
                noteIdPendingCameraAppend = null
            },
            onStatusMessage = { msg ->
                viewModel.postStatusMessage(msg)
            }
        )
        return
    }

    if (isQuickCameraOpen) {
        QuickDocumentCameraScreen(
            vaultStore = cameraVaultStore,
            onCompleteWithKeptPhotos = { keptPhotos, manualTitle, manualContent ->
                isQuickCameraOpen = false
                if (keptPhotos.isNotEmpty()) {
                    viewModel.saveDocumentCameraCaptureNote(
                        folderId = selectedFolderId.orEmpty(),
                        keptPhotos = keptPhotos,
                        vaultStore = cameraVaultStore,
                        manualTitle = manualTitle,
                        manualContent = manualContent
                    )
                }
            },
            onCancelOrClose = {
                isQuickCameraOpen = false
            },
            onStatusMessage = { msg ->
                viewModel.postStatusMessage(msg)
            }
        )
        return
    }

    if (isEditorOpen) {
        val currentNote = editingNote
        val editorDraftNoteId = remember(currentNote?.id) {
            currentNote?.id?.takeIf { it.isNotBlank() } ?: "draft_note_${System.currentTimeMillis()}"
        }
        val initialPlain = if (currentNote != null && currentNote.isLocked) {
            unlockedNotes[currentNote.id] ?: ""
        } else {
            currentNote?.content ?: ""
        }
        NoteEditorScreen(
            existingNote = currentNote,
            draftNoteId = editorDraftNoteId,
            initialPlainContent = initialPlain,
            availableCategories = editableContentCategories,
            availableFolders = folders,
            initialFolderId = editorInitialFolderId,
            autoStartVoice = editorAutoVoice,
            autoEnableLock = editorAutoLock,
            initialReminderMillis = editorInitialReminderMillis,
            onCreateCategory = { viewModel.createCategory(it) },
            onCreateFolder = { folderName, onCreated ->
                viewModel.createFolder(
                    name = folderName,
                    parentFolderId = "",
                    colorHex = "#F59E0B",
                    onCreated = onCreated
                )
            },
            onSave = { title, plainContent, folderId, category, tags, colorHex, isPinned, isArchived, isLocked, newPassword, reminderMillis, reminderLabel ->
                viewModel.saveNote(
                    context = context,
                    existingNote = currentNote,
                    title = title,
                    plainContent = plainContent,
                    folderId = folderId,
                    category = category,
                    tags = tags,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isLocked = isLocked,
                    newPassword = newPassword,
                    reminderEpochMillis = reminderMillis,
                    reminderLabel = reminderLabel,
                    draftNoteId = editorDraftNoteId,
                    pushPermanentNotification = false,
                    onComplete = { ok ->
                        if (ok) {
                            isEditorOpen = false
                            editingNote = null
                            editorAutoVoice = false
                            editorAutoLock = false
                            editorInitialReminderMillis = 0L
                            editorInitialFolderId = ""
                        }
                    }
                )
            },
            onSaveAndNotify = { title, plainContent, folderId, category, tags, colorHex, isPinned, isArchived, isLocked, newPassword, reminderMillis, reminderLabel ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ReminderNotificationHelper.hasNotificationPermission(context)
                ) {
                    permanentNotificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
                viewModel.saveNote(
                    context = context,
                    existingNote = currentNote,
                    title = title,
                    plainContent = plainContent,
                    folderId = folderId,
                    category = category,
                    tags = tags,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isLocked = isLocked,
                    newPassword = newPassword,
                    reminderEpochMillis = reminderMillis,
                    reminderLabel = reminderLabel,
                    draftNoteId = editorDraftNoteId,
                    pushPermanentNotification = true,
                    onComplete = { ok ->
                        if (ok) {
                            isEditorOpen = false
                            editingNote = null
                            editorAutoVoice = false
                            editorAutoLock = false
                            editorInitialReminderMillis = 0L
                            editorInitialFolderId = ""
                        }
                    }
                )
            },
            onRestoreFromTrash = if (currentNote != null && currentNote.isTrashed) {
                {
                    viewModel.restoreFromTrash(currentNote)
                    isEditorOpen = false
                    editingNote = null
                }
            } else null,
            onDeleteForever = if (currentNote != null && currentNote.id.isNotBlank()) {
                {
                    viewModel.permanentlyDeleteNote(context, currentNote.id)
                    isEditorOpen = false
                    editingNote = null
                }
            } else null,
            onDelete = if (currentNote != null && currentNote.id.isNotBlank()) {
                {
                    viewModel.deleteNote(context, currentNote.id)
                    isEditorOpen = false
                    editingNote = null
                    editorInitialReminderMillis = 0L
                    editorInitialFolderId = ""
                }
            } else null,
            onBack = {
                if (currentNote == null || currentNote.id.isBlank()) {
                    cameraVaultStore.deleteNotePhotos(editorDraftNoteId)
                }
                isEditorOpen = false
                editingNote = null
                editorAutoVoice = false
                editorAutoLock = false
                editorInitialReminderMillis = 0L
                editorInitialFolderId = ""
            }
        )
        return
    }

    val activeWorkspaceNotes = remember(snapshot.notes) {
        snapshot.notes.filter { !it.isTrashed && !it.isArchived }
    }
    val archivedCount = remember(snapshot.notes) {
        snapshot.notes.count { it.isArchived && !it.isTrashed }
    }
    val trashedCount = remember(snapshot.notes) {
        snapshot.notes.count { it.isTrashed }
    }
    val isOfflineOrPending = preferences.simulateOfflineMode || snapshot.isFromCache || snapshot.hasPendingWrites
    val folderById = remember(folders) { folders.associateBy { it.id } }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            editingNote = null
                            editorAutoVoice = false
                            editorAutoLock = true
                            editorInitialReminderMillis = 0L
                            editorInitialFolderId = selectedFolderId.orEmpty()
                            isEditorOpen = true
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("vault_note_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "New password-protected note"
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            isQuickCameraOpen = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("quick_camera_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Quick camera document shot (up to 10 consecutive pictures)"
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        editingNote = null
                        editorAutoVoice = false
                        editorAutoLock = false
                        editorInitialReminderMillis = 0L
                        editorInitialFolderId = selectedFolderId.orEmpty()
                        isEditorOpen = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("new_note_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create quick note"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 640.dp
            val gridColumns = when {
                !preferences.isGridView -> 1
                isWideScreen -> 3
                else -> 2
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Top Workspace Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    val nextCount = if (now - lastAppIconTapMillis > 3000L) 1 else appIconTapCount + 1
                                    lastAppIconTapMillis = now
                                    if (nextCount >= 5) {
                                        appIconTapCount = 0
                                        showMasterPasswordDialog = true
                                    } else {
                                        appIconTapCount = nextCount
                                    }
                                }
                                .testTag("app_icon_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_icon_cropped),
                                contentDescription = "Notii icon — tap 5 times to view Master Password",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Notii",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .widthIn(min = 95.dp, max = 145.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                        .border(
                                            width = 1.dp,
                                            color = if (searchQuery.isNotBlank()) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp)
                                        .testTag("workspace_search_input"),
                                    decorationBox = { innerTextField ->
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search notes",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Search...",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                innerTextField()
                                            }
                                            if (searchQuery.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .clickable { viewModel.setSearchQuery("") }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showSyncDialog = true }
                                    .padding(vertical = 2.dp)
                                    .testTag("sync_status_badge")
                            ) {
                                Icon(
                                    imageVector = when {
                                        viewModel.isLocalGuestMode -> Icons.Default.CloudOff
                                        preferences.simulateOfflineMode -> Icons.Default.CloudOff
                                        snapshot.hasPendingWrites -> Icons.Default.CloudSync
                                        else -> Icons.Default.CloudDone
                                    },
                                    contentDescription = "Cloud sync state",
                                    tint = if (viewModel.isLocalGuestMode || isOfflineOrPending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        viewModel.isLocalGuestMode -> "Local Mode (No Sign-In) • Tap for Cloud Sync"
                                        preferences.simulateOfflineMode -> "Offline Mode (Local Cache)"
                                        snapshot.hasPendingWrites -> "Syncing changes..."
                                        else -> "Cloud Synced"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    activeTab = WorkspaceTab.NOTES
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("note_view_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Notes view",
                                    tint = if (activeTab == WorkspaceTab.NOTES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    activeTab = if (activeTab == WorkspaceTab.FOLDERS) WorkspaceTab.NOTES else WorkspaceTab.FOLDERS
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("folder_view_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Toggle folders view",
                                    tint = if (activeTab == WorkspaceTab.FOLDERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    activeTab = if (activeTab == WorkspaceTab.CALENDAR) WorkspaceTab.NOTES else WorkspaceTab.CALENDAR
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("calendar_view_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Toggle calendar view",
                                    tint = if (activeTab == WorkspaceTab.CALENDAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Floating Status Popup (when copying, archiving, moving, or deleting notes)
                if (statusBanner != null) {
                    val bannerDensity = LocalDensity.current
                    Popup(
                        alignment = Alignment.TopCenter,
                        offset = with(bannerDensity) { IntOffset(0, 12.dp.roundToPx()) },
                        onDismissRequest = { viewModel.clearStatusBanner() },
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = false,
                            clippingEnabled = true
                        )
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 10.dp,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.clearStatusBanner() }
                                .testTag("status_popup_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = statusBanner ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                TextButton(
                                    onClick = {
                                        if (statusUndoNote != null) {
                                            viewModel.undoLastStatusAction()
                                        } else {
                                            viewModel.clearStatusBanner()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag(if (statusUndoNote != null) "status_popup_undo_button" else "status_popup_dismiss_button")
                                ) {
                                    Text(
                                        text = if (statusUndoNote != null) "Undo" else "Dismiss",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                when (activeTab) {
                    WorkspaceTab.CALENDAR -> {
                        CalendarViewSection(
                            notes = activeWorkspaceNotes,
                            unlockedNotes = unlockedNotes,
                            isDarkTheme = preferences.isDarkMode,
                            onSelectNote = { note ->
                                val isUnlockedInSession = !note.isLocked || unlockedNotes.containsKey(note.id)
                                if (note.isLocked && !isUnlockedInSession) {
                                    notePendingUnlock = note
                                    unlockPasswordInput = ""
                                    unlockError = null
                                } else {
                                    editingNote = note
                                    editorAutoVoice = false
                                    editorAutoLock = note.isLocked
                                    editorInitialReminderMillis = note.reminderEpochMillis
                                    editorInitialFolderId = note.folderId
                                    isEditorOpen = true
                                }
                            },
                            onCreateNoteForTimestamp = { targetTimestampMillis ->
                                editingNote = null
                                editorAutoVoice = false
                                editorAutoLock = false
                                editorInitialReminderMillis = targetTimestampMillis
                                editorInitialFolderId = selectedFolderId.orEmpty()
                                isEditorOpen = true
                            },
                            onBackToNotes = {
                                activeTab = WorkspaceTab.NOTES
                            }
                        )
                    }

                    WorkspaceTab.FOLDERS -> {
                        FolderOrganizationSection(
                            folders = folders,
                            notes = activeWorkspaceNotes,
                            activeFolderId = selectedFolderId,
                            onSelectFolder = { viewModel.setSelectedFolderId(it) },
                            onCreateFolder = { name, parentId, colorHex, onCreated ->
                                viewModel.createFolder(name, parentId, colorHex, onCreated)
                            },
                            onRenameFolder = { folder, newName, newColorHex ->
                                viewModel.renameFolder(folder, newName, newColorHex)
                            },
                            onMoveFolder = { folder, newParentId ->
                                viewModel.moveFolder(folder, newParentId)
                            },
                            onDeleteFolder = { folder ->
                                viewModel.deleteFolder(folder)
                            },
                            onAssignNoteToFolder = { note, targetFolderId ->
                                viewModel.assignNoteToFolder(note, targetFolderId)
                            },
                            onOpenNote = { note ->
                                val isUnlockedInSession = !note.isLocked || unlockedNotes.containsKey(note.id)
                                if (note.isLocked && !isUnlockedInSession) {
                                    notePendingUnlock = note
                                    unlockPasswordInput = ""
                                    unlockError = null
                                } else {
                                    editingNote = note
                                    editorAutoVoice = false
                                    editorAutoLock = note.isLocked
                                    editorInitialReminderMillis = note.reminderEpochMillis
                                    editorInitialFolderId = note.folderId
                                    isEditorOpen = true
                                }
                            },
                            onCreateNoteInFolder = { targetFolderId ->
                                editingNote = null
                                editorAutoVoice = false
                                editorAutoLock = false
                                editorInitialReminderMillis = 0L
                                editorInitialFolderId = targetFolderId
                                isEditorOpen = true
                            },
                            onBackToNotes = {
                                activeTab = WorkspaceTab.NOTES
                            }
                        )
                    }

                    WorkspaceTab.NOTES -> {
                        // Folder Filter Strip in Notes View
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedFolderId == null,
                                onClick = { viewModel.setSelectedFolderId(null) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "All folders",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("All Folders") },
                                modifier = Modifier.testTag("folder_filter_all")
                            )

                            FilterChip(
                                selected = selectedFolderId == "",
                                onClick = { viewModel.setSelectedFolderId("") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = "Unfiled notes",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Unfiled") },
                                modifier = Modifier.testTag("folder_filter_unfiled")
                            )

                            folders.forEach { folder ->
                                val path = formatFolderPath(folder, folderById)
                                FilterChip(
                                    selected = selectedFolderId == folder.id,
                                    onClick = { viewModel.setSelectedFolderId(folder.id) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(path) },
                                    modifier = Modifier.testTag("folder_filter_${folder.id}")
                                )
                            }

                            AssistChip(
                                onClick = { showQuickCreateFolderDialog = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = "New folder",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("+ Folder") },
                                modifier = Modifier.testTag("quick_new_folder_chip")
                            )
                        }

                        // Category & Smart Filter Bar — Row 1: All, Pinned, Reminders, Archive
                        val firstRowFilters = remember(allCategories, systemSmartFilters) {
                            val smartLower = systemSmartFilters.map { it.lowercase() }.toSet()
                            allCategories.filter { smartLower.contains(it.lowercase()) }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            firstRowFilters.forEach { filterName ->
                                val chipLabel = when (filterName) {
                                    "Archive" -> if (archivedCount > 0) "Archive ($archivedCount)" else "Archive"
                                    "Trash" -> if (trashedCount > 0) "Trash ($trashedCount)" else "Trash"
                                    else -> filterName
                                }
                                FilterChip(
                                    selected = selectedFilter.equals(filterName, ignoreCase = true),
                                    onClick = { viewModel.setSelectedFilter(filterName) },
                                    leadingIcon = when (filterName) {
                                        "Archive" -> {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Archive,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        else -> null
                                    },
                                    label = { Text(chipLabel) },
                                    modifier = Modifier.testTag("filter_chip_${filterName.lowercase()}")
                                )
                            }

                            FilterChip(
                                selected = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        isSelectionMode = false
                                        selectedNoteIds = emptySet()
                                    } else {
                                        isSelectionMode = true
                                        tooltipNoteId = null
                                        highlightedNoteId = null
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = "Select notes",
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = { Text(if (isSelectionMode) "Done" else "Select") },
                                modifier = Modifier.testTag("selection_mode_toggle_button")
                            )
                        }

                        // Category Bar — Row 2: Everything else (General, Work, Personal, Ideas, custom categories)
                        if (editableContentCategories.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                editableContentCategories.forEach { filterName ->
                                    FilterChip(
                                        selected = selectedFilter.equals(filterName, ignoreCase = true),
                                        onClick = { viewModel.setSelectedFilter(filterName) },
                                        label = { Text(filterName) },
                                        modifier = Modifier.testTag("filter_chip_${filterName.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Active Tags Row
                        val allTags = remember(snapshot.notes) {
                            snapshot.notes.filter { !it.isTrashed }.flatMap { it.tags }.distinct().take(12)
                        }
                        if (allTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allTags.forEach { tag ->
                                    val isSelected = selectedTag == tag
                                    AssistChip(
                                        onClick = { viewModel.setSelectedTag(tag) },
                                        label = {
                                            Text(
                                                text = "#$tag",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Contextual Banner when viewing Trash or Archive
                        if (selectedFilter.equals("Trash", ignoreCase = true)) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("trash_bin_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Trash Bin ($trashedCount)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Restore notes back to your workspace or delete them forever.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    if (trashedCount > 0) {
                                        Button(
                                            onClick = { showEmptyTrashConfirmDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                            ),
                                            modifier = Modifier.testTag("empty_trash_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Empty Trash",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Empty Trash")
                                        }
                                    }
                                }
                            }
                        } else if (selectedFilter.equals("Archive", ignoreCase = true)) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("archive_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Archive ($archivedCount) • Kept safe out of your active workspace",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        when (val state = notesUiState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            is UiState.Success -> {
                                val allowedFolderIds = remember(selectedFolderId, folders) {
                                    when (val targetFolder = selectedFolderId) {
                                        null -> null
                                        "" -> setOf("")
                                        else -> {
                                            val matching = mutableSetOf(targetFolder)
                                            for (folder in folders) {
                                                if (folder.id == targetFolder ||
                                                    NoteViewModel.isDescendantFolder(
                                                        targetParentId = folder.id,
                                                        sourceFolderId = targetFolder,
                                                        allFolders = folders
                                                    )
                                                ) {
                                                    matching.add(folder.id)
                                                }
                                            }
                                            matching
                                        }
                                    }
                                }

                                val filteredNotes = remember(
                                    state.data.notes,
                                    allowedFolderIds,
                                    selectedFilter,
                                    selectedTag,
                                    searchQuery,
                                    folderById,
                                    notePhotosByNoteId,
                                    unlockedNotes
                                ) {
                                    val q = searchQuery.trim().lowercase()
                                    state.data.notes.filter { note ->
                                        val matchesFolder = when (allowedFolderIds) {
                                            null -> true
                                            else -> if (allowedFolderIds.contains("")) {
                                                note.folderId.isBlank()
                                            } else {
                                                allowedFolderIds.contains(note.folderId)
                                            }
                                        }
                                        if (!matchesFolder) return@filter false

                                        val matchesLifecycleAndFilter = when {
                                            selectedFilter.equals("Trash", ignoreCase = true) -> note.isTrashed
                                            selectedFilter.equals("Archive", ignoreCase = true) -> note.isArchived && !note.isTrashed
                                            selectedFilter.equals("All", ignoreCase = true) -> !note.isTrashed && !note.isArchived
                                            selectedFilter.equals("Pinned", ignoreCase = true) -> !note.isTrashed && !note.isArchived && note.isPinned
                                            selectedFilter.equals("Vault", ignoreCase = true) -> !note.isTrashed && !note.isArchived && note.isLocked
                                            selectedFilter.equals("Reminders", ignoreCase = true) -> !note.isTrashed && !note.isArchived && note.reminderEpochMillis > 0L
                                            else -> !note.isTrashed && !note.isArchived && note.category.equals(selectedFilter, ignoreCase = true)
                                        }
                                        if (!matchesLifecycleAndFilter) return@filter false

                                        val matchesTag = selectedTag == null || note.tags.contains(selectedTag)
                                        if (!matchesTag) return@filter false

                                        if (q.isEmpty()) return@filter true

                                        val folderName = folderById[note.folderId]?.name?.lowercase().orEmpty()
                                        val attachedForSearch = notePhotosByNoteId[note.id].orEmpty()
                                        val isNoteSearchAccessible = !note.isLocked || unlockedNotes.containsKey(note.id)
                                        val matchesOcr = isNoteSearchAccessible &&
                                            attachedForSearch.isNotEmpty() &&
                                            cameraVaultStore.matchesOcrSearch(attachedForSearch, q)
                                        note.title.lowercase().contains(q) ||
                                            (!note.isLocked && note.content.lowercase().contains(q)) ||
                                            (isNoteSearchAccessible && unlockedNotes[note.id]?.lowercase()?.contains(q) == true) ||
                                            matchesOcr ||
                                            note.category.lowercase().contains(q) ||
                                            folderName.contains(q) ||
                                            note.tags.any { it.lowercase().contains(q) }
                                    }
                                }

                                val allVisibleSelected = remember(filteredNotes, selectedNoteIds) {
                                    filteredNotes.isNotEmpty() &&
                                        filteredNotes.all { selectedNoteIds.contains(it.id) }
                                }

                                if (isSelectionMode) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .testTag("bulk_selection_bar")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                                                        IconButton(
                                                            onClick = {
                                                                isSelectionMode = false
                                                                selectedNoteIds = emptySet()
                                                            },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .testTag("exit_selection_mode_button")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Exit selection mode",
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "${selectedNoteIds.size} selected",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.testTag("selected_notes_count_text")
                                                    )
                                                }

                                                TextButton(
                                                    onClick = {
                                                        selectedNoteIds = if (allVisibleSelected) {
                                                            emptySet()
                                                        } else {
                                                            filteredNotes.map { it.id }.toSet()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier
                                                        .height(30.dp)
                                                        .testTag("select_all_notes_button")
                                                ) {
                                                    Text(
                                                        text = if (allVisibleSelected) "Deselect All" else "Select All (${filteredNotes.size})",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val hasSelection = selectedNoteIds.isNotEmpty()
                                                val isTrashView = selectedFilter.equals("Trash", ignoreCase = true)
                                                val isArchiveView = selectedFilter.equals("Archive", ignoreCase = true)

                                                if (!isTrashView) {
                                                    AssistChip(
                                                        onClick = {
                                                            if (hasSelection) {
                                                                showBulkMoveFolderDialog = true
                                                            }
                                                        },
                                                        enabled = hasSelection,
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                                                contentDescription = "Move selected notes to folder",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        },
                                                        label = { Text("Move to Folder") },
                                                        modifier = Modifier.testTag("bulk_move_folder_button")
                                                    )

                                                    AssistChip(
                                                        onClick = {
                                                            if (hasSelection) {
                                                                showBulkCategoryDialog = true
                                                            }
                                                        },
                                                        enabled = hasSelection,
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Default.FolderOpen,
                                                                contentDescription = "Change category of selected notes",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        },
                                                        label = { Text("Category") },
                                                        modifier = Modifier.testTag("bulk_change_category_button")
                                                    )

                                                    if (!isArchiveView) {
                                                        AssistChip(
                                                            onClick = {
                                                                if (hasSelection) {
                                                                    viewModel.bulkTogglePin(selectedNoteIds)
                                                                    selectedNoteIds = emptySet()
                                                                    isSelectionMode = false
                                                                }
                                                            },
                                                            enabled = hasSelection,
                                                            leadingIcon = {
                                                                Icon(
                                                                    imageVector = Icons.Filled.PushPin,
                                                                    contentDescription = "Pin or unpin selected notes",
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            },
                                                            label = { Text("Pin") },
                                                            modifier = Modifier.testTag("bulk_pin_button")
                                                        )
                                                    }

                                                    AssistChip(
                                                        onClick = {
                                                            if (hasSelection) {
                                                                viewModel.bulkArchiveNotes(
                                                                    noteIds = selectedNoteIds,
                                                                    archive = !isArchiveView
                                                                )
                                                                selectedNoteIds = emptySet()
                                                                isSelectionMode = false
                                                            }
                                                        },
                                                        enabled = hasSelection,
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = if (isArchiveView) Icons.Default.Unarchive else Icons.Default.Archive,
                                                                contentDescription = if (isArchiveView) "Unarchive selected notes" else "Archive selected notes",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        },
                                                        label = { Text(if (isArchiveView) "Unarchive" else "Archive") },
                                                        modifier = Modifier.testTag("bulk_archive_button")
                                                    )
                                                } else {
                                                    AssistChip(
                                                        onClick = {
                                                            if (hasSelection) {
                                                                viewModel.bulkRestoreNotesFromTrash(selectedNoteIds)
                                                                selectedNoteIds = emptySet()
                                                                isSelectionMode = false
                                                            }
                                                        },
                                                        enabled = hasSelection,
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Default.RestoreFromTrash,
                                                                contentDescription = "Restore selected notes",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        },
                                                        label = { Text("Restore") },
                                                        modifier = Modifier.testTag("bulk_restore_button")
                                                    )
                                                }

                                                AssistChip(
                                                    onClick = {
                                                        if (hasSelection) {
                                                            viewModel.bulkDeleteNotes(context, selectedNoteIds)
                                                            selectedNoteIds = emptySet()
                                                            isSelectionMode = false
                                                        }
                                                    },
                                                    enabled = hasSelection,
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = if (isTrashView) Icons.Default.DeleteForever else Icons.Default.DeleteOutline,
                                                            contentDescription = "Delete selected notes",
                                                            tint = if (hasSelection) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = if (isTrashView) "Delete Forever" else "Delete",
                                                            color = if (hasSelection) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("bulk_delete_button")
                                                )
                                            }
                                        }
                                    }
                                }

                                if (filteredNotes.isEmpty()) {
                                    EmptyWorkspaceCard(
                                        selectedFilter = selectedFilter,
                                        onQuickNote = {
                                            editingNote = null
                                            editorAutoVoice = false
                                            editorAutoLock = false
                                            editorInitialReminderMillis = 0L
                                            editorInitialFolderId = selectedFolderId.orEmpty()
                                            isEditorOpen = true
                                        },
                                        onQuickCamera = {
                                            isQuickCameraOpen = true
                                        }
                                    )
                                } else {
                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Fixed(gridColumns),
                                        contentPadding = PaddingValues(bottom = 96.dp, top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalItemSpacing = 8.dp,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("notes_grid")
                                    ) {
                                        items(filteredNotes, key = { it.id }) { note ->
                                            val isUnlockedInSession = !note.isLocked || unlockedNotes.containsKey(note.id)
                                            val displayContent = if (note.isLocked) {
                                                unlockedNotes[note.id] ?: ""
                                            } else {
                                                note.content
                                            }
                                            val assignedFolder = note.folderId.takeIf { it.isNotBlank() }?.let { folderById[it] }
                                            val folderBadgeLabel = assignedFolder?.let { formatFolderPath(it, folderById) }
                                            val isHighlighted = highlightedNoteId == note.id
                                            val isTooltipOpen = !isSelectionMode && tooltipNoteId == note.id
                                            val isNoteSelected = selectedNoteIds.contains(note.id)
                                            val attachedPhotos = notePhotosByNoteId[note.id].orEmpty()
                                            val combinedOcrForCard = remember(attachedPhotos, isUnlockedInSession) {
                                                if (isUnlockedInSession && attachedPhotos.isNotEmpty()) {
                                                    cameraVaultStore.getCombinedOcrText(attachedPhotos)
                                                } else {
                                                    ""
                                                }
                                            }
                                            val ocrMatchSnippet = remember(attachedPhotos, searchQuery, isUnlockedInSession) {
                                                if (isUnlockedInSession && searchQuery.isNotBlank() && attachedPhotos.isNotEmpty()) {
                                                    cameraVaultStore.findOcrSearchSnippet(attachedPhotos, searchQuery)
                                                } else {
                                                    null
                                                }
                                            }

                                            val toggleNoteSelection = {
                                                selectedNoteIds = if (selectedNoteIds.contains(note.id)) {
                                                    selectedNoteIds - note.id
                                                } else {
                                                    selectedNoteIds + note.id
                                                }
                                            }

                                            val openEditorForNote = {
                                                tooltipNoteId = null
                                                if (note.isLocked && !isUnlockedInSession) {
                                                    notePendingUnlock = note
                                                    unlockPasswordInput = ""
                                                    unlockError = null
                                                } else {
                                                    editingNote = note
                                                    editorAutoVoice = false
                                                    editorAutoLock = note.isLocked
                                                    editorInitialReminderMillis = note.reminderEpochMillis
                                                    editorInitialFolderId = note.folderId
                                                    isEditorOpen = true
                                                }
                                            }

                                            NoteCardItem(
                                                note = note,
                                                folderBadgeLabel = folderBadgeLabel,
                                                isDarkTheme = preferences.isDarkMode,
                                                isUnlocked = isUnlockedInSession,
                                                decryptedContent = displayContent,
                                                isHighlighted = isHighlighted,
                                                isTooltipVisible = isTooltipOpen,
                                                isRecentlyCopied = lastCopiedNoteId == note.id,
                                                isPermanentNotificationActive = pinnedNotificationNotes.contains(note.id),
                                                isSelectionMode = isSelectionMode,
                                                isSelected = isNoteSelected,
                                                attachedPhotos = attachedPhotos,
                                                combinedOcrText = combinedOcrForCard,
                                                ocrSearchSnippet = ocrMatchSnippet,
                                                onExportPhotos = {
                                                    if (attachedPhotos.isNotEmpty()) {
                                                        notePhotosPendingExport = note.title.ifBlank { "Notii_Document_Scan" } to attachedPhotos
                                                    }
                                                },
                                                onAddPictures = {
                                                    if (!note.isLocked || isUnlockedInSession) {
                                                        noteIdPendingCameraAppend = note.id
                                                    } else {
                                                        openEditorForNote()
                                                    }
                                                },
                                                onEditPicture = { photo ->
                                                    if (!note.isLocked || isUnlockedInSession) {
                                                        workspacePhotoPendingEdit = photo
                                                    }
                                                },
                                                onToggleSelect = toggleNoteSelection,
                                                onStartSelectionWithNote = {
                                                    tooltipNoteId = null
                                                    highlightedNoteId = null
                                                    isSelectionMode = true
                                                    selectedNoteIds = setOf(note.id)
                                                },
                                                onTapCard = {
                                                    if (isSelectionMode) {
                                                        toggleNoteSelection()
                                                    } else if (isHighlighted || (note.isLocked && !isUnlockedInSession)) {
                                                        openEditorForNote()
                                                    } else {
                                                        highlightedNoteId = note.id
                                                        tooltipNoteId = null
                                                    }
                                                },
                                                onLongPressCard = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (isSelectionMode) {
                                                        toggleNoteSelection()
                                                    } else {
                                                        highlightedNoteId = note.id
                                                        tooltipNoteId = if (tooltipNoteId == note.id) null else note.id
                                                    }
                                                },
                                                onDismissTooltip = {
                                                    if (tooltipNoteId == note.id) {
                                                        tooltipNoteId = null
                                                    }
                                                },
                                                onEdit = openEditorForNote,
                                                onCopy = {
                                                    viewModel.copyNoteToClipboard(
                                                        context = context,
                                                        note = note,
                                                        isUnlocked = isUnlockedInSession,
                                                        decryptedContent = displayContent
                                                    )
                                                    lastCopiedNoteId = note.id
                                                },
                                                onPushPermanentNotification = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                        !ReminderNotificationHelper.hasNotificationPermission(context)
                                                    ) {
                                                        pendingPermanentNotificationNote = note
                                                        permanentNotificationPermissionLauncher.launch(
                                                            Manifest.permission.POST_NOTIFICATIONS
                                                        )
                                                    } else {
                                                        viewModel.togglePermanentNotification(
                                                            context = context,
                                                            note = note,
                                                            isUnlocked = isUnlockedInSession,
                                                            decryptedContent = displayContent
                                                        )
                                                    }
                                                },
                                                onMoveToFolder = { notePendingFolderMove = note },
                                                onTogglePin = { viewModel.togglePin(note) },
                                                onToggleArchive = {
                                                    if (highlightedNoteId == note.id) highlightedNoteId = null
                                                    viewModel.toggleArchive(note)
                                                },
                                                onRestoreFromTrash = { viewModel.restoreFromTrash(note) },
                                                onDeleteForever = {
                                                    if (highlightedNoteId == note.id) highlightedNoteId = null
                                                    viewModel.permanentlyDeleteNote(context, note.id)
                                                },
                                                onLockAgain = { viewModel.lockNoteInSession(note.id) },
                                                onDelete = {
                                                    if (highlightedNoteId == note.id) highlightedNoteId = null
                                                    viewModel.deleteNote(context, note.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom-left Option Button
            FloatingActionButton(
                onClick = { showOptionsDialog = true },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .testTag("options_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Options"
                )
            }
        }
    }

    // Empty Trash Confirmation Dialog
    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Empty Trash",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = { Text("Empty Trash?") },
            text = {
                Text(
                    text = "This will permanently delete all $trashedCount note(s) in Trash. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash(context)
                        showEmptyTrashConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.testTag("confirm_empty_trash_button")
                ) {
                    Text("Delete All Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Create Folder Dialog from Notes Filter Strip
    if (showQuickCreateFolderDialog) {
        CreateOrRenameFolderDialog(
            title = "Create New Folder",
            confirmLabel = "Create",
            initialName = "",
            initialColorHex = "#F59E0B",
            onDismiss = { showQuickCreateFolderDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.createFolder(
                    name = name,
                    parentFolderId = selectedFolderId.orEmpty(),
                    colorHex = colorHex,
                    onCreated = { createdId ->
                        viewModel.setSelectedFolderId(createdId)
                    }
                )
                showQuickCreateFolderDialog = false
            }
        )
    }

    // Assign / Move Note to Folder Dialog from Note Card
    val targetFolderMoveNote = notePendingFolderMove
    if (targetFolderMoveNote != null) {
        AssignNoteToFolderDialog(
            note = targetFolderMoveNote,
            folders = folders,
            folderById = folderById,
            onDismiss = { notePendingFolderMove = null },
            onSelectFolderId = { chosenFolderId ->
                viewModel.assignNoteToFolder(targetFolderMoveNote, chosenFolderId)
                notePendingFolderMove = null
            },
            onCreateFolderAndAssign = { newFolderName ->
                viewModel.createFolder(newFolderName) { newFolderId ->
                    viewModel.assignNoteToFolder(targetFolderMoveNote, newFolderId)
                    notePendingFolderMove = null
                }
            }
        )
    }

    // Password Unlock Dialog for Sensitive Vault Notes
    if (notePendingUnlock != null) {
        val targetNote = notePendingUnlock!!
        val unlockFocusRequester = remember { FocusRequester() }
        val unlockKeyboardController = LocalSoftwareKeyboardController.current

        val submitUnlock = {
            val success = viewModel.unlockSensitiveNote(targetNote, unlockPasswordInput)
            if (success) {
                notePendingUnlock = null
                editingNote = targetNote
                editorAutoVoice = false
                editorAutoLock = true
                editorInitialReminderMillis = targetNote.reminderEpochMillis
                editorInitialFolderId = targetNote.folderId
                isEditorOpen = true
            } else {
                unlockError = "Incorrect password. Please try again."
            }
        }

        LaunchedEffect(targetNote.id) {
            delay(120)
            runCatching {
                unlockFocusRequester.requestFocus()
                unlockKeyboardController?.show()
            }
        }

        AlertDialog(
            onDismissRequest = { notePendingUnlock = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock sensitive note",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Unlock Vault Note") },
            text = {
                ProvideSafeTextToolbar {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the password for “${targetNote.title.ifBlank { "Sensitive Note" }}” to decrypt its content.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = unlockPasswordInput,
                        onValueChange = {
                            unlockPasswordInput = it
                            unlockError = null
                        },
                        label = { Text("Vault password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitUnlock() }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(unlockFocusRequester)
                            .testTag("unlock_password_input")
                    )
                    if (unlockError != null) {
                        Text(
                            text = unlockError ?: "",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitUnlock() },
                    modifier = Modifier.testTag("confirm_unlock_button")
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { notePendingUnlock = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cloud Sync & Offline Mode Control Dialog
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            icon = {
                Icon(
                    imageVector = if (viewModel.isLocalGuestMode || preferences.simulateOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = "Cloud synchronization",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(if (viewModel.isLocalGuestMode) "Local Mode & Optional Cloud Sync" else "Cloud Sync & Offline Mode")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (viewModel.isLocalGuestMode) {
                            "You are using Notii without signing in. All notes, folders, archive, trash, and version history are stored locally on your device. Sign in with Google anytime if you want real-time cloud synchronization."
                        } else {
                            "Notii automatically caches all notes, folders, and version history on-device for instant offline access and synchronizes with Cloud Firestore in real time."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Active Notes: ${activeWorkspaceNotes.size}\n• Archived: $archivedCount • Trash: $trashedCount\n• Total Folders: ${folders.size}\n• Mode: ${
                            when {
                                viewModel.isLocalGuestMode -> "Local Device Storage (No Sign-In)"
                                preferences.simulateOfflineMode -> "Offline Mode Enabled"
                                else -> "Online Real-Time Cloud Sync"
                            }
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (viewModel.isLocalGuestMode) {
                    Button(
                        onClick = {
                            showSyncDialog = false
                            onRequestSignIn()
                        },
                        modifier = Modifier.testTag("dialog_sign_in_button")
                    ) {
                        Text("Sign in with Google")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.toggleOfflineMode(!preferences.simulateOfflineMode)
                            showSyncDialog = false
                        },
                        modifier = Modifier.testTag("toggle_offline_mode_button")
                    ) {
                        Text(if (preferences.simulateOfflineMode) "Go Online & Sync Now" else "Switch to Offline Mode")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text(if (viewModel.isLocalGuestMode) "Keep Local Mode" else "Close")
                }
            }
        )
    }

    // Master Password Pop-up Dialog (triggered by tapping the top-left app icon)
    if (showMasterPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showMasterPasswordDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Master Password",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Master Password") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = NoteVaultCrypto.MASTER_PASSWORD,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("master_password_value_text")
                            )
                        }
                    }
                    Text(
                        text = "Use this Master Password (${NoteVaultCrypto.MASTER_PASSWORD}) to unlock any password-protected Vault note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMasterPasswordDialog = false },
                    modifier = Modifier.testTag("close_master_password_dialog_button")
                ) {
                    Text("OK")
                }
            },
            modifier = Modifier.testTag("master_password_dialog")
        )
    }

    // Home Screen Widget Preview & Quick Pin Dialog
    if (showWidgetPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetPreviewDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "Home screen widget",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Home Screen Quick Widgets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Notii provides two Home Screen Widgets: a dedicated 'Quick Camera Shot' widget (up to 10 consecutive document shots, hidden from Android Gallery) and a 'Live Notes + Camera' widget.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Quick Camera Shot Widget • ${allVaultPhotos.size} private capture(s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "1-tap Home Screen button to launch Document Camera, take up to 10 consecutive shots (saved automatically), review/discard, and export.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    showWidgetPreviewDialog = false
                                    isQuickCameraOpen = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("widget_dialog_launch_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Launch Quick Camera Now")
                            }
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Notii Notes Widget • ${activeWorkspaceNotes.size} active notes",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            activeWorkspaceNotes.take(3).forEach { n ->
                                Text(
                                    text = "• [${n.category}] ${n.title.ifBlank { "Untitled" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (activeWorkspaceNotes.isEmpty()) {
                                Text(
                                    text = "Create a note to see it in your Home Screen Widget!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.syncHomeWidget(context, snapshot)
                        com.example.widget.QuickCameraWidgetProvider.refreshAllWidgets(context, allVaultPhotos.size)
                        showWidgetPreviewDialog = false
                    }
                ) {
                    Text("Refresh Widgets Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWidgetPreviewDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Options Dialog (triggered by the bottom-left Option button)
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Options") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Display & Appearance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.setGridView(!preferences.isGridView)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("layout_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (preferences.isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (preferences.isGridView) "Switch to List View" else "Switch to Grid View")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.setDarkMode(!preferences.isDarkMode)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (preferences.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (preferences.isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode")
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Quick Actions & Workspace",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            isQuickCameraOpen = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_quick_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Camera Shot (Up to 10 Pics)")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            showPrivateCameraVaultDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_camera_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Private Camera Captures & Export (${allVaultPhotos.size})")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            activeTab = WorkspaceTab.NOTES
                            isSelectionMode = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_select_notes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Multiple Notes")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            showManageFoldersCategoriesDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_manage_folders_categories_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage & Rearrange Folder & Category")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            activeTab = WorkspaceTab.NOTES
                            viewModel.setSelectedFolderId(null)
                            viewModel.setSelectedFilter("Archive")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_open_archive_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Archive ($archivedCount)")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            activeTab = WorkspaceTab.NOTES
                            viewModel.setSelectedFolderId(null)
                            viewModel.setSelectedFilter("Trash")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_open_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Trash Bin ($trashedCount)")
                    }

                    if (trashedCount > 0) {
                        OutlinedButton(
                            onClick = {
                                showOptionsDialog = false
                                showEmptyTrashConfirmDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("options_empty_trash_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Empty Trash Now ($trashedCount)",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (unlockedNotes.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                unlockedNotes.keys.toList().forEach { id ->
                                    viewModel.lockNoteInSession(id)
                                }
                                showOptionsDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("options_lock_all_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock All Unlocked Notes (${unlockedNotes.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Widget, Cloud & Account",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            showWidgetPreviewDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("widget_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Home Screen Widget")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            showSyncDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("options_sync_button")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isLocalGuestMode || preferences.simulateOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloud Sync & Storage")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            if (viewModel.isLocalGuestMode) {
                                onRequestSignIn()
                            } else {
                                onSignOut()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (viewModel.isLocalGuestMode) "sign_in_button" else "sign_out_button")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isLocalGuestMode) Icons.Default.CloudSync else Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (viewModel.isLocalGuestMode) "Sign in with Google" else "Sign Out")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showOptionsDialog = false },
                    modifier = Modifier.testTag("close_options_dialog_button")
                ) {
                    Text("Close")
                }
            },
            modifier = Modifier.testTag("options_dialog")
        )
    }

    // Manage & Rearrange Folders & Categories Dialog
    if (showManageFoldersCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showManageFoldersCategoriesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Manage Folders and Categories",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Manage & Rearrange") },
            text = {
                ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tab Switcher: Folders | Categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val foldersActive = manageDialogTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (foldersActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { manageDialogTab = 0 }
                                .padding(vertical = 7.dp)
                                .testTag("manage_tab_folders"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Folders (${folders.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (foldersActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val categoriesActive = manageDialogTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (categoriesActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { manageDialogTab = 1 }
                                .padding(vertical = 7.dp)
                                .testTag("manage_tab_categories"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Categories (${allCategories.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (categoriesActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (manageDialogTab == 0) {
                        // Folders Management & Rearrangement
                        OutlinedButton(
                            onClick = { showQuickCreateFolderDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manage_add_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ New Folder")
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (folders.isEmpty()) {
                                Text(
                                    text = "No folders yet. Tap '+ New Folder' above to create one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                folders.forEachIndexed { idx, folder ->
                                    val pathLabel = formatFolderPath(folder, folderById)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = pathLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { viewModel.reorderFolder(folder.id, -1, folders) },
                                                        enabled = idx > 0,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .testTag("folder_move_up_${folder.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowUpward,
                                                            contentDescription = "Move folder up",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.reorderFolder(folder.id, 1, folders) },
                                                        enabled = idx < folders.lastIndex,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .testTag("folder_move_down_${folder.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDownward,
                                                            contentDescription = "Move folder down",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { folderPendingRenameInManager = folder },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .testTag("folder_edit_${folder.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Rename folder",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteFolder(folder) },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .testTag("folder_delete_${folder.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Delete folder",
                                                            tint = MaterialTheme.colorScheme.tertiary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Categories Management & Rearrangement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newCategoryInput,
                                onValueChange = { newCategoryInput = it },
                                placeholder = { Text("New category...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manage_new_category_input")
                            )
                            Button(
                                onClick = {
                                    if (newCategoryInput.isNotBlank()) {
                                        viewModel.createCategory(newCategoryInput.trim())
                                        newCategoryInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("manage_add_category_button")
                            ) {
                                Text("Add")
                            }
                        }

                        val smartLowerSet = remember(systemSmartFilters) {
                            systemSmartFilters.map { it.lowercase() }.toSet()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allCategories.forEachIndexed { idx, catName ->
                                val isSystemFilter = smartLowerSet.contains(catName.lowercase())
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = catName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isSystemFilter) "System Filter" else "Category",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 28.dp) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.reorderCategory(catName, -1, allCategories) },
                                                    enabled = idx > 0,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .testTag("category_move_up_${catName.lowercase()}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowUpward,
                                                        contentDescription = "Move category up",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.reorderCategory(catName, 1, allCategories) },
                                                    enabled = idx < allCategories.lastIndex,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .testTag("category_move_down_${catName.lowercase()}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDownward,
                                                        contentDescription = "Move category down",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                if (!isSystemFilter) {
                                                    IconButton(
                                                        onClick = {
                                                            categoryPendingRename = catName
                                                            renameCategoryInput = catName
                                                        },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .testTag("category_rename_${catName.lowercase()}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Rename category",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteCategoryByName(catName) },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .testTag("category_delete_${catName.lowercase()}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Delete category",
                                                            tint = MaterialTheme.colorScheme.tertiary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showManageFoldersCategoriesDialog = false },
                    modifier = Modifier.testTag("close_manage_folders_categories_button")
                ) {
                    Text("Done")
                }
            },
            modifier = Modifier.testTag("manage_folders_categories_dialog")
        )
    }

    val targetFolderRenameInManager = folderPendingRenameInManager
    if (targetFolderRenameInManager != null) {
        CreateOrRenameFolderDialog(
            title = "Rename Folder",
            confirmLabel = "Save",
            initialName = targetFolderRenameInManager.name,
            initialColorHex = targetFolderRenameInManager.colorHex,
            onDismiss = { folderPendingRenameInManager = null },
            onConfirm = { newName, newColorHex ->
                viewModel.renameFolder(targetFolderRenameInManager, newName, newColorHex)
                folderPendingRenameInManager = null
            }
        )
    }

    val targetCategoryRename = categoryPendingRename
    if (targetCategoryRename != null) {
        AlertDialog(
            onDismissRequest = { categoryPendingRename = null },
            title = { Text("Rename Category") },
            text = {
                ProvideSafeTextToolbar {
                OutlinedTextField(
                    value = renameCategoryInput,
                    onValueChange = { renameCategoryInput = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_category_input")
                )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameCategoryInput.isNotBlank()) {
                            viewModel.renameCategory(targetCategoryRename, renameCategoryInput.trim())
                            categoryPendingRename = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_category_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Move Notes to Folder Dialog
    if (showBulkMoveFolderDialog) {
        var chosenBulkFolderId by remember { mutableStateOf(selectedFolderId.orEmpty()) }
        var newBulkFolderName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBulkMoveFolderDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = "Move selected notes to folder",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Move ${selectedNoteIds.size} Note(s) to Folder") },
            text = {
                ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose a destination folder for the ${selectedNoteIds.size} selected note(s):",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    FilterChip(
                        selected = chosenBulkFolderId.isBlank(),
                        onClick = { chosenBulkFolderId = "" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = "Unfiled",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("No Folder (Unfiled)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_assign_folder_unfiled_chip")
                    )

                    folders.forEach { folder ->
                        val path = formatFolderPath(folder, folderById)
                        FilterChip(
                            selected = chosenBulkFolderId == folder.id,
                            onClick = { chosenBulkFolderId = folder.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(path) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bulk_assign_folder_${folder.id}")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newBulkFolderName,
                            onValueChange = { newBulkFolderName = it },
                            placeholder = { Text("Or quick-create folder...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bulk_quick_create_folder_input")
                        )
                        AssistChip(
                            onClick = {
                                val clean = newBulkFolderName.trim()
                                if (clean.isNotEmpty()) {
                                    val idsToMove = selectedNoteIds
                                    viewModel.createFolder(clean) { newFolderId ->
                                        viewModel.bulkMoveNotesToFolder(idsToMove, newFolderId)
                                    }
                                    newBulkFolderName = ""
                                    showBulkMoveFolderDialog = false
                                    selectedNoteIds = emptySet()
                                    isSelectionMode = false
                                }
                            },
                            label = { Text("+ Add") },
                            modifier = Modifier.testTag("bulk_quick_create_folder_button")
                        )
                    }
                }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkMoveNotesToFolder(selectedNoteIds, chosenBulkFolderId)
                        showBulkMoveFolderDialog = false
                        selectedNoteIds = emptySet()
                        isSelectionMode = false
                    },
                    modifier = Modifier.testTag("confirm_bulk_move_folder_button")
                ) {
                    Text("Move Here")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkMoveFolderDialog = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("bulk_move_folder_dialog")
        )
    }

    // Bulk Change Category Dialog
    if (showBulkCategoryDialog) {
        var chosenBulkCategory by remember(editableContentCategories) {
            mutableStateOf(editableContentCategories.firstOrNull() ?: "General")
        }

        AlertDialog(
            onDismissRequest = { showBulkCategoryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Change category",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Assign Category (${selectedNoteIds.size} Note(s))") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select a category for the ${selectedNoteIds.size} selected note(s):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    editableContentCategories.forEach { catName ->
                        FilterChip(
                            selected = chosenBulkCategory.equals(catName, ignoreCase = true),
                            onClick = { chosenBulkCategory = catName },
                            label = { Text(catName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bulk_category_chip_${catName.lowercase()}")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkChangeCategory(selectedNoteIds, chosenBulkCategory)
                        showBulkCategoryDialog = false
                        selectedNoteIds = emptySet()
                        isSelectionMode = false
                    },
                    modifier = Modifier.testTag("confirm_bulk_category_button")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkCategoryDialog = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("bulk_change_category_dialog")
        )
    }

    if (showPrivateCameraVaultDialog) {
        PrivateCameraVaultDialog(
            vaultStore = cameraVaultStore,
            allPhotos = allVaultPhotos,
            onLaunchQuickCamera = {
                showPrivateCameraVaultDialog = false
                isQuickCameraOpen = true
            },
            onDismiss = { showPrivateCameraVaultDialog = false },
            onStatusMessage = { msg -> viewModel.postStatusMessage(msg) }
        )
    }

    val targetExportPair = notePhotosPendingExport
    if (targetExportPair != null) {
        ExportDocumentPhotosDialog(
            vaultStore = cameraVaultStore,
            photos = targetExportPair.second,
            defaultTitle = targetExportPair.first,
            onDismiss = { notePhotosPendingExport = null },
            onStatusMessage = { msg -> viewModel.postStatusMessage(msg) }
        )
    }

    val activeWorkspacePhotoToEdit = workspacePhotoPendingEdit?.let { target ->
        allVaultPhotos.find { it.id == target.id } ?: target
    }
    if (activeWorkspacePhotoToEdit != null) {
        val siblingPhotos = notePhotosByNoteId[activeWorkspacePhotoToEdit.noteId].orEmpty()
        val photoIdx = siblingPhotos.indexOfFirst { it.id == activeWorkspacePhotoToEdit.id }
        EditDocumentPhotoDialog(
            vaultStore = cameraVaultStore,
            photo = activeWorkspacePhotoToEdit,
            noteId = activeWorkspacePhotoToEdit.noteId,
            canMoveLeft = photoIdx > 0,
            canMoveRight = photoIdx in 0 until (siblingPhotos.size - 1),
            onMovePhoto = { moveLeft ->
                cameraVaultStore.reorderPhotoInNote(
                    noteId = activeWorkspacePhotoToEdit.noteId,
                    photoId = activeWorkspacePhotoToEdit.id,
                    moveEarlier = moveLeft
                )
                viewModel.postStatusMessage(
                    if (moveLeft) "Moved picture earlier in note." else "Moved picture later in note."
                )
            },
            onPhotoUpdated = { updated ->
                workspacePhotoPendingEdit = updated
            },
            onPhotoDiscarded = {
                workspacePhotoPendingEdit = null
                viewModel.postStatusMessage("Removed picture from note.")
            },
            onDismiss = { workspacePhotoPendingEdit = null },
            onStatusMessage = { msg -> viewModel.postStatusMessage(msg) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NoteCardItem(
    note: Note,
    folderBadgeLabel: String?,
    isDarkTheme: Boolean,
    isUnlocked: Boolean,
    decryptedContent: String,
    isHighlighted: Boolean,
    isTooltipVisible: Boolean,
    isRecentlyCopied: Boolean,
    isPermanentNotificationActive: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    attachedPhotos: List<CapturedDocumentPhoto> = emptyList(),
    combinedOcrText: String = "",
    ocrSearchSnippet: String? = null,
    onExportPhotos: () -> Unit = {},
    onAddPictures: () -> Unit = {},
    onEditPicture: (CapturedDocumentPhoto) -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onStartSelectionWithNote: () -> Unit = {},
    onTapCard: () -> Unit,
    onLongPressCard: () -> Unit,
    onDismissTooltip: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onPushPermanentNotification: () -> Unit,
    onMoveToFolder: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onRestoreFromTrash: () -> Unit,
    onDeleteForever: () -> Unit,
    onLockAgain: () -> Unit,
    onDelete: () -> Unit
) {
    val swatch = NoteColorPalette.find { it.hex.equals(note.colorHex, ignoreCase = true) }
        ?: NoteColorPalette.first()
    val cardBg = if (isDarkTheme) swatch.darkSurface else swatch.lightSurface
    val borderWidth = if (isSelected || isHighlighted) 2.5.dp else 1.dp
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isHighlighted -> if (isDarkTheme) HighlightYellowOutlineDark else HighlightYellowOutlineLight
        else -> swatch.accent.copy(alpha = 0.4f)
    }

    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .combinedClickable(
                    onClick = onTapCard,
                    onLongClick = onLongPressCard
                )
                .testTag("note_card_${note.id}"),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onToggleSelect() }
                                .testTag("select_note_checkbox_${note.id}")
                        )
                    }
                    Surface(
                        color = swatch.accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = note.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = swatch.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (!folderBadgeLabel.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = folderBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (note.versionHistory.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("note_version_badge_${note.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Revisions",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "v${note.versionHistory.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!note.isTrashed) {
                        IconButton(
                            onClick = onMoveToFolder,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("move_note_folder_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = "Assign note to folder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleArchive,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("archive_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (note.isArchived) "Unarchive note" else "Archive note",
                                tint = if (note.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (note.isLocked) {
                        IconButton(
                            onClick = {
                                if (isUnlocked) onLockAgain() else onEdit()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isUnlocked) "Lock note" else "Locked note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (!note.isTrashed && !note.isArchived) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("pin_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin note",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.isLocked && !isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password protected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Password protected • Tap to unlock",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (decryptedContent.isNotBlank()) {
                Text(
                    text = decryptedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (attachedPhotos.isNotEmpty() && (!note.isLocked || isUnlocked)) {
                var showCardOcrText by remember(note.id) { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        attachedPhotos.take(6).forEachIndexed { idx, photo ->
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(82.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onEditPicture(photo) }
                                    .testTag("card_edit_photo_${photo.id}")
                            ) {
                                DocumentPhotoThumbnail(
                                    photo = photo,
                                    badgeLabel = "#${idx + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(5.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit picture",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // If an active search query matched hidden OCR text, show a compact OCR Match badge
                    if (!ocrSearchSnippet.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_search_match_badge_${note.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "OCR Match: $ocrSearchSnippet",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${attachedPhotos.size} pic(s) • Tap to edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!note.isTrashed && attachedPhotos.size < MAX_CONSECUTIVE_SHOTS) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onAddPictures() }
                                        .testTag("card_add_photo_${note.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Add more pictures to note",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "+ Pic",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            if (note.title.isBlank() && decryptedContent.isBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onEdit() }
                                        .testTag("add_manual_text_${note.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = "Add note text manually",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "+ Text",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (combinedOcrText.isNotBlank()) {
                                Surface(
                                    color = if (showCardOcrText) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showCardOcrText = !showCardOcrText }
                                        .testTag("toggle_card_ocr_${note.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showCardOcrText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showCardOcrText) "Hide OCR text" else "Show OCR text",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (showCardOcrText) "Hide OCR" else "Show OCR",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onExportPhotos() }
                                    .testTag("export_note_photos_${note.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = "Export document pictures",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Export",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showCardOcrText && combinedOcrText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_ocr_text_box_${note.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Extracted Document Text (On-Device OCR):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = combinedOcrText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (note.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.tags.take(4).forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (note.isTrashed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trashedText = if (note.trashedAtEpochMillis > 0L) {
                        "Trashed " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(note.trashedAtEpochMillis))
                    } else {
                        "In Trash"
                    }
                    Text(
                        text = trashedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onRestoreFromTrash,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("restore_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = "Restore from Trash",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteForever,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("delete_forever_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete forever",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ts = note.updatedAt ?: note.createdAt
                    val dateText = remember(ts?.seconds, ts?.nanoseconds) {
                        if (ts != null) {
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts.toDate())
                        } else {
                            "Just now"
                        }
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.reminderEpochMillis > 0L) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Reminder scheduled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = onPushPermanentNotification,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("notify_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (isPermanentNotificationActive) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = if (isPermanentNotificationActive) {
                                    "Remove permanent notification"
                                } else {
                                    "Push permanent notification"
                                },
                                tint = if (isPermanentNotificationActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("copy_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (isRecentlyCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy to clipboard",
                                tint = if (isRecentlyCopied) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        }

        // Floating Long-press Tooltip Overlay: Copy, Edit, Archive, Pin, Delete
        if (isTooltipVisible) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = with(density) { IntOffset(0, (-8).dp.roundToPx()) },
                onDismissRequest = onDismissTooltip,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = true
                )
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 10.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("note_tooltip_${note.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NoteTooltipActionItem(
                            icon = Icons.Default.CheckCircle,
                            label = "Select",
                            testTag = "tooltip_select_${note.id}",
                            onClick = {
                                onDismissTooltip()
                                onStartSelectionWithNote()
                            }
                        )
                        NoteTooltipActionItem(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            testTag = "tooltip_copy_${note.id}",
                            onClick = {
                                onCopy()
                                onDismissTooltip()
                            }
                        )
                        NoteTooltipActionItem(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            testTag = "tooltip_edit_${note.id}",
                            onClick = {
                                onDismissTooltip()
                                onEdit()
                            }
                        )
                        if (!note.isTrashed) {
                            NoteTooltipActionItem(
                                icon = Icons.Default.PhotoCamera,
                                label = "Add Pic",
                                testTag = "tooltip_add_photo_${note.id}",
                                onClick = {
                                    onDismissTooltip()
                                    onAddPictures()
                                }
                            )
                        }
                        NoteTooltipActionItem(
                            icon = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            label = if (note.isArchived) "Unarchive" else "Archive",
                            testTag = "tooltip_archive_${note.id}",
                            onClick = {
                                onDismissTooltip()
                                onToggleArchive()
                            }
                        )
                        NoteTooltipActionItem(
                            icon = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            label = if (note.isPinned) "Unpin" else "Pin",
                            testTag = "tooltip_pin_${note.id}",
                            onClick = {
                                onDismissTooltip()
                                onTogglePin()
                            }
                        )
                        NoteTooltipActionItem(
                            icon = Icons.Default.DeleteOutline,
                            label = "Delete",
                            isDestructive = true,
                            testTag = "tooltip_delete_${note.id}",
                            onClick = {
                                onDismissTooltip()
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteTooltipActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentTint = if (isDestructive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.inverseOnSurface
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentTint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentTint
        )
    }
}

@Composable
private fun EmptyWorkspaceCard(
    selectedFilter: String,
    onQuickNote: () -> Unit,
    onQuickCamera: () -> Unit
) {
    val isTrash = selectedFilter.equals("Trash", ignoreCase = true)
    val isArchive = selectedFilter.equals("Archive", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isTrash -> Icons.Default.DeleteOutline
                            isArchive -> Icons.Default.Archive
                            else -> Icons.Default.EditNote
                        },
                        contentDescription = "Empty state",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Text(
                    text = when {
                        isTrash -> "Trash Bin is empty"
                        isArchive -> "No archived notes yet"
                        else -> "Your workspace is ready"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when {
                        isTrash -> "Notes moved to Trash appear here so you can restore them anytime or delete them forever."
                        isArchive -> "Archive completed notes to keep your main workspace clutter-free without losing them."
                        else -> "Capture ideas with instant cloud sync, offline mode, folders, calendar view, version history, archive & trash, private document camera (up to 10 consecutive shots), custom reminders, and AES-256 vault security."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isTrash && !isArchive) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onQuickNote,
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("empty_state_new_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Create Note",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onQuickCamera,
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("empty_state_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Camera",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
