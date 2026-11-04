package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Category
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteSyncSnapshot
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

    fun observeNotesWithMetadata(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<NoteSyncSnapshot> {
        val path = "users/$userId/notes"
        return db.collection("users").document(userId).collection("notes")
            .whereEqualTo("userId", userId)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val notes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Note::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedWith(
                    compareByDescending<Note> { it.isPinned }
                        .thenByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
                )
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
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.name.lowercase() }
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
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Folder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.name.lowercase() }
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
                it.toObject(Note::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(notes)
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
            val note = doc.toObject(Note::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
        versionHistory: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val notesCol = db.collection("users").document(uid).collection("notes")
        val docRef = notesCol.document()
        val boundedTags = tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10)
        val boundedHistory = versionHistory.map { it.take(5000) }.take(10)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(200),
            "content" to content.take(20000),
            "folderId" to folderId.trim().take(128),
            "category" to category.ifBlank { "General" }.take(50),
            "tags" to boundedTags,
            "colorHex" to colorHex.take(20),
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
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
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
        requireNotNull(note.createdAt) { "Note createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("notes").document(note.id)
        val boundedTags = note.tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(10)
        val boundedHistory = note.versionHistory.map { it.take(5000) }.take(10)
        val updatedModel = note.copy(
            userId = uid,
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
            updatedAt = null
        )
        return try {
            docRef.set(updatedModel).await()
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

    suspend fun createCategory(name: String, colorHex: String = "#F59E0B"): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("categories").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(40),
            "colorHex" to colorHex.take(20),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
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
        colorHex: String = "#F59E0B"
    ): Result<String> {
        val uid = requireUserId()
        val cleanName = name.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Folder name cannot be blank"))
        }
        val docRef = db.collection("users").document(uid).collection("folders").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to cleanName,
            "parentFolderId" to parentFolderId.trim().take(128),
            "colorHex" to colorHex.ifBlank { "#F59E0B" }.take(20),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
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
        requireNotNull(folder.createdAt) { "Folder createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("folders").document(folder.id)
        val updatedModel = folder.copy(
            userId = uid,
            name = folder.name.trim().ifBlank { "Untitled Folder" }.take(60),
            parentFolderId = folder.parentFolderId.trim().take(128),
            colorHex = folder.colorHex.ifBlank { "#F59E0B" }.take(20),
            updatedAt = null
        )
        return try {
            docRef.set(updatedModel).await()
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
