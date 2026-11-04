package com.example.data.util

import com.example.data.model.Category
import com.example.data.model.Folder
import com.example.data.model.Note
import com.google.firebase.Timestamp
import java.security.MessageDigest

data class CloudDeduplicationPlan(
    val duplicateCategoryIdsToDelete: Set<String> = emptySet(),
    val folderRemap: Map<String, String> = emptyMap(),
    val childFoldersToReparent: List<Folder> = emptyList(),
    val duplicateFolderIdsToDelete: Set<String> = emptySet(),
    val notesToUpdate: List<Note> = emptyList(),
    val photoNoteIdRemap: Map<String, String> = emptyMap(),
    val duplicateNoteIdsToDelete: Set<String> = emptySet()
) {
    val hasChanges: Boolean
        get() = duplicateCategoryIdsToDelete.isNotEmpty() ||
            duplicateFolderIdsToDelete.isNotEmpty() ||
            childFoldersToReparent.isNotEmpty() ||
            notesToUpdate.isNotEmpty() ||
            duplicateNoteIdsToDelete.isNotEmpty()
}

object NoteSyncDeduplicator {

    private val VALID_ID_REGEX = Regex("[^a-zA-Z0-9_\\-]")

    fun sanitizeDocumentId(rawId: String): String {
        val cleaned = rawId.trim().replace(VALID_ID_REGEX, "_")
        return cleaned.take(120)
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun deterministicCategoryId(name: String): String {
        val normalized = name.trim().lowercase()
        return "cat_${sha256Hex(normalized).take(24)}"
    }

    fun deterministicFolderId(name: String, parentFolderId: String): String {
        val normalized = "${parentFolderId.trim()}|${name.trim().lowercase()}"
        return "fld_${sha256Hex(normalized).take(24)}"
    }

    fun deterministicNoteId(note: Note, resolvedFolderId: String = note.folderId): String {
        val existingId = sanitizeDocumentId(note.id)
        if (existingId.isNotBlank() &&
            !existingId.startsWith("local_note_") &&
            !existingId.startsWith("draft_note_")
        ) {
            return existingId
        }
        val signature = noteContentSignature(note, resolvedFolderId)
        val seed = signature.ifBlank { existingId.ifBlank { "${note.title}|${note.content}" } }
        return "note_${sha256Hex(seed).take(24)}"
    }

    fun deterministicDraftCloudId(draftNoteId: String): String {
        val clean = sanitizeDocumentId(draftNoteId)
        if (clean.isBlank()) return ""
        return if (clean.startsWith("draft_note_")) {
            "note_${clean.removePrefix("draft_note_")}"
        } else {
            clean
        }
    }

    /**
     * Generates a normalized multi-device content signature for a note.
     * Blank notes (with neither title nor content/encryptedPayload) return "" so that
     * separate blank photo-capture notes are never merged by content alone.
     */
    fun noteContentSignature(note: Note, resolvedFolderId: String = note.folderId): String {
        val cleanTitle = note.title.trim()
        val cleanBody = if (note.isLocked) {
            note.encryptedPayload.trim()
        } else {
            note.content.trim()
        }
        if (cleanTitle.isEmpty() && cleanBody.isEmpty()) {
            return ""
        }
        val cleanCategory = note.category.trim().ifBlank { "General" }.lowercase()
        val cleanFolder = resolvedFolderId.trim()
        return buildString {
            append(cleanTitle)
            append('\u0000')
            append(cleanBody)
            append('\u0000')
            append(cleanCategory)
            append('\u0000')
            append(cleanFolder)
            append('\u0000')
            append(note.isLocked)
            append('\u0000')
            append(note.isTrashed)
            append('\u0000')
            append(note.isArchived)
        }
    }

    fun noteTimestampMillis(note: Note): Long {
        val ts: Timestamp = note.updatedAt ?: note.createdAt ?: return 0L
        return ts.seconds * 1000L + (ts.nanoseconds / 1_000_000L)
    }

    private fun pickCanonicalNoteWinner(candidates: List<Note>): Note {
        return candidates.sortedWith(
            compareByDescending<Note> { noteTimestampMillis(it) }
                .thenByDescending { it.versionHistory.size }
                .thenByDescending { it.tags.size }
                .thenByDescending { it.reminderEpochMillis }
                .thenBy { it.id }
        ).first()
    }

    fun mergeDuplicateNotesIntoWinner(winner: Note, duplicates: List<Note>): Note {
        if (duplicates.isEmpty()) return winner
        val mergedTags = (winner.tags + duplicates.flatMap { it.tags })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)
        val mergedHistory = (winner.versionHistory + duplicates.flatMap { it.versionHistory })
            .filter { it.isNotBlank() }
            .distinct()
            .take(10)
        val mergedReminderMillis = if (winner.reminderEpochMillis > 0L) {
            winner.reminderEpochMillis
        } else {
            duplicates.maxOfOrNull { it.reminderEpochMillis } ?: 0L
        }
        val mergedReminderLabel = winner.reminderLabel.ifBlank {
            duplicates.firstOrNull { it.reminderLabel.isNotBlank() }?.reminderLabel.orEmpty()
        }
        return winner.copy(
            tags = mergedTags,
            versionHistory = mergedHistory,
            reminderEpochMillis = mergedReminderMillis,
            reminderLabel = mergedReminderLabel
        )
    }

