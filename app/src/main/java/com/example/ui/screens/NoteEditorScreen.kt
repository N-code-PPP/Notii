package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CapturedDocumentPhoto
import com.example.data.local.DocumentEnhancementMode
import com.example.data.local.PrivateDocumentVaultStore
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteVersionEntry
import com.example.notification.ReminderNotificationHelper
import com.example.ui.theme.NoteColorPalette
import com.example.ui.theme.ProvideSafeTextToolbar
import com.example.voice.VoiceToTextController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    existingNote: Note?,
    initialPlainContent: String,
    availableCategories: List<String>,
    availableFolders: List<Folder> = emptyList(),
    initialFolderId: String = "",
    draftNoteId: String = "",
    autoStartVoice: Boolean = false,
    autoEnableLock: Boolean = false,
    initialReminderMillis: Long = 0L,
    onCreateCategory: (String) -> Unit,
    onCreateFolder: (name: String, onCreated: (String) -> Unit) -> Unit = { _, _ -> },
    onSave: (
        title: String,
        plainContent: String,
        folderId: String,
        category: String,
        tags: List<String>,
        colorHex: String,
        isPinned: Boolean,
        isArchived: Boolean,
        isLocked: Boolean,
        newPassword: String,
        reminderEpochMillis: Long,
        reminderLabel: String
    ) -> Unit,
    onSaveAndNotify: ((
        title: String,
        plainContent: String,
        folderId: String,
        category: String,
        tags: List<String>,
        colorHex: String,
        isPinned: Boolean,
        isArchived: Boolean,
        isLocked: Boolean,
        newPassword: String,
        reminderEpochMillis: Long,
        reminderLabel: String
    ) -> Unit)? = null,
    onRestoreFromTrash: (() -> Unit)? = null,
    onDeleteForever: (() -> Unit)? = null,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val cleanupTextFocus = {
        runCatching { textToolbar.hide() }
        runCatching { focusManager.clearFocus(force = true) }
        runCatching { keyboardController?.hide() }
    }

    val effectiveNoteId = remember(existingNote?.id, draftNoteId) {
        existingNote?.id?.takeIf { it.isNotBlank() }
            ?: draftNoteId.ifBlank { "draft_note_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    }
    val vaultStore = remember(context) { PrivateDocumentVaultStore.getInstance(context) }
    val allVaultPhotos by vaultStore.photosFlow.collectAsStateWithLifecycle()
    val noteVaultPhotos = remember(allVaultPhotos, effectiveNoteId) {
        if (effectiveNoteId.isBlank()) {
            emptyList()
        } else {
            allVaultPhotos
                .filter { it.noteId == effectiveNoteId && it.fileExists }
                .sortedWith(compareBy<CapturedDocumentPhoto> { it.pageNumber }.thenBy { it.capturedAtMillis })
        }
    }
    var isAddingPhotosViaCamera by remember { mutableStateOf(false) }
    var photoPendingEdit by remember { mutableStateOf<CapturedDocumentPhoto?>(null) }
    var editorPhotoStatusMsg by remember { mutableStateOf<String?>(null) }

    var title by remember(existingNote) { mutableStateOf(existingNote?.title ?: "") }
    var content by remember(existingNote, initialPlainContent) {
        mutableStateOf(initialPlainContent)
    }
    var folderId by remember(existingNote, initialFolderId) {
        mutableStateOf(existingNote?.folderId?.takeIf { it.isNotBlank() } ?: initialFolderId)
    }
    var newFolderInput by remember { mutableStateOf("") }
    var showNewFolderField by remember { mutableStateOf(false) }

    var category by remember(existingNote) { mutableStateOf(existingNote?.category ?: "General") }
    val tags = remember(existingNote) {
        mutableStateListOf<String>().apply {
            addAll(existingNote?.tags ?: emptyList())
        }
    }
    var newTagInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("") }
    var showNewCategoryField by remember { mutableStateOf(false) }

    var colorHex by remember(existingNote) { mutableStateOf(existingNote?.colorHex ?: "#1E293B") }
    var isPinned by remember(existingNote) { mutableStateOf(existingNote?.isPinned ?: false) }
    var isArchived by remember(existingNote) { mutableStateOf(existingNote?.isArchived ?: false) }
    var isLocked by remember(existingNote, autoEnableLock) {
        mutableStateOf(existingNote?.isLocked ?: autoEnableLock)
    }
    var vaultPassword by remember { mutableStateOf("") }
    val resolvedInitialReminder = existingNote?.reminderEpochMillis?.takeIf { it > 0L } ?: initialReminderMillis
    var reminderEpochMillis by remember(existingNote, initialReminderMillis) {
        mutableLongStateOf(resolvedInitialReminder)
    }
    var reminderLabel by remember(existingNote) {
        mutableStateOf(existingNote?.reminderLabel ?: "")
    }
    var showReminderPanel by remember(existingNote, initialReminderMillis) {
        mutableStateOf(resolvedInitialReminder > 0L)
    }
    var showVoicePanel by remember { mutableStateOf(autoStartVoice) }
    var showVersionHistoryPanel by remember { mutableStateOf(false) }
    var versionRestoreFeedback by remember { mutableStateOf<String?>(null) }
    var isListeningVoice by remember { mutableStateOf(false) }
    var liveVoicePreview by remember { mutableStateOf("") }
    var voiceFeedback by remember { mutableStateOf<String?>(null) }
    var notificationFeedback by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val editorScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val contentFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    var shouldFocusPasswordField by remember {
        mutableStateOf(autoEnableLock && existingNote == null)
    }

    val parsedVersionHistory = remember(existingNote) {
        existingNote?.versionHistory
            .orEmpty()
            .mapNotNull { NoteVersionEntry.fromSerializedString(it) }
            .sortedByDescending { it.savedAtMillis }
    }

    val voiceController = remember(context) {
        VoiceToTextController(
            context = context,
            onPartialText = { partial -> liveVoicePreview = partial },
            onFinalText = { recognized ->
                liveVoicePreview = ""
                content = if (content.isBlank()) recognized else "$content\n$recognized"
            },
            onStatusChange = { active, msg ->
                isListeningVoice = active
                if (msg != null) voiceFeedback = msg
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cleanupTextFocus()
            voiceController.stopListening()
        }
    }

    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull().orEmpty()
            if (recognized.isNotBlank()) {
                content = if (content.isBlank()) recognized else "$content\n$recognized"
                voiceFeedback = "Voice transcription added."
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoicePanel = true
            voiceController.startListening()
        } else {
            showVoicePanel = true
            voiceFeedback = "Microphone permission declined. You can still use System Speech Dialog or quick voice phrases below."
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderNotificationHelper.showImmediateNotification(
                context = context,
                noteId = existingNote?.id?.ifBlank { "preview_note" } ?: "preview_note",
                noteTitle = title.trim(),
                reminderLabel = reminderLabel.ifBlank { content.trim() },
                isLocked = isLocked
            )
            notificationFeedback = "Custom push notification sent to your status bar!"
        } else {
            notificationFeedback = "Notification permission denied in system settings."
        }
    }

    val importPhotosPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                var importedCount = 0
                withContext(Dispatchers.IO) {
                    uris.forEach { uri ->
                        vaultStore.importUriToNoteWithOcr(
                            uri = uri,
                            noteId = effectiveNoteId,
                            mode = DocumentEnhancementMode.ORIGINAL_COLOR
                        ).onSuccess {
                            importedCount++
                        }
                    }
                }
                if (importedCount > 0) {
                    editorPhotoStatusMsg = "Added $importedCount picture(s) to this note."
                } else {
                    editorPhotoStatusMsg = "Unable to import selected picture(s)."
                }
            }
        }
    }

    val handleBackNavigation = {
        cleanupTextFocus()
        if (existingNote == null && (noteVaultPhotos.isNotEmpty() || title.isNotBlank() || content.isNotBlank())) {
            onSave(
                title,
                content,
                folderId,
                category,
                tags.toList(),
                colorHex,
                isPinned,
                isArchived,
                isLocked,
                vaultPassword,
                reminderEpochMillis,
                reminderLabel
            )
        } else {
            onBack()
        }
    }

    BackHandler(enabled = !isAddingPhotosViaCamera) {
        handleBackNavigation()
    }

    if (isAddingPhotosViaCamera) {
        QuickDocumentCameraScreen(
            vaultStore = vaultStore,
            isAppendingToNote = true,
            onCompleteWithKeptPhotos = { keptPhotos, _, _ ->
                isAddingPhotosViaCamera = false
                if (keptPhotos.isNotEmpty()) {
                    vaultStore.appendPhotosToNote(
                        photoIds = keptPhotos.map { it.id },
                        noteId = effectiveNoteId
                    )
                    editorPhotoStatusMsg = "Added ${keptPhotos.size} picture(s) to this note."
                }
            },
            onCancelOrClose = {
                isAddingPhotosViaCamera = false
            },
            onStatusMessage = { msg ->
                editorPhotoStatusMsg = msg
            }
        )
        return
    }

    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                voiceController.startListening()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Auto-focus note content input and show keyboard when creating a Quick Note, opening a locked note, or adding text to a blank camera note
    LaunchedEffect(existingNote?.id, autoStartVoice, autoEnableLock) {
        val isBlankExistingNote = existingNote != null && !autoEnableLock && existingNote.content.isBlank()
        if (!autoStartVoice && (existingNote == null && !autoEnableLock || existingNote?.isLocked == true || isBlankExistingNote)) {
            delay(120)
            runCatching {
                contentFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    // Auto-scroll down, select the password box, and show keyboard when Password Protection is toggled ON
    LaunchedEffect(isLocked, shouldFocusPasswordField) {
        if (isLocked && shouldFocusPasswordField) {
            shouldFocusPasswordField = false
            delay(180)
            runCatching {
                editorScrollState.animateScrollTo(editorScrollState.maxValue)
                passwordFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    ProvideSafeTextToolbar {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        handleBackNavigation()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to workspace"
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End)
                ) {
                    IconButton(
                        onClick = {
                            isPinned = !isPinned
                            if (isPinned) isArchived = false
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            isArchived = !isArchived
                            if (isArchived) isPinned = false
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_archive_button")
                    ) {
                        Icon(
                            imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (isArchived) "Unarchive note" else "Archive note",
                            tint = if (isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (existingNote != null) {
                        IconButton(
                            onClick = { showVersionHistoryPanel = !showVersionHistoryPanel },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("editor_version_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Version history",
                                tint = if (showVersionHistoryPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            cleanupTextFocus()
                            isAddingPhotosViaCamera = true
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_add_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Add or capture pictures for this note",
                            tint = if (noteVaultPhotos.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            showVoicePanel = !showVoicePanel
                            if (showVoicePanel && !isListeningVoice) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    voiceController.startListening()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                voiceController.stopListening()
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice to text dictation",
                            tint = if (showVoicePanel || isListeningVoice) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showReminderPanel = !showReminderPanel },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_reminder_button")
                    ) {
                        Icon(
                            imageVector = if (reminderEpochMillis > 0L) Icons.Default.Alarm else Icons.Default.AlarmAdd,
                            contentDescription = "Custom reminder notification",
                            tint = if (reminderEpochMillis > 0L || showReminderPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = {
                                cleanupTextFocus()
                                onDelete()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = if (existingNote?.isTrashed == true) "Delete forever" else "Move to Trash",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        cleanupTextFocus()
                        ReminderNotificationHelper.showPermanentNoteNotification(
                            context = context,
                            noteId = effectiveNoteId,
                            noteTitle = title,
                            noteContent = content,
                            isLocked = false
                        )
                        val saveAction = onSaveAndNotify ?: onSave
                        saveAction(
                            title,
                            content,
                            folderId,
                            category,
                            tags.toList(),
                            colorHex,
                            isPinned,
                            isArchived,
                            isLocked,
                            vaultPassword,
                            reminderEpochMillis,
                            reminderLabel
                        )
                    },
                    modifier = Modifier.testTag("editor_save_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save & Notify",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save & Notify",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (validationError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Trashed Note Warning Banner with Restore & Delete Forever actions
            if (existingNote?.isTrashed == true) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("editor_trashed_banner")
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
                                text = "This note is in Trash",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Restore it to your active workspace or delete it permanently.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onRestoreFromTrash != null) {
                                OutlinedButton(
                                    onClick = onRestoreFromTrash,
                                    modifier = Modifier.testTag("editor_restore_from_trash_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestoreFromTrash,
                                        contentDescription = "Restore note",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore")
                                }
                            }
                            if (onDeleteForever != null) {
                                Button(
                                    onClick = onDeleteForever,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.testTag("editor_delete_forever_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Delete forever",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(editorScrollState)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Version History Interactive Panel
                AnimatedVisibility(visible = showVersionHistoryPanel) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("version_history_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Version history",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Version History (${parsedVersionHistory.size}/10 revisions)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Every save captures a snapshot so you can inspect or restore prior edits.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { showVersionHistoryPanel = false }) {
                                    Text("Hide")
                                }
                            }

                            if (versionRestoreFeedback != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = versionRestoreFeedback ?: "",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (parsedVersionHistory.isEmpty()) {
                                Text(
                                    text = "No previous versions recorded yet. Save edits to build revision history.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                parsedVersionHistory.forEachIndexed { idx, entry ->
                                    val formattedTime = remember(entry.savedAtMillis) {
                                        if (entry.savedAtMillis > 0L) {
                                            SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault())
                                                .format(Date(entry.savedAtMillis))
                                        } else {
                                            "Saved snapshot"
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .testTag("version_history_item_$idx")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (idx == 0) "Latest Snapshot • $formattedTime" else formattedTime,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = entry.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = entry.title.ifBlank { "Untitled" },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (entry.content.isNotBlank()) {
                                                    Text(
                                                        text = entry.content,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    title = entry.title
                                                    if (entry.content != "[Encrypted Vault Note]") {
                                                        content = entry.content
                                                    }
                                                    category = entry.category
                                                    versionRestoreFeedback =
                                                        "Restored version from $formattedTime into editor. Tap Save to commit."
                                                },
                                                modifier = Modifier.testTag("restore_version_button_$idx")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = "Restore version",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restore")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Voice-to-Text Interactive Panel
                AnimatedVisibility(visible = showVoicePanel) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_to_text_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Voice status",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isListeningVoice) "Listening... Speak now" else "Voice-to-Text Dictation",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                speechIntentLauncher.launch(voiceController.buildSystemRecognizerIntent())
                                            } catch (e: Exception) {
                                                voiceFeedback = "System speech prompt unavailable on this emulator; tap a quick phrase below."
                                            }
                                        },
                                        modifier = Modifier.testTag("system_speech_button")
                                    ) {
                                        Text("System Mic")
                                    }
                                    Button(
                                        onClick = {
                                            if (isListeningVoice) {
                                                voiceController.stopListening()
                                                isListeningVoice = false
                                            } else {
                                                val hasPerm = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPerm) {
                                                    voiceController.startListening()
                                                } else {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text(if (isListeningVoice) "Stop" else "Listen")
                                    }
                                }
                            }

                            if (liveVoicePreview.isNotBlank()) {
                                Text(
                                    text = "“$liveVoicePreview”",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            if (voiceFeedback != null) {
                                Text(
                                    text = voiceFeedback ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Quick Dictation Phrases (Tap to insert):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "Follow up with design team at 3 PM tomorrow.",
                                    "Action item: finalize Q4 cloud sync release checklist.",
                                    "Remember to pick up groceries and coffee beans on the way home."
                                ).forEachIndexed { idx, phrase ->
                                    AssistChip(
                                        onClick = {
                                            content = if (content.isBlank()) phrase else "$content\n$phrase"
                                        },
                                        label = { Text(phrase.take(28) + "…") },
                                        modifier = Modifier.testTag("quick_voice_phrase_$idx")
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Push Notification Reminder Panel
                AnimatedVisibility(visible = showReminderPanel) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Reminder settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Custom Push Notification Reminder",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                if (reminderEpochMillis > 0L) {
                                    TextButton(onClick = { reminderEpochMillis = 0L }) {
                                        Text("Clear")
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = reminderLabel,
                                onValueChange = { reminderLabel = it },
                                label = { Text("Custom notification message") },
                                placeholder = { Text("e.g., Don't forget to send the report!") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_label_input")
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "-1 Day" to -24 * 60 * 60_000L,
                                    "+1 Day" to 24 * 60 * 60_000L,
                                    "+1 Week" to 7 * 24 * 60 * 60_000L,
                                    "In 15 min" to 15 * 60_000L,
                                    "In 1 hour" to 60 * 60_000L
                                ).forEach { (label, offsetMs) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val base = if (reminderEpochMillis > 0L && (label.startsWith("+") || label.startsWith("-"))) {
                                                reminderEpochMillis
                                            } else {
                                                System.currentTimeMillis()
                                            }
                                            reminderEpochMillis = (base + offsetMs).coerceAtLeast(60_000L)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                !ReminderNotificationHelper.hasNotificationPermission(context)
                                            ) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (reminderEpochMillis > 0L) {
                                        "Scheduled: " + SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault())
                                            .format(Date(reminderEpochMillis))
                                    } else {
                                        "No time selected yet"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !ReminderNotificationHelper.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            ReminderNotificationHelper.showImmediateNotification(
                                                context = context,
                                                noteId = existingNote?.id?.ifBlank { "preview_note" } ?: "preview_note",
                                                noteTitle = title.trim(),
                                                reminderLabel = reminderLabel.ifBlank { content.trim() },
                                                isLocked = isLocked
                                            )
                                            notificationFeedback = "Push notification dispatched!"
                                        }
                                    },
                                    modifier = Modifier.testTag("send_test_notification_button")
                                ) {
                                    Text("Notify Now")
                                }
                            }

                            if (notificationFeedback != null) {
                                Text(
                                    text = notificationFeedback ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = "Note title...",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                // Status & Version History Quick Bar for Existing Notes
                if (existingNote != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isArchived,
                            onClick = {
                                isArchived = !isArchived
                                if (isArchived) isPinned = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(if (isArchived) "Archived" else "Archive Note") },
                            modifier = Modifier.testTag("editor_archive_chip")
                        )

                        AssistChip(
                            onClick = { showVersionHistoryPanel = !showVersionHistoryPanel },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text("Version History (${parsedVersionHistory.size})")
                            },
                            modifier = Modifier.testTag("editor_version_history_chip")
                        )
                    }
                }

                // Folder Selector & Inline Creator
                val folderById = remember(availableFolders) { availableFolders.associateBy { it.id } }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = folderId.isBlank(),
                            onClick = { folderId = "" },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FolderOff,
                                    contentDescription = "No folder",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Unfiled") },
                            modifier = Modifier.testTag("editor_folder_unfiled_chip")
                        )
                        availableFolders.forEach { folder ->
                            val folderPath = formatFolderPath(folder, folderById)
                            FilterChip(
                                selected = folderId == folder.id,
                                onClick = { folderId = folder.id },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(folderPath) },
                                modifier = Modifier.testTag("editor_folder_chip_${folder.id}")
                            )
                        }
                        AssistChip(
                            onClick = { showNewFolderField = !showNewFolderField },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New folder",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("New Folder") },
                            modifier = Modifier.testTag("editor_add_folder_chip")
                        )
                    }

                    AnimatedVisibility(visible = showNewFolderField) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newFolderInput,
                                onValueChange = { newFolderInput = it },
                                label = { Text("New folder name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_new_folder_input")
                            )
                            Button(
                                onClick = {
                                    val clean = newFolderInput.trim()
                                    if (clean.isNotEmpty()) {
                                        onCreateFolder(clean) { createdId ->
                                            folderId = createdId
                                        }
                                        newFolderInput = ""
                                        showNewFolderField = false
                                    }
                                },
                                modifier = Modifier.testTag("editor_confirm_new_folder_button")
                            ) {
                                Text("Create")
                            }
                        }
                    }
                }

                // Category Selector & Inline Creator
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allCats = (listOf("General", "Work", "Personal", "Ideas", "Urgent") + availableCategories).distinct()
                        allCats.forEach { cat ->
                            FilterChip(
                                selected = category.equals(cat, ignoreCase = true),
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("category_chip_${cat.lowercase()}")
                            )
                        }
                        AssistChip(
                            onClick = { showNewCategoryField = !showNewCategoryField },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New category",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("New Category") },
                            modifier = Modifier.testTag("add_category_chip")
                        )
                    }

                    AnimatedVisibility(visible = showNewCategoryField) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCategoryInput,
                                onValueChange = { newCategoryInput = it },
                                label = { Text("Custom category name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clean = newCategoryInput.trim()
                                    if (clean.isNotEmpty()) {
                                        onCreateCategory(clean)
                                        category = clean
                                        newCategoryInput = ""
                                        showNewCategoryField = false
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }

                // Note Body Editor
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = "Start typing or tap the microphone icon to dictate...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contentFocusRequester)
                        .testTag("note_content_input")
                )

                // Private Pictures & Document Captures Section (Available in ANY note anytime afterward)
                var selectedEditorPhotoIds by remember { mutableStateOf(emptySet<String>()) }
                var showEditorExportDialog by remember { mutableStateOf(false) }
                var showEditorOcrText by remember { mutableStateOf(false) }
                val combinedOcrText = remember(noteVaultPhotos) {
                    vaultStore.getCombinedOcrText(noteVaultPhotos)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (noteVaultPhotos.isNotEmpty()) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_private_photos_section")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action chips to Add Pictures to ANY note anytime afterward
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {
                                    cleanupTextFocus()
                                    isAddingPhotosViaCamera = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("+ Camera") },
                                modifier = Modifier.testTag("editor_open_camera_button")
                            )

                            AssistChip(
                                onClick = {
                                    cleanupTextFocus()
                                    runCatching {
                                        importPhotosPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }.onFailure {
                                        val fallbackBmp = PrivateDocumentVaultStore.createDocumentScanFallbackBitmap(
                                            shotNumber = noteVaultPhotos.size + 1,
                                            mode = DocumentEnhancementMode.ORIGINAL_COLOR
                                        )
                                        vaultStore.addBitmapToNote(
                                            rawBitmap = fallbackBmp,
                                            noteId = effectiveNoteId,
                                            mode = DocumentEnhancementMode.ORIGINAL_COLOR
                                        )
                                        editorPhotoStatusMsg = "Added picture to note."
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("+ Import Photo") },
                                modifier = Modifier.testTag("editor_import_photo_button")
                            )

                            AssistChip(
                                onClick = {
                                    val nextShot = noteVaultPhotos.size + 1
                                    val scanBmp = PrivateDocumentVaultStore.createDocumentScanFallbackBitmap(
                                        shotNumber = nextShot,
                                        mode = DocumentEnhancementMode.HIGH_CONTRAST_DOC
                                    )
                                    vaultStore.addBitmapToNote(
                                        rawBitmap = scanBmp,
                                        noteId = effectiveNoteId,
                                        mode = DocumentEnhancementMode.HIGH_CONTRAST_DOC
                                    )
                                    editorPhotoStatusMsg = "Captured & attached Page #$nextShot to this note."
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("+ Quick Scan") },
                                modifier = Modifier.testTag("editor_quick_scan_page_button")
                            )
                        }

                        if (editorPhotoStatusMsg != null) {
                            Text(
                                text = editorPhotoStatusMsg ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (noteVaultPhotos.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                noteVaultPhotos.forEachIndexed { idx, photo ->
                                    val isSelected = selectedEditorPhotoIds.contains(photo.id)
                                    val badge = if (photo.customCaption.isNotBlank()) {
                                        "#${idx + 1} ${photo.customCaption.take(10)}"
                                    } else {
                                        "#${idx + 1}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(142.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                photoPendingEdit = photo
                                            }
                                            .testTag("editor_doc_photo_${photo.id}")
                                    ) {
                                        DocumentPhotoThumbnail(
                                            photo = photo,
                                            badgeLabel = badge,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Top-right selection toggle for bulk discard/export
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .clickable {
                                                    selectedEditorPhotoIds = if (isSelected) {
                                                        selectedEditorPhotoIds - photo.id
                                                    } else {
                                                        selectedEditorPhotoIds + photo.id
                                                    }
                                                }
                                                .testTag("editor_select_photo_${photo.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Select picture",
                                                tint = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Bottom Edit overlay pill
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.72f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(5.dp)
                                                .clickable { photoPendingEdit = photo }
                                                .testTag("editor_edit_photo_${photo.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit picture",
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Edit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // On-Device OCR Section (Hidden by default, toggleable on demand)
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DocumentScanner,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Extracted Document Text (On-Device OCR)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (showEditorOcrText) {
                                                        "Showing extracted text from ${noteVaultPhotos.size} document page(s)"
                                                    } else {
                                                        "Hidden by default • Active for workspace search"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = { showEditorOcrText = !showEditorOcrText },
                                            modifier = Modifier.testTag("editor_toggle_ocr_button")
                                        ) {
                                            Icon(
                                                imageVector = if (showEditorOcrText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (showEditorOcrText) "Hide OCR" else "Show OCR",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = showEditorOcrText) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("editor_ocr_expanded_panel")
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = combinedOcrText.ifBlank { "No text extracted from these document captures." },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .padding(10.dp)
                                                        .testTag("editor_ocr_text_value")
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                        clipboard?.setPrimaryClip(
                                                            ClipData.newPlainText("Extracted Document OCR", combinedOcrText)
                                                        )
                                                        editorPhotoStatusMsg = "Copied extracted OCR text to clipboard."
                                                    },
                                                    enabled = combinedOcrText.isNotBlank(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("editor_copy_ocr_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Copy OCR", style = MaterialTheme.typography.labelSmall)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        if (combinedOcrText.isNotBlank()) {
                                                            content = if (content.isBlank()) {
                                                                combinedOcrText
                                                            } else {
                                                                "$content\n\n--- Extracted OCR Text ---\n$combinedOcrText"
                                                            }
                                                            editorPhotoStatusMsg = "Inserted extracted OCR text into note body."
                                                        }
                                                    },
                                                    enabled = combinedOcrText.isNotBlank(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("editor_insert_ocr_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Insert into Note", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val targetPhoto = noteVaultPhotos.firstOrNull { selectedEditorPhotoIds.contains(it.id) }
                                            ?: noteVaultPhotos.firstOrNull()
                                        if (targetPhoto != null) {
                                            photoPendingEdit = targetPhoto
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("editor_edit_selected_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Pic", maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val removed = vaultStore.discardPhotos(selectedEditorPhotoIds)
                                        selectedEditorPhotoIds = emptySet()
                                        editorPhotoStatusMsg = "Discarded $removed picture(s) from private vault."
                                    },
                                    enabled = selectedEditorPhotoIds.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("editor_discard_selected_photos_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Discard (${selectedEditorPhotoIds.size})", maxLines = 1)
                                }

                                Button(
                                    onClick = { showEditorExportDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("editor_export_photos_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.IosShare,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export (${noteVaultPhotos.size})", maxLines = 1)
                                }
                            }
                        }
                    }
                }

                val activeEditPhoto = photoPendingEdit
                if (activeEditPhoto != null) {
                    val refreshedPhoto = noteVaultPhotos.firstOrNull { it.id == activeEditPhoto.id } ?: activeEditPhoto
                    val idxInNote = noteVaultPhotos.indexOfFirst { it.id == refreshedPhoto.id }
                    EditDocumentPhotoDialog(
                        vaultStore = vaultStore,
                        photo = refreshedPhoto,
                        noteId = effectiveNoteId,
                        canMoveLeft = idxInNote > 0,
                        canMoveRight = idxInNote in 0 until (noteVaultPhotos.size - 1),
                        onMovePhoto = { moveLeft ->
                            if (vaultStore.reorderPhotoInNote(effectiveNoteId, refreshedPhoto.id, moveEarlier = moveLeft)) {
                                editorPhotoStatusMsg = if (moveLeft) "Moved picture earlier." else "Moved picture later."
                            }
                        },
                        onInsertOcrIntoNote = { ocrSnippet ->
                            if (ocrSnippet.isNotBlank()) {
                                content = if (content.isBlank()) ocrSnippet else "$content\n\n$ocrSnippet"
                            }
                        },
                        onPhotoUpdated = { updated ->
                            photoPendingEdit = updated
                        },
                        onPhotoDiscarded = {
                            selectedEditorPhotoIds = selectedEditorPhotoIds - refreshedPhoto.id
                            photoPendingEdit = null
                            editorPhotoStatusMsg = "Removed picture from note."
                        },
                        onDismiss = { photoPendingEdit = null },
                        onStatusMessage = { msg -> editorPhotoStatusMsg = msg }
                    )
                }

                if (showEditorExportDialog && noteVaultPhotos.isNotEmpty()) {
                    ExportDocumentPhotosDialog(
                        vaultStore = vaultStore,
                        photos = noteVaultPhotos,
                        defaultTitle = title.ifBlank { "Notii_Document_Scan" },
                        onDismiss = { showEditorExportDialog = false },
                        onStatusMessage = { msg -> editorPhotoStatusMsg = msg }
                    )
                }

                // Tags Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "TAGS (${tags.size}/10)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { tag ->
                                AssistChip(
                                    onClick = { tags.remove(tag) },
                                    label = { Text("#$tag") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag $tag",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it.replace(" ", "") },
                            placeholder = { Text("Add tag (e.g. roadmap, ideas)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_tag_input")
                        )
                        OutlinedButton(
                            onClick = {
                                val cleaned = newTagInput.trim().removePrefix("#").lowercase()
                                if (cleaned.isNotEmpty() && !tags.contains(cleaned) && tags.size < 10) {
                                    tags.add(cleaned)
                                    newTagInput = ""
                                }
                            },
                            enabled = newTagInput.isNotBlank() && tags.size < 10,
                            modifier = Modifier.testTag("add_tag_button")
                        ) {
                            Text("Add Tag")
                        }
                    }
                }

                // Card Accent Color Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CARD ACCENT COLOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NoteColorPalette.forEach { swatch ->
                            val selected = colorHex.equals(swatch.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(swatch.accent)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = swatch.hex }
                                    .testTag("color_swatch_${swatch.label.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = swatch.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact Password Protection & Bottom Save Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Password protection",
                                        tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Password Protection",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "AES-256-GCM Vault",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = isLocked,
                                    onCheckedChange = { enabled ->
                                        isLocked = enabled
                                        if (enabled) {
                                            shouldFocusPasswordField = true
                                            coroutineScope.launch {
                                                delay(180)
                                                runCatching {
                                                    editorScrollState.animateScrollTo(editorScrollState.maxValue)
                                                    passwordFocusRequester.requestFocus()
                                                    keyboardController?.show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("lock_note_switch")
                                )
                            }
                        }

                        Button(
                            onClick = {
                                cleanupTextFocus()
                                onSave(
                                    title,
                                    content,
                                    folderId,
                                    category,
                                    tags.toList(),
                                    colorHex,
                                    isPinned,
                                    isArchived,
                                    isLocked,
                                    vaultPassword,
                                    reminderEpochMillis,
                                    reminderLabel
                                )
                            },
                            modifier = Modifier.testTag("editor_bottom_save_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save note",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                    }

                    AnimatedVisibility(visible = isLocked) {
                        OutlinedTextField(
                            value = vaultPassword,
                            onValueChange = {
                                vaultPassword = it
                                validationError = null
                            },
                            label = {
                                Text(
                                    if (existingNote?.isLocked == true) "Set custom password (or leave blank for Master Password)"
                                    else "Custom password (optional — defaults to Master Password)"
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocusRequester)
                                .testTag("vault_password_input")
                        )
                    }
                }
            }
        }
    }
    }
}
