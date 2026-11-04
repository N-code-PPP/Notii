package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalWorkspaceStore
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteVersionEntry
import com.example.data.util.NoteVaultCrypto
import com.example.ui.screens.addDays
import com.example.ui.screens.defaultReminderTimeForDay
import com.example.ui.screens.formatFolderPath
import com.example.ui.screens.notesForDay
import com.example.ui.screens.startOfDayMillis
import com.example.ui.viewmodel.NoteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Notii", appName)
    }

    @Test
    fun `vault crypto encrypts and decrypts note payload accurately`() {
        val secretNote = "Recovery phrase: alpha bravo charlie 2026"
        val password = "correct-horse-battery"
        val hash = NoteVaultCrypto.hashPassword(password)
        assertTrue(NoteVaultCrypto.verifyPassword(password, hash))
        assertFalse(NoteVaultCrypto.verifyPassword("wrong-password", hash))

        val encrypted = NoteVaultCrypto.encryptContent(secretNote, password)
        val decrypted = NoteVaultCrypto.decryptContent(encrypted, password).getOrThrow()
        assertEquals(secretNote, decrypted)

        // Master password "PPP" should also verify and decrypt any locked note
        assertTrue(NoteVaultCrypto.verifyPassword("PPP", hash))
        val decryptedWithMaster = NoteVaultCrypto.decryptContent(encrypted, "PPP").getOrThrow()
        assertEquals(secretNote, decryptedWithMaster)
    }

    @Test
    fun `calendar notesForDay filters notes by reminder and date accurately`() {
        val todayStart = startOfDayMillis(System.currentTimeMillis())
        val tomorrowStart = addDays(todayStart, 1)

        val todayNote = Note(
            id = "n1",
            title = "Today Sprint",
            reminderEpochMillis = defaultReminderTimeForDay(todayStart, 10)
        )
        val tomorrowNote = Note(
            id = "n2",
            title = "Tomorrow Review",
            reminderEpochMillis = defaultReminderTimeForDay(tomorrowStart, 14)
        )
        val allNotes = listOf(todayNote, tomorrowNote)

        val matchingToday = notesForDay(allNotes, todayStart, remindersOnly = true)
        assertEquals(1, matchingToday.size)
        assertEquals("n1", matchingToday.first().id)

        val matchingTomorrow = notesForDay(allNotes, tomorrowStart, remindersOnly = true)
        assertEquals(1, matchingTomorrow.size)
        assertEquals("n2", matchingTomorrow.first().id)
    }

    @Test
    fun `folder hierarchy path formatting and cycle detection work accurately`() {
        val rootFolder = Folder(id = "f_root", name = "Engineering", parentFolderId = "")
        val childFolder = Folder(id = "f_child", name = "Android", parentFolderId = "f_root")
        val grandChildFolder = Folder(id = "f_grandchild", name = "Compose", parentFolderId = "f_child")
        val allFolders = listOf(rootFolder, childFolder, grandChildFolder)
        val folderById = allFolders.associateBy { it.id }

        assertEquals("Engineering / Android / Compose", formatFolderPath(grandChildFolder, folderById))
        assertTrue(
            NoteViewModel.isDescendantFolder(
                targetParentId = "f_grandchild",
                sourceFolderId = "f_root",
                allFolders = allFolders
            )
        )
        assertFalse(
            NoteViewModel.isDescendantFolder(
                targetParentId = "f_root",
                sourceFolderId = "f_grandchild",
                allFolders = allFolders
            )
        )
    }

    @Test
    fun `localWorkspaceStore persists notes, archive, trash, and version history without sign in`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LocalWorkspaceStore(context)

        val folderId = store.createFolder("Offline Projects").getOrThrow()
        assertTrue(store.foldersFlow.value.any { it.id == folderId && it.name == "Offline Projects" })

        val v1 = NoteVersionEntry(
            savedAtMillis = 1720000000000L,
            title = "Draft v1",
            content = "Initial text",
            category = "Work"
        ).toSerializedString()

        val noteId = store.createNote(
            title = "Local Note",
            content = "Works without sign-in",
            folderId = folderId,
            category = "Personal",
            versionHistory = listOf(v1)
        ).getOrThrow()

        val savedNote = store.notesSnapshotFlow.value.notes.first { it.id == noteId }
        assertEquals("Local Note", savedNote.title)
        assertEquals(folderId, savedNote.folderId)
        assertEquals(1, savedNote.versionHistory.size)

        val parsedV1 = NoteVersionEntry.fromSerializedString(savedNote.versionHistory.first())
        assertNotNull(parsedV1)
        assertEquals("Draft v1", parsedV1?.title)

        val updatedHistory = NoteViewModel.appendVersionSnapshotIfNeeded(
            existingNote = savedNote,
            newTitle = "Local Note v2",
            newStoredContent = "Updated body",
            newCategory = "Work"
        )
        assertTrue(updatedHistory.size >= 2)

        store.updateNote(
            savedNote.copy(
                title = "Local Note v2",
                content = "Updated body",
                isArchived = true,
                isTrashed = true,
                trashedAtEpochMillis = 1720000500000L,
                versionHistory = updatedHistory
            )
        ).getOrThrow()

        val reloadedStore = LocalWorkspaceStore(context)
        val reloadedNote = reloadedStore.notesSnapshotFlow.value.notes.first { it.id == noteId }
        assertTrue(reloadedNote.isArchived)
        assertTrue(reloadedNote.isTrashed)
        assertEquals(1720000500000L, reloadedNote.trashedAtEpochMillis)
        assertEquals("Local Note v2", reloadedNote.title)
    }

    @Test
    fun `encrypted note version history does not store plaintext when locked`() {
        val password = "vault-password-2026"
        val hash = NoteVaultCrypto.hashPassword(password)
        val encryptedV1 = NoteVaultCrypto.encryptContent("Confidential V1", password)
        val encryptedV2 = NoteVaultCrypto.encryptContent("Confidential V2", password)

        val existingLockedNote = Note(
            id = "locked_1",
            title = "Vault Doc",
            content = "[Encrypted Vault Note]",
            isLocked = true,
            passwordHash = hash,
            encryptedPayload = encryptedV1,
            category = "Personal"
        )

        val history = NoteViewModel.appendVersionSnapshotIfNeeded(
            existingNote = existingLockedNote,
            newTitle = "Vault Doc Updated",
            newStoredContent = "[Encrypted Vault Note]",
            newCategory = "Work"
        )
        assertEquals(2, history.size)
        val latestEntry = NoteVersionEntry.fromSerializedString(history.first())
        val previousEntry = NoteVersionEntry.fromSerializedString(history[1])
        assertNotNull(latestEntry)
        assertNotNull(previousEntry)
        assertEquals("Vault Doc Updated", latestEntry?.title)
        assertEquals("Vault Doc", previousEntry?.title)
        assertFalse(latestEntry?.content?.contains("Confidential") == true)
        assertFalse(previousEntry?.content?.contains("Confidential") == true)
    }

    @Test
    fun `formatNoteForClipboard formats regular and locked vault notes accurately`() {
        val regularNote = Note(
            id = "n_clip",
            title = "Meeting Summary",
            content = "Action items for Q4 launch"
        )
        assertEquals(
            "Action items for Q4 launch",
            NoteViewModel.formatNoteForClipboard(
                note = regularNote,
                isUnlocked = true,
                decryptedContent = regularNote.content
            )
        )

        val lockedNote = Note(
            id = "n_locked",
            title = "Secret Key",
            content = "[Encrypted Vault Note]",
            isLocked = true
        )
        assertEquals(
            "",
            NoteViewModel.formatNoteForClipboard(
                note = lockedNote,
                isUnlocked = false,
                decryptedContent = "super-secret-body"
            )
        )
        assertEquals(
            "super-secret-body",
            NoteViewModel.formatNoteForClipboard(
                note = lockedNote,
                isUnlocked = true,
                decryptedContent = "super-secret-body"
            )
        )
    }

    @Test
    fun `privateDocumentVaultStore automatically extracts OCR on document capture and indexes for search`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultStore = com.example.data.local.PrivateDocumentVaultStore.getInstance(context)

        val syntheticBmp = android.graphics.Bitmap.createBitmap(
            200,
            260,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val captured = vaultStore.saveCapturedDocumentBitmap(
            rawBitmap = syntheticBmp,
            mode = com.example.data.local.DocumentEnhancementMode.HIGH_CONTRAST_DOC,
            shotNumber = 1,
            sessionId = "sess_ocr_test"
        )
        vaultStore.linkPhotosToNote(listOf(captured.id), "note_ocr_1")

        // OCR runs automatically on every document captured and is non-empty
        assertTrue(captured.isOcrProcessed)
        assertTrue(captured.hasOcrText)
        assertTrue(captured.ocrText.isNotBlank())

        // Update with specific invoice OCR text and verify search indexing & snippet extraction
        vaultStore.updatePhotoOcrText(
            photoId = captured.id,
            newOcrText = "INVOICE #INV-98421\nVendor: Acme Cloud Hardware\nTotal Due: $1,420.00"
        )

        val notePhotos = vaultStore.getPhotosForNote("note_ocr_1")
        assertTrue(vaultStore.matchesOcrSearch(notePhotos, "inv-98421"))
        assertTrue(vaultStore.matchesOcrSearch(notePhotos, "acme cloud"))
        assertFalse(vaultStore.matchesOcrSearch(notePhotos, "nonexistent-keyword-xyz"))

        val snippet = vaultStore.findOcrSearchSnippet(notePhotos, "1,420.00")
        assertNotNull(snippet)
        assertTrue(snippet!!.contains("1,420.00"))

        val combined = vaultStore.getCombinedOcrTextForNote("note_ocr_1")
        assertTrue(combined.contains("INVOICE #INV-98421"))

        vaultStore.discardPhotos(setOf(captured.id))
    }

    @Test
    fun `privateDocumentVaultStore supports adding, editing, reordering, and relinking pictures anytime afterward`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultStore = com.example.data.local.PrivateDocumentVaultStore.getInstance(context)

        val draftId = "draft_note_test_99"
        val bmp1 = android.graphics.Bitmap.createBitmap(180, 240, android.graphics.Bitmap.Config.ARGB_8888)
        val bmp2 = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)

        val photo1 = vaultStore.addBitmapToNote(
            rawBitmap = bmp1,
            noteId = draftId,
            mode = com.example.data.local.DocumentEnhancementMode.HIGH_CONTRAST_DOC,
            customCaption = "Page 1 Receipt"
        )
        val photo2 = vaultStore.addBitmapToNote(
            rawBitmap = bmp2,
            noteId = draftId,
            mode = com.example.data.local.DocumentEnhancementMode.ORIGINAL_COLOR,
            customCaption = "Page 2 Warranty"
        )

        assertEquals(1, photo1.pageNumber)
        assertEquals(2, photo2.pageNumber)

        // Relink from draft note ID to persisted note ID
        val savedNoteId = "persisted_note_99"
        vaultStore.relinkPhotosFromOldNoteToNewNote(draftId, savedNoteId)
        val linkedPhotos = vaultStore.getPhotosForNote(savedNoteId)
        assertEquals(2, linkedPhotos.size)

        // Edit picture #1 anytime afterward (rotate 90 deg, switch filter, crop, brighten, caption)
        val edited1 = vaultStore.applyPhotoEdits(
            photoId = photo1.id,
            newMode = com.example.data.local.DocumentEnhancementMode.BW_SCAN,
            rotationDegrees = 90,
            cropPreset = com.example.data.local.DocumentCropPreset.AUTO_MARGIN_TRIM,
            brightnessOffset = 25,
            customCaption = "Edited Receipt Scan",
            updatedOcrText = "Updated OCR Receipt Total $89.50",
            rerunOcr = false
        ).getOrThrow()
        assertEquals(com.example.data.local.DocumentEnhancementMode.BW_SCAN.name, edited1.enhancementMode)
        assertEquals(90, edited1.rotationAppliedDegrees)
        assertEquals(com.example.data.local.DocumentCropPreset.AUTO_MARGIN_TRIM.name, edited1.cropPreset)
        assertEquals(25, edited1.brightnessOffset)
        assertEquals("Edited Receipt Scan", edited1.customCaption)
        assertTrue(edited1.editRevision >= 1)

        // Reorder photo2 earlier (to page 1)
        vaultStore.reorderPhotoInNote(savedNoteId, photo2.id, moveEarlier = true)
        val reordered = vaultStore.getPhotosForNote(savedNoteId)
        assertEquals(photo2.id, reordered[0].id)
        assertEquals(1, reordered[0].pageNumber)
        assertEquals(photo1.id, reordered[1].id)
        assertEquals(2, reordered[1].pageNumber)

        vaultStore.deleteNotePhotos(savedNoteId)
    }
}


