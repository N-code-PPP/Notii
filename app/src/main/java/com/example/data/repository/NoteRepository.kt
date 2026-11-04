package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteSyncSnapshot
import com.example.data.util.NoteSyncDeduplicator
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NoteRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun DocumentSnapshot.toNoteModel(): Note? {
        if (!exists()) return null
        val rawTags = (get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val rawHistory = (get("versionHistory") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return Note(
            id = id,
            userId = getString("userId") ?: "",
            title = getString("title") ?: "",
            content = getString("content") ?: "",
            folderId = getString("folderId") ?: "",
            category = getString("category")?.ifBlank { "General" } ?: "General",
            tags = rawTags,
            colorHex = getString("colorHex")?.ifBlank { "#1E293B" } ?: "#1E293B",
            isPinned = getBoolean("isPinned") ?: getBoolean("pinned") ?: false,
            isArchived = getBoolean("isArchived") ?: getBoolean("archived") ?: false,
            isTrashed = getBoolean("isTrashed") ?: getBoolean("trashed") ?: false,
            trashedAtEpochMillis = getLong("trashedAtEpochMillis") ?: 0L,
            isLocked = getBoolean("isLocked") ?: getBoolean("locked") ?: false,
            passwordHash = getString("passwordHash") ?: "",
            encryptedPayload = getString("encryptedPayload") ?: "",
            reminderEpochMillis = getLong("reminderEpochMillis") ?: 0L,
            reminderLabel = getString("reminderLabel") ?: "",
            versionHistory = rawHistory,
            createdAt = getTimestamp("createdAt", estimate),
            updatedAt = getTimestamp("updatedAt", estimate)
        )
    }

    private fun DocumentSnapshot.toFolderModel(): Folder? {
        if (!exists()) return null
        val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return Folder(
            id = id,
            userId = getString("userId") ?: "",
            name = getString("name") ?: "",
            parentFolderId = getString("parentFolderId") ?: "",
            colorHex = getString("colorHex")?.ifBlank { "#F59E0B" } ?: "#F59E0B",
            createdAt = getTimestamp("createdAt", estimate),
            updatedAt = getTimestamp("updatedAt", estimate)
        )
    }

    private fun DocumentSnapshot.toCategoryModel(): Category? {
        if (!exists()) return null
        val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return Category(
            id = id,
            userId = getString("userId") ?: "",
            name = getString("name") ?: "",
            colorHex = getString("colorHex")?.ifBlank { "#F59E0B" } ?: "#F59E0B",
            createdAt = getTimestamp("createdAt", estimate)
        )
    }

    fun observeNotesWithMetadata(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<NoteSyncSnapshot> {
        val path = "users/$userId/notes"
        return db.collection("users").document(userId).collection("notes")
            .whereEqualTo("userId", userId)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val rawNotes = snapshot.documents.mapNotNull { doc ->
                    doc.toNoteModel()
                }
                val notes = NoteSyncDeduplicator.deduplicateNotesForDisplay(rawNotes)
                NoteSyncSnapshot(
                    notes = notes,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    isFromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
                    ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
                    ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
                if (unwrapped != null) {
                    handleFirestoreError(unwrapped, OperationType.LIST, path)
                    throw unwrapped
                }
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeNotes(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<Note>> =
        observeNotesWithMetadata(userId).map { it.notes }

    fun observeCategories(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<Category>> {
        val path = "users/$userId/categories"
        return db.collection("users").document(userId).collection("categories")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                val rawCategories = snapshot.documents.mapNotNull { doc ->
                    doc.toCategoryModel()
                }
                NoteSyncDeduplicator.deduplicateCategoriesForDisplay(rawCategories)
            }
            .catch { error ->
                val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
                    ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
                    ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
                if (unwrapped != null) {
                    handleFirestoreError(unwrapped, OperationType.LIST, path)
                    throw unwrapped
                }
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeFolders(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<Folder>> {
        val path = "users/$userId/folders"
        return db.collection("users").document(userId).collection("folders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                val rawFolders = snapshot.documents.mapNotNull { doc ->
                    doc.toFolderModel()
                }
                NoteSyncDeduplicator.deduplicateFoldersForDisplay(rawFolders)
            }
            .catch { error ->
                val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
                    ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
                    ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
                if (unwrapped != null) {
                    handleFirestoreError(unwrapped, OperationType.LIST, path)
                    throw unwrapped
                }
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getUserNotes(userId: String = requireUserId()): Result<List<Note>> {
        val path = "users/$userId/notes"
        return try {
            val snapshot = db.collection("users").document(userId).collection("notes")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val notes = snapshot.documents.mapNotNull {
                it.toNoteModel()
            }
            Result.success(notes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getUserFolders(userId: String = requireUserId()): Result<List<Folder>> {
        val path = "users/$userId/folders"
        return try {
            val snapshot = db.collection("users").document(userId).collection("folders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val folders = snapshot.documents.mapNotNull {
                it.toFolderModel()
            }
            Result.success(folders)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getUserCategories(userId: String = requireUserId()): Result<List<Category>> {
        val path = "users/$userId/categories"
        return try {
            val snapshot = db.collection("users").document(userId).collection("categories")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val categories = snapshot.documents.mapNotNull {
                it.toCategoryModel()
            }
            Result.success(categories)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getNoteById(noteId: String, targetUserId: String = requireUserId()): Result<Note> {
        val docRef = db.collection("users").document(targetUserId).collection("notes").document(noteId)
        return try {
            val doc = docRef.get().await()
            val note = doc.toNoteModel()
                ?: throw NoSuchElementException("Note not found: $noteId")
            Result.success(note)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createNote(
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
        versionHistory: List<String> = emptyList(),
        preferredId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val notesCol = db.collection("users").document(uid).collection("notes")
        val cleanPreferredId = NoteSyncDeduplicator.sanitizeDocumentId(preferredId)
        val docRef = if (cleanPreferredId.isNotBlank()) {
            notesCol.document(cleanPreferredId)
        } else {
            notesCol.document()
        }
        val boundedTags = tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10)
        val boundedHistory = versionHistory.map { it.take(5000) }.take(10)
        val basePayload = mapOf<String, Any>(
            "title" to title.take(200),
            "content" to content.take(20000),
            "folderId" to folderId.trim().take(128),
            "category" to category.ifBlank { "General" }.take(50),
            "tags" to boundedTags,
            "colorHex" to colorHex.ifBlank { "#1E293B" }.take(20),
            "isPinned" to isPinned,
            "isArchived" to isArchived,
            "isTrashed" to isTrashed,
            "trashedAtEpochMillis" to trashedAtEpochMillis.coerceAtLeast(0L),
            "isLocked" to isLocked,
            "passwordHash" to passwordHash.take(256),
            "encryptedPayload" to encryptedPayload.take(30000),
            "reminderEpochMillis" to reminderEpochMillis.coerceAtLeast(0L),
            "reminderLabel" to reminderLabel.take(100),
            "versionHistory" to boundedHistory,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createPayload = basePayload + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            if (cleanPreferredId.isNotBlank()) {
                val existingDoc = runCatching { docRef.get().await() }.getOrNull()
                if (existingDoc != null && existingDoc.exists()) {
                    docRef.update(basePayload).await()
                } else {
                    docRef.set(createPayload).await()
                }
            } else {
                docRef.set(createPayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateNote(note: Note): Result<Unit> {
        val uid = requireUserId()
        require(note.id.isNotBlank()) { "Note ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("notes").document(note.id)
        val boundedTags = note.tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10)
        val boundedHistory = note.versionHistory.map { it.take(5000) }.take(10)
        val updatePayload = mapOf<String, Any>(
            "title" to note.title.take(200),
            "content" to note.content.take(20000),
            "folderId" to note.folderId.trim().take(128),
            "category" to note.category.ifBlank { "General" }.take(50),
            "tags" to boundedTags,
            "colorHex" to note.colorHex.ifBlank { "#1E293B" }.take(20),
            "isPinned" to note.isPinned,
            "isArchived" to note.isArchived,
            "isTrashed" to note.isTrashed,
            "trashedAtEpochMillis" to note.trashedAtEpochMillis.coerceAtLeast(0L),
            "isLocked" to note.isLocked,
            "passwordHash" to note.passwordHash.take(256),
            "encryptedPayload" to note.encryptedPayload.take(30000),
            "reminderEpochMillis" to note.reminderEpochMillis.coerceAtLeast(0L),
            "reminderLabel" to note.reminderLabel.take(100),
            "versionHistory" to boundedHistory,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            try {
                docRef.update(updatePayload).await()
            } catch (updateErr: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (updateErr.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                    val fullPayload = updatePayload + mapOf(
                        "userId" to uid,
                        "createdAt" to (note.createdAt ?: FieldValue.serverTimestamp())
                    )
                    docRef.set(fullPayload).await()
                } else {
                    throw updateErr
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createCategory(
        name: String,
        colorHex: String = "#F59E0B",
        preferredId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val cleanName = name.trim().take(40)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Category name cannot be blank"))
        }
        val cleanPreferredId = NoteSyncDeduplicator.sanitizeDocumentId(preferredId)
        val categoriesCol = db.collection("users").document(uid).collection("categories")
        val docRef = if (cleanPreferredId.isNotBlank()) {
            categoriesCol.document(cleanPreferredId)
        } else {
            categoriesCol.document()
        }
        val payload = mapOf(
            "userId" to uid,
            "name" to cleanName,
            "colorHex" to colorHex.ifBlank { "#F59E0B" }.take(20),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            if (cleanPreferredId.isNotBlank()) {
                val existingDoc = runCatching { docRef.get().await() }.getOrNull()
                if (existingDoc == null || !existingDoc.exists()) {
                    docRef.set(payload).await()
                }
            } else {
                docRef.set(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("categories").document(categoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createFolder(
        name: String,
        parentFolderId: String = "",
        colorHex: String = "#F59E0B",
        preferredId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Folder name cannot be blank"))
        }
        val cleanPreferredId = NoteSyncDeduplicator.sanitizeDocumentId(preferredId)
        val foldersCol = db.collection("users").document(uid).collection("folders")
        val docRef = if (cleanPreferredId.isNotBlank()) {
            foldersCol.document(cleanPreferredId)
        } else {
            foldersCol.document()
        }
        val basePayload = mapOf<String, Any>(
            "name" to cleanName,
            "parentFolderId" to parentFolderId.trim().take(128),
            "colorHex" to colorHex.ifBlank { "#F59E0B" }.take(20),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createPayload = basePayload + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            if (cleanPreferredId.isNotBlank()) {
                val existingDoc = runCatching { docRef.get().await() }.getOrNull()
                if (existingDoc != null && existingDoc.exists()) {
                    docRef.update(basePayload).await()
                } else {
                    docRef.set(createPayload).await()
                }
            } else {
                docRef.set(createPayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateFolder(folder: Folder): Result<Unit> {
        val uid = requireUserId()
        require(folder.id.isNotBlank()) { "Folder ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("folders").document(folder.id)
        val updatePayload = mapOf<String, Any>(
            "name" to folder.name.trim().ifBlank { "Untitled Folder" }.take(60),
            "parentFolderId" to folder.parentFolderId.trim().take(128),
            "colorHex" to folder.colorHex.ifBlank { "#F59E0B" }.take(20),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            try {
                docRef.update(updatePayload).await()
            } catch (updateErr: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (updateErr.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                    val fullPayload = updatePayload + mapOf(
                        "userId" to uid,
                        "createdAt" to (folder.createdAt ?: FieldValue.serverTimestamp())
                    )
                    docRef.set(fullPayload).await()
                } else {
                    throw updateErr
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteFolder(folderId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("folders").document(folderId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
