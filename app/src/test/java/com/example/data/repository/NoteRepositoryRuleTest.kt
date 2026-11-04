package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.NoteVersionEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NoteRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createNote_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = NoteRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createNote(
                title = NOTE_TITLE,
                content = "Quick thought with cloud sync",
                folderId = "folder_1",
                category = "Work",
                tags = listOf("important", "sync")
            )
        }
        assertTrue(createResult.isSuccess)
        val noteId = createResult.getOrThrow()
        assertTrue(noteId.isNotEmpty())
    }

    @Test
    fun archiveTrashAndVersionHistory_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoteRepository(firestore, auth)

        val v1 = NoteVersionEntry(
            savedAtMillis = 1720000000000L,
            title = "Initial Spec",
            content = "Draft 1",
            category = "Work"
        ).toSerializedString()

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createNote(
                title = "Initial Spec",
                content = "Draft 1",
                category = "Work",
                versionHistory = listOf(v1)
            ).getOrThrow()
        }

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getNoteById(noteId, aliceUid).getOrThrow()
        }

        val v2 = NoteVersionEntry(
            savedAtMillis = 1720000100000L,
            title = "Revised Spec",
            content = "Draft 2",
            category = "Work"
        ).toSerializedString()

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateNote(
                fetched.copy(
                    title = "Revised Spec",
                    content = "Draft 2",
                    isArchived = true,
                    isTrashed = true,
                    trashedAtEpochMillis = 1720000200000L,
                    versionHistory = listOf(v2, v1)
                )
            )
        }
        assertTrue(updateRes.isSuccess)

        val updatedNote = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getNoteById(noteId, aliceUid).getOrThrow()
        }
        assertTrue(updatedNote.isArchived)
        assertTrue(updatedNote.isTrashed)
        assertEquals(2, updatedNote.versionHistory.size)
    }

    @Test
    fun createRenameMoveAndDeleteFolder_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoteRepository(firestore, auth)

        val parentFolderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFolder(name = "Projects").getOrThrow()
        }
        val childFolderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFolder(name = "Q4 Roadmap", parentFolderId = parentFolderId).getOrThrow()
        }

        val emittedFolders = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeFolders(aliceUid).first { list -> list.size >= 2 }
        }
        val childFolder = emittedFolders.first { it.id == childFolderId }
        assertEquals(parentFolderId, childFolder.parentFolderId)

        val renameMoveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateFolder(childFolder.copy(name = "Q4 Shipped", parentFolderId = ""))
        }
        assertTrue(renameMoveResult.isSuccess)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteFolder(childFolderId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun getUserNotes_authenticatedOwner_returnsMatchingNotes() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoteRepository(firestore, auth)
        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createNote(
                title = NOTE_TITLE,
                content = "Body text",
                category = "General"
            ).getOrThrow()
        }

        val notesResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserNotes(aliceUid) }
        assertTrue(notesResult.isSuccess)
        assertTrue(notesResult.getOrThrow().map { it.id }.contains(noteId))
    }

    @Test
    fun observeNotes_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NoteRepository(firestore, auth)
        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createNote(
                title = NOTE_TITLE,
                content = "Realtime test"
            ).getOrThrow()
        }

        val emittedNotes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeNotes(aliceUid).first { list -> list.any { it.id == noteId } }
        }
        assertTrue(emittedNotes.map { it.id }.contains(noteId))
    }

    @Test
    fun getNoteById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NoteRepository(firestore, auth)
        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createNote(title = NOTE_TITLE, content = "Secret").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NoteRepository(firestore, auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getNoteById(noteId, targetUserId = aliceUid).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeNotes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = NoteRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeNotes("unauthenticated_user").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val NOTE_TITLE = "Sample Note"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
