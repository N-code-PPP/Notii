package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONObject

@IgnoreExtraProperties
data class Note(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val folderId: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val colorHex: String = "#1E293B",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAtEpochMillis: Long = 0L,
    val isLocked: Boolean = false,
    val passwordHash: String = "",
    val encryptedPayload: String = "",
    val reminderEpochMillis: Long = 0L,
    val reminderLabel: String = "",
    val versionHistory: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class NoteVersionEntry(
    val savedAtMillis: Long,
    val title: String,
    val content: String,
    val category: String
) {
    fun toSerializedString(): String {
        return JSONObject().apply {
            put("savedAtMillis", savedAtMillis)
            put("title", title.take(200))
            put("content", content.take(3500))
            put("category", category.take(50))
        }.toString()
    }

    companion object {
        fun fromSerializedString(raw: String): NoteVersionEntry? {
            return try {
                val obj = JSONObject(raw)
                NoteVersionEntry(
                    savedAtMillis = obj.optLong("savedAtMillis", 0L),
                    title = obj.optString("title", ""),
                    content = obj.optString("content", ""),
                    category = obj.optString("category", "General")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#F59E0B",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Folder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val parentFolderId: String = "",
    val colorHex: String = "#F59E0B",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class NoteSyncSnapshot(
    val notes: List<Note> = emptyList(),
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false
)
