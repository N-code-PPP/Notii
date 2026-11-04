package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.theme.NoteColorPalette
import com.example.ui.theme.ProvideSafeTextToolbar
import com.example.ui.viewmodel.NoteViewModel

@Composable
fun FolderOrganizationSection(
    folders: List<Folder>,
    notes: List<Note>,
    activeFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onCreateFolder: (name: String, parentFolderId: String, colorHex: String, onCreated: (String) -> Unit) -> Unit,
    onRenameFolder: (folder: Folder, newName: String, newColorHex: String) -> Unit,
    onMoveFolder: (folder: Folder, newParentFolderId: String) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    onAssignNoteToFolder: (note: Note, folderId: String) -> Unit,
    onOpenNote: (Note) -> Unit,
    onCreateNoteInFolder: (String) -> Unit,
    onBackToNotes: () -> Unit
) {
    BackHandler {
        if (!activeFolderId.isNullOrBlank()) {
            val currentFolder = folders.find { it.id == activeFolderId }
            onSelectFolder(currentFolder?.parentFolderId?.ifBlank { null })
        } else {
            onBackToNotes()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createParentFolderId by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToMove by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }

    val folderById = remember(folders) { folders.associateBy { it.id } }
    val currentFolder = remember(folders, activeFolderId) {
        activeFolderId?.takeIf { it.isNotBlank() }?.let { folderById[it] }
    }

    val breadcrumbChain = remember(folders, currentFolder) {
        buildFolderBreadcrumb(currentFolder, folderById)
    }

    val displayedFolders = remember(folders, currentFolder) {
        val targetParentId = currentFolder?.id ?: ""
        folders.filter { it.parentFolderId == targetParentId }
    }

    val notesInCurrentView = remember(notes, currentFolder) {
        if (currentFolder != null) {
            notes.filter { it.folderId == currentFolder.id }
        } else {
            notes.filter { it.folderId.isBlank() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("folder_organization_container")
    ) {
        // Header + Create Folder Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentFolder?.name ?: "Folder Organization",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (currentFolder != null) {
                        "Full path: ${formatFolderPath(currentFolder, folderById)}"
                    } else {
                        "${folders.size} total folder(s) • Create, rename, move, or assign notes"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentFolder != null) {
                    OutlinedButton(
                        onClick = { onCreateNoteInFolder(currentFolder.id) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("new_note_in_current_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New note in folder",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Note")
                    }
                }

                Button(
                    onClick = {
                        createParentFolderId = currentFolder?.id ?: ""
                        showCreateDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Create folder",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentFolder != null) "New Subfolder" else "New Folder")
                }
            }
        }

        // Breadcrumb Navigation Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentFolder == null,
                onClick = { onSelectFolder(null) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Root folders",
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("All Root Folders") },
                modifier = Modifier.testTag("breadcrumb_root_folders")
            )

            breadcrumbChain.forEach { crumb ->
                Text(
                    text = "/",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = crumb.id == currentFolder?.id,
                    onClick = { onSelectFolder(crumb.id) },
                    label = { Text(crumb.name) },
                    modifier = Modifier.testTag("breadcrumb_folder_${crumb.id}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subfolders / Root Folders List
            item {
                Text(
                    text = if (currentFolder != null) {
                        "SUBFOLDERS IN ${currentFolder.name.uppercase()} (${displayedFolders.size})"
                    } else {
                        "FOLDERS (${displayedFolders.size} root • ${folders.size} total)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (displayedFolders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (currentFolder != null) {
                                    "No subfolders inside “${currentFolder.name}”"
                                } else {
                                    "No folders created yet"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap '${if (currentFolder != null) "New Subfolder" else "New Folder"}' to organize your notes into folders.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedFolders, key = { it.id }) { folder ->
                    val directNotesCount = notes.count { it.folderId == folder.id }
                    val subfolderCount = folders.count { it.parentFolderId == folder.id }
                    FolderCardItem(
                        folder = folder,
                        folderById = folderById,
                        noteCount = directNotesCount,
                        subfolderCount = subfolderCount,
                        onOpenFolder = { onSelectFolder(folder.id) },
                        onCreateNoteHere = { onCreateNoteInFolder(folder.id) },
                        onRename = { folderToRename = folder },
                        onMove = { folderToMove = folder },
                        onDelete = { folderToDelete = folder }
                    )
                }
            }

            // If user has nested folders and is at Root, also show a quick flat directory overview so any nested folder is 1-tap reachable
            if (currentFolder == null && folders.any { it.parentFolderId.isNotBlank() }) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NESTED SUBFOLDERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val nestedFolders = folders.filter { it.parentFolderId.isNotBlank() }
                items(nestedFolders, key = { "nested_${it.id}" }) { folder ->
                    val directNotesCount = notes.count { it.folderId == folder.id }
                    val subfolderCount = folders.count { it.parentFolderId == folder.id }
                    FolderCardItem(
                        folder = folder,
                        folderById = folderById,
                        noteCount = directNotesCount,
                        subfolderCount = subfolderCount,
                        onOpenFolder = { onSelectFolder(folder.id) },
                        onCreateNoteHere = { onCreateNoteInFolder(folder.id) },
                        onRename = { folderToRename = folder },
                        onMove = { folderToMove = folder },
                        onDelete = { folderToDelete = folder }
                    )
                }
            }

            // Notes inside current folder (or Unfiled notes at root)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (currentFolder != null) {
                        "NOTES IN ${currentFolder.name.uppercase()} (${notesInCurrentView.size})"
                    } else {
                        "UNFILED NOTES (${notesInCurrentView.size})"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (notesInCurrentView.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentFolder != null) {
                                "No notes in this folder yet. Tap 'New Note' above or move an existing note here."
                            } else {
                                "All notes are organized in folders!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(notesInCurrentView, key = { "folder_note_${it.id}" }) { note ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onOpenNote(note) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (note.title.isNotBlank()) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${note.category} • ${if (note.isLocked) "Encrypted Vault Note" else note.content.ifBlank { "No content" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { noteToMove = note },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("assign_folder_button_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = "Assign note to folder",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (currentFolder != null) "Move" else "Assign Folder")
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Create Folder Dialog
    if (showCreateDialog) {
        CreateOrRenameFolderDialog(
            title = if (createParentFolderId.isNotBlank()) "Create Subfolder" else "Create New Folder",
            confirmLabel = "Create",
            initialName = "",
            initialColorHex = "#F59E0B",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, colorHex ->
                onCreateFolder(name, createParentFolderId, colorHex) {}
                showCreateDialog = false
            }
        )
    }

    // 2. Rename Folder Dialog
    val targetRename = folderToRename
    if (targetRename != null) {
        CreateOrRenameFolderDialog(
            title = "Rename Folder",
            confirmLabel = "Save",
            initialName = targetRename.name,
            initialColorHex = targetRename.colorHex,
            onDismiss = { folderToRename = null },
            onConfirm = { newName, newColorHex ->
                onRenameFolder(targetRename, newName, newColorHex)
                folderToRename = null
            }
        )
    }

    // 3. Move Folder Dialog
    val targetMove = folderToMove
    if (targetMove != null) {
        MoveFolderDialog(
            folderToMove = targetMove,
            allFolders = folders,
            folderById = folderById,
            onDismiss = { folderToMove = null },
            onConfirmMove = { newParentId ->
                onMoveFolder(targetMove, newParentId)
                folderToMove = null
            }
        )
    }

    // 4. Delete Folder Confirmation Dialog
    val targetDelete = folderToDelete
    if (targetDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete folder",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = { Text("Delete Folder “${targetDelete.name}”?") },
            text = {
                Text(
                    "Deleting this folder will keep all of its notes safe and move them to ${
                        if (targetDelete.parentFolderId.isNotBlank()) "its parent folder" else "Unfiled Notes"
                    }."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(targetDelete)
                        folderToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_folder_button")
                ) {
                    Text("Delete Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Assign / Move Note to Folder Dialog
    val targetNote = noteToMove
    if (targetNote != null) {
        AssignNoteToFolderDialog(
            note = targetNote,
            folders = folders,
            folderById = folderById,
            onDismiss = { noteToMove = null },
            onSelectFolderId = { chosenFolderId ->
                onAssignNoteToFolder(targetNote, chosenFolderId)
                noteToMove = null
            },
            onCreateFolderAndAssign = { newFolderName ->
                onCreateFolder(newFolderName, "", "#F59E0B") { createdFolderId ->
                    onAssignNoteToFolder(targetNote, createdFolderId)
                    noteToMove = null
                }
            }
        )
    }
}

@Composable
private fun FolderCardItem(
    folder: Folder,
    folderById: Map<String, Folder>,
    noteCount: Int,
    subfolderCount: Int,
    onOpenFolder: () -> Unit,
    onCreateNoteHere: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val swatch = NoteColorPalette.find { it.hex.equals(folder.colorHex, ignoreCase = true) }
        ?: NoteColorPalette.first()
    val fullPath = remember(folder, folderById) { formatFolderPath(folder, folderById) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = swatch.accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onOpenFolder() }
            .testTag("folder_card_${folder.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(swatch.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder icon",
                            tint = swatch.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$fullPath • $noteCount note(s)" +
                                if (subfolderCount > 0) " • $subfolderCount subfolder(s)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCreateNoteHere,
                        modifier = Modifier.testTag("folder_add_note_${folder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create note in ${folder.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.testTag("rename_folder_${folder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename folder ${folder.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onMove,
                        modifier = Modifier.testTag("move_folder_${folder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = "Move folder ${folder.name}",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_folder_${folder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete folder ${folder.name}",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateOrRenameFolderDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialColorHex: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var folderName by remember(initialName) { mutableStateOf(initialName) }
    var selectedColorHex by remember(initialColorHex) { mutableStateOf(initialColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CreateNewFolder,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title) },
        text = {
            ProvideSafeTextToolbar {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    placeholder = { Text("e.g., Product Roadmap, Travel, Receipts") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )

                Text(
                    text = "FOLDER ACCENT COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoteColorPalette.forEach { swatch ->
                        val isSelected = selectedColorHex.equals(swatch.hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatch.accent)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = swatch.hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = folderName.trim()
                    if (clean.isNotEmpty()) {
                        onConfirm(clean, selectedColorHex)
                    }
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier.testTag("confirm_folder_dialog_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MoveFolderDialog(
    folderToMove: Folder,
    allFolders: List<Folder>,
    folderById: Map<String, Folder>,
    onDismiss: () -> Unit,
    onConfirmMove: (newParentFolderId: String) -> Unit
) {
    val validDestinations = remember(folderToMove, allFolders) {
        allFolders.filter { candidate ->
            candidate.id != folderToMove.id &&
                !NoteViewModel.isDescendantFolder(
                    targetParentId = candidate.id,
                    sourceFolderId = folderToMove.id,
                    allFolders = allFolders
                )
        }
    }
    var selectedParentId by remember(folderToMove) { mutableStateOf(folderToMove.parentFolderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                contentDescription = "Move folder",
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = { Text("Move Folder “${folderToMove.name}”") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select a destination parent folder (or Root level):",
                    style = MaterialTheme.typography.bodyMedium
                )

                FilterChip(
                    selected = selectedParentId.isBlank(),
                    onClick = { selectedParentId = "" },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Root level",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Root Level (Top-level Folder)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("move_folder_target_root")
                )

                validDestinations.forEach { destination ->
                    val path = formatFolderPath(destination, folderById)
                    FilterChip(
                        selected = selectedParentId == destination.id,
                        onClick = { selectedParentId = destination.id },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SubdirectoryArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(path) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("move_folder_target_${destination.id}")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmMove(selectedParentId) },
                modifier = Modifier.testTag("confirm_move_folder_button")
            ) {
                Text("Move Here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AssignNoteToFolderDialog(
    note: Note,
    folders: List<Folder>,
    folderById: Map<String, Folder>,
    onDismiss: () -> Unit,
    onSelectFolderId: (String) -> Unit,
    onCreateFolderAndAssign: (String) -> Unit
) {
    var selectedFolderId by remember(note) { mutableStateOf(note.folderId) }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Move note to folder",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Assign Note to Folder") },
        text = {
            ProvideSafeTextToolbar {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose a folder for “${note.title.ifBlank { "Untitled Note" }}”:",
                    style = MaterialTheme.typography.bodyMedium
                )

                FilterChip(
                    selected = selectedFolderId.isBlank(),
                    onClick = { selectedFolderId = "" },
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
                        .testTag("assign_note_unfiled_chip")
                )

                folders.forEach { folder ->
                    val path = formatFolderPath(folder, folderById)
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
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
                            .testTag("assign_note_folder_${folder.id}")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Or quick-create folder...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_create_folder_input")
                    )
                    AssistChip(
                        onClick = {
                            val clean = newFolderName.trim()
                            if (clean.isNotEmpty()) {
                                onCreateFolderAndAssign(clean)
                                newFolderName = ""
                            }
                        },
                        label = { Text("+ Add") },
                        modifier = Modifier.testTag("quick_create_folder_button")
                    )
                }
            }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectFolderId(selectedFolderId) },
                modifier = Modifier.testTag("confirm_assign_note_folder_button")
            ) {
                Text("Save Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

internal fun buildFolderBreadcrumb(
    currentFolder: Folder?,
    folderById: Map<String, Folder>
): List<Folder> {
    if (currentFolder == null) return emptyList()
    val chain = mutableListOf<Folder>()
    var cursor: Folder? = currentFolder
    val visited = mutableSetOf<String>()
    while (cursor != null && visited.add(cursor.id)) {
        chain.add(0, cursor)
        cursor = if (cursor.parentFolderId.isNotBlank()) folderById[cursor.parentFolderId] else null
    }
    return chain
}

internal fun formatFolderPath(
    folder: Folder,
    folderById: Map<String, Folder>
): String {
    return buildFolderBreadcrumb(folder, folderById).joinToString(" / ") { it.name }
}