    /**
     * Deduplicates notes in-memory so the UI and widgets never render duplicate items
     * while multi-device sync is reconciling.
     */
    fun deduplicateNotesForDisplay(notes: List<Note>): List<Note> {
        if (notes.size <= 1) return notes
        val uniqueById = notes.distinctBy { it.id }
        val result = mutableListOf<Note>()
        val groupedBySignature = LinkedHashMap<String, MutableList<Note>>()

        for (note in uniqueById) {
            val sig = noteContentSignature(note)
            if (sig.isEmpty()) {
                result.add(note)
            } else {
                groupedBySignature.getOrPut(sig) { mutableListOf() }.add(note)
            }
        }

        for ((_, group) in groupedBySignature) {
            if (group.size == 1) {
                result.add(group.first())
            } else {
                val winner = pickCanonicalNoteWinner(group)
                val others = group.filterNot { it.id == winner.id }
                result.add(mergeDuplicateNotesIntoWinner(winner, others))
            }
        }

        return result.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
        )
    }

    fun deduplicateFoldersForDisplay(folders: List<Folder>): List<Folder> {
        if (folders.size <= 1) return folders
        return folders
            .distinctBy { it.id }
            .groupBy { "${it.parentFolderId.trim()}|${it.name.trim().lowercase()}" }
            .values
            .map { group ->
                group.sortedWith(
                    compareBy<Folder> { (it.createdAt ?: Timestamp(Long.MAX_VALUE, 0)).seconds }
                        .thenBy { it.id }
                ).first()
            }
            .sortedBy { it.name.lowercase() }
    }

    fun deduplicateCategoriesForDisplay(categories: List<Category>): List<Category> {
        if (categories.size <= 1) return categories
        return categories
            .distinctBy { it.id }
            .groupBy { it.name.trim().lowercase() }
            .values
            .map { group ->
                group.sortedWith(
                    compareBy<Category> { (it.createdAt ?: Timestamp(Long.MAX_VALUE, 0)).seconds }
                        .thenBy { it.id }
                ).first()
            }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Builds a complete reconciliation plan for cleaning up duplicate categories, folders,
     * and notes across multiple devices in Firestore.
     */
    fun planCloudDeduplication(
        notes: List<Note>,
        folders: List<Folder>,
        categories: List<Category>
    ): CloudDeduplicationPlan {
        // 1. Categories deduplication by normalized name
        val duplicateCategoryIds = mutableSetOf<String>()
        categories
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .groupBy { it.name.trim().lowercase() }
            .forEach { (_, group) ->
                if (group.size > 1) {
                    val sorted = group.sortedWith(
                        compareBy<Category> { (it.createdAt ?: Timestamp(Long.MAX_VALUE, 0)).seconds }
                            .thenBy { it.id }
                    )
                    sorted.drop(1).forEach { duplicateCategoryIds.add(it.id) }
                }
            }

        // 2. Folders deduplication by (parentFolderId, normalized name)
        val folderRemap = mutableMapOf<String, String>()
        val duplicateFolderIds = mutableSetOf<String>()
        folders
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .groupBy { "${it.parentFolderId.trim()}|${it.name.trim().lowercase()}" }
            .forEach { (_, group) ->
                if (group.size > 1) {
                    val sorted = group.sortedWith(
                        compareBy<Folder> { (it.createdAt ?: Timestamp(Long.MAX_VALUE, 0)).seconds }
                            .thenBy { it.id }
                    )
                    val winner = sorted.first()
                    sorted.drop(1).forEach { dup ->
                        folderRemap[dup.id] = winner.id
                        duplicateFolderIds.add(dup.id)
                    }
                }
            }

        val childFoldersToReparent = folders
            .filter { !duplicateFolderIds.contains(it.id) && folderRemap.containsKey(it.parentFolderId) }
            .map { folder ->
                folder.copy(parentFolderId = folderRemap[folder.parentFolderId] ?: folder.parentFolderId)
            }

        // 3. Notes deduplication by normalized content signature (after applying folderRemap)
        val notesToUpdateById = LinkedHashMap<String, Note>()
        val duplicateNoteIds = mutableSetOf<String>()
        val photoNoteIdRemap = mutableMapOf<String, String>()

        val remappedNotes = notes.filter { it.id.isNotBlank() }.map { note ->
            val targetFolderId = folderRemap[note.folderId] ?: note.folderId
            if (targetFolderId != note.folderId) {
                val updated = note.copy(folderId = targetFolderId)
                notesToUpdateById[updated.id] = updated
                updated
            } else {
                note
            }
        }

        val signatureGroups = remappedNotes
            .map { it to noteContentSignature(it, it.folderId) }
            .filter { (_, sig) -> sig.isNotEmpty() }
            .groupBy({ it.second }, { it.first })

        signatureGroups.forEach { (_, group) ->
            if (group.size > 1) {
                val winner = pickCanonicalNoteWinner(group)
                val duplicates = group.filterNot { it.id == winner.id }
                val mergedWinner = mergeDuplicateNotesIntoWinner(winner, duplicates)

                duplicates.forEach { dup ->
                    duplicateNoteIds.add(dup.id)
                    notesToUpdateById.remove(dup.id)
                    photoNoteIdRemap[dup.id] = winner.id
                }

                if (mergedWinner != winner || notesToUpdateById.containsKey(winner.id)) {
                    notesToUpdateById[winner.id] = mergedWinner
                }
            }
        }

        return CloudDeduplicationPlan(
            duplicateCategoryIdsToDelete = duplicateCategoryIds,
            folderRemap = folderRemap,
            childFoldersToReparent = childFoldersToReparent,
            duplicateFolderIdsToDelete = duplicateFolderIds,
            notesToUpdate = notesToUpdateById.values.toList(),
            photoNoteIdRemap = photoNoteIdRemap,
            duplicateNoteIdsToDelete = duplicateNoteIds
        )
    }
}
