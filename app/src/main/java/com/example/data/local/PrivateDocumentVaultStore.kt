package com.example.data.local

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.widget.QuickCameraWidgetProvider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class DocumentEnhancementMode(val label: String, val shortLabel: String) {
    HIGH_CONTRAST_DOC("Auto High-Contrast Document", "Crisp Doc"),
    BW_SCAN("B&W Text Scan", "B&W Scan"),
    ORIGINAL_COLOR("Original Color Document", "Color")
}

enum class DocumentCropPreset(val label: String, val shortLabel: String, val marginFraction: Float) {
    FULL_FRAME("Full Frame (No Crop)", "Full Frame", 0f),
    AUTO_MARGIN_TRIM("Document Margin Trim (6%)", "Trim Margins", 0.06f),
    TIGHT_RECEIPT_CROP("Tight Focus Crop (14%)", "Tight Crop", 0.14f)
}

data class CapturedDocumentPhoto(
    val id: String,
    val filePath: String,
    val sessionId: String,
    val noteId: String = "",
    val pageNumber: Int = 1,
    val capturedAtMillis: Long = System.currentTimeMillis(),
    val enhancementMode: String = DocumentEnhancementMode.HIGH_CONTRAST_DOC.name,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val ocrText: String = "",
    val isOcrProcessed: Boolean = true,
    val customCaption: String = "",
    val rotationAppliedDegrees: Int = 0,
    val cropPreset: String = DocumentCropPreset.FULL_FRAME.name,
    val brightnessOffset: Int = 0,
    val editRevision: Int = 0
) {
    val fileExists: Boolean
        get() = filePath.isNotBlank() && File(filePath).exists()

    val title: String
        get() = customCaption.trim().ifBlank { "Document Page #$pageNumber" }

    val hasOcrText: Boolean
        get() = ocrText.isNotBlank()
}

class PrivateDocumentVaultStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val vaultDir: File = File(appContext.filesDir, PRIVATE_DIR_NAME).apply {
        if (!exists()) {
            mkdirs()
        }
        val noMedia = File(this, ".nomedia")
        if (!noMedia.exists()) {
            runCatching { noMedia.createNewFile() }
        }
    }

    private val _photosFlow = MutableStateFlow(loadPhotosFromPrefs())
    val photosFlow: StateFlow<List<CapturedDocumentPhoto>> = _photosFlow.asStateFlow()

    init {
        QuickCameraWidgetProvider.syncCameraWidget(appContext, _photosFlow.value.size)
    }

    fun getPrivateDirectory(): File = vaultDir

    suspend fun saveCapturedDocumentBitmapWithOcr(
        rawBitmap: Bitmap,
        mode: DocumentEnhancementMode,
        shotNumber: Int,
        sessionId: String,
        rotationDegrees: Int = 0
    ): CapturedDocumentPhoto {
        val rotated = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }
        val optimized = optimizeBitmapForDocument(rotated, mode)
        val extractedText = DocumentOcrExtractor.extractTextFromBitmap(
            bitmap = optimized,
            shotNumber = shotNumber,
            mode = mode
        )
        return persistOptimizedDocumentBitmap(
            optimized = optimized,
            rawUprightSource = rotated,
            mode = mode,
            shotNumber = shotNumber,
            sessionId = sessionId,
            extractedOcrText = extractedText
        )
    }

    fun saveCapturedDocumentBitmap(
        rawBitmap: Bitmap,
        mode: DocumentEnhancementMode,
        shotNumber: Int,
        sessionId: String,
        rotationDegrees: Int = 0,
        customOcrText: String? = null
    ): CapturedDocumentPhoto {
        val rotated = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        } else {
            rawBitmap
        }

        val optimized = optimizeBitmapForDocument(rotated, mode)
        val resolvedOcrText = customOcrText?.trim()?.takeIf { it.isNotEmpty() }
            ?: DocumentOcrExtractor.extractTextSynchronouslyOrDefault(
                bitmap = optimized,
                shotNumber = shotNumber,
                mode = mode
            )

        return persistOptimizedDocumentBitmap(
            optimized = optimized,
            rawUprightSource = rotated,
            mode = mode,
            shotNumber = shotNumber,
            sessionId = sessionId,
            extractedOcrText = resolvedOcrText
        )
    }

    suspend fun importUriToNoteWithOcr(
        uri: android.net.Uri,
        noteId: String = "",
        mode: DocumentEnhancementMode = DocumentEnhancementMode.ORIGINAL_COLOR
    ): Result<CapturedDocumentPhoto> {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = calculateImportSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_DOCUMENT_DIMENSION)
            }
            val rawBitmap = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: throw IllegalArgumentException("Unable to decode selected picture.")

            val existingForNote = if (noteId.isBlank()) emptyList() else getPhotosForNote(noteId)
            val nextPageNumber = (existingForNote.maxOfOrNull { it.pageNumber } ?: existingForNote.size) + 1
            val captured = saveCapturedDocumentBitmapWithOcr(
                rawBitmap = rawBitmap,
                mode = mode,
                shotNumber = nextPageNumber,
                sessionId = "import_${System.currentTimeMillis()}"
            )
            if (noteId.isNotBlank()) {
                appendPhotosToNote(listOf(captured.id), noteId)
                _photosFlow.value.firstOrNull { it.id == captured.id } ?: captured.copy(noteId = noteId)
            } else {
                captured
            }
        }
    }

    fun addBitmapToNote(
        rawBitmap: Bitmap,
        noteId: String,
        mode: DocumentEnhancementMode = DocumentEnhancementMode.HIGH_CONTRAST_DOC,
        customCaption: String = "",
        customOcrText: String? = null
    ): CapturedDocumentPhoto {
        val existingForNote = if (noteId.isBlank()) emptyList() else getPhotosForNote(noteId)
        val nextPageNumber = (existingForNote.maxOfOrNull { it.pageNumber } ?: existingForNote.size) + 1
        val captured = saveCapturedDocumentBitmap(
            rawBitmap = rawBitmap,
            mode = mode,
            shotNumber = nextPageNumber,
            sessionId = "note_add_${System.currentTimeMillis()}",
            customOcrText = customOcrText
        )
        if (noteId.isNotBlank()) {
            appendPhotosToNote(listOf(captured.id), noteId)
        }
        if (customCaption.isNotBlank()) {
            applyPhotoEdits(
                photoId = captured.id,
                newMode = mode,
                rotationDegrees = 0,
                cropPreset = DocumentCropPreset.FULL_FRAME,
                brightnessOffset = 0,
                customCaption = customCaption
            )
        }
        return _photosFlow.value.firstOrNull { it.id == captured.id } ?: captured
    }

    private fun persistOptimizedDocumentBitmap(
        optimized: Bitmap,
        rawUprightSource: Bitmap? = null,
        mode: DocumentEnhancementMode,
        shotNumber: Int,
        sessionId: String,
        extractedOcrText: String
    ): CapturedDocumentPhoto {
        val photoId = "doc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val targetFile = File(vaultDir, "$photoId.jpg")
        val masterFile = File(vaultDir, "${photoId}_master.jpg")

        FileOutputStream(targetFile).use { out ->
            optimized.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }

        // Save upright unfiltered source bitmap so user can non-destructively edit filter/crop/rotation anytime later
        runCatching {
            val masterBmp = rawUprightSource ?: optimized
            FileOutputStream(masterFile).use { out ->
                masterBmp.compress(Bitmap.CompressFormat.JPEG, 93, out)
                out.flush()
            }
        }

        val record = CapturedDocumentPhoto(
            id = photoId,
            filePath = targetFile.absolutePath,
            sessionId = sessionId,
            noteId = "",
            pageNumber = shotNumber,
            capturedAtMillis = System.currentTimeMillis(),
            enhancementMode = mode.name,
            width = optimized.width,
            height = optimized.height,
            sizeBytes = targetFile.length(),
            ocrText = extractedOcrText.trim(),
            isOcrProcessed = true
        )

        val updated = listOf(record) + _photosFlow.value
        savePhotosToPrefs(updated)
        _photosFlow.value = updated
        QuickCameraWidgetProvider.syncCameraWidget(appContext, updated.size)
        return record
    }

    fun applyPhotoEdits(
        photoId: String,
        newMode: DocumentEnhancementMode? = null,
        rotationDegrees: Int? = null,
        cropPreset: DocumentCropPreset? = null,
        brightnessOffset: Int? = null,
        customCaption: String? = null,
        updatedOcrText: String? = null,
        rerunOcr: Boolean = false
    ): Result<CapturedDocumentPhoto> {
        val existing = _photosFlow.value.firstOrNull { it.id == photoId }
            ?: return Result.failure(IllegalArgumentException("Picture not found in private vault."))

        return runCatching {
            val targetMode = newMode ?: runCatching {
                DocumentEnhancementMode.valueOf(existing.enhancementMode)
            }.getOrDefault(DocumentEnhancementMode.HIGH_CONTRAST_DOC)

            val targetRotation = ((rotationDegrees ?: existing.rotationAppliedDegrees) % 360 + 360) % 360
            val targetCrop = cropPreset ?: runCatching {
                DocumentCropPreset.valueOf(existing.cropPreset)
            }.getOrDefault(DocumentCropPreset.FULL_FRAME)
            val targetBrightness = (brightnessOffset ?: existing.brightnessOffset).coerceIn(-60, 60)
            val targetCaption = customCaption?.trim() ?: existing.customCaption

            val geometryOrFilterChanged =
                targetMode.name != existing.enhancementMode ||
                    targetRotation != existing.rotationAppliedDegrees ||
                    targetCrop.name != existing.cropPreset ||
                    targetBrightness != existing.brightnessOffset

            val targetFile = File(existing.filePath)
            val masterFile = File(vaultDir, "${existing.id}_master.jpg")

            var newW = existing.width
            var newH = existing.height
            var newSize = existing.sizeBytes
            var finalOcr = updatedOcrText?.trim() ?: existing.ocrText

            if (geometryOrFilterChanged || rerunOcr) {
                if (!masterFile.exists() && targetFile.exists()) {
                    runCatching { targetFile.copyTo(masterFile, overwrite = true) }
                }
                val sourceFile = if (masterFile.exists()) masterFile else targetFile
                val masterBmp = BitmapFactory.decodeFile(sourceFile.absolutePath)
                    ?: createDocumentScanFallbackBitmap(existing.pageNumber, targetMode)

                val editedBmp = renderEditedBitmap(
                    source = masterBmp,
                    mode = targetMode,
                    rotationDegrees = targetRotation,
                    cropPreset = targetCrop,
                    brightnessOffset = targetBrightness
                )

                FileOutputStream(targetFile).use { out ->
                    editedBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    out.flush()
                }

                newW = editedBmp.width
                newH = editedBmp.height
                newSize = targetFile.length()

                if (rerunOcr) {
                    finalOcr = DocumentOcrExtractor.extractTextSynchronouslyOrDefault(
                        bitmap = editedBmp,
                        shotNumber = existing.pageNumber,
                        mode = targetMode
                    )
                }
            }

            val updatedRecord = existing.copy(
                enhancementMode = targetMode.name,
                width = newW,
                height = newH,
                sizeBytes = newSize,
                ocrText = finalOcr,
                isOcrProcessed = true,
                customCaption = targetCaption,
                rotationAppliedDegrees = targetRotation,
                cropPreset = targetCrop.name,
                brightnessOffset = targetBrightness,
                editRevision = existing.editRevision + 1
            )

            val updatedList = _photosFlow.value.map { photo ->
                if (photo.id == photoId) updatedRecord else photo
            }
            savePhotosToPrefs(updatedList)
            _photosFlow.value = updatedList
            updatedRecord
        }
    }

    fun reorderPhotoInNote(noteId: String, photoId: String, moveEarlier: Boolean): Boolean {
        if (noteId.isBlank() || photoId.isBlank()) return false
        val notePhotos = getPhotosForNote(noteId)
        if (notePhotos.size < 2) return false
        val currentIndex = notePhotos.indexOfFirst { it.id == photoId }
        if (currentIndex == -1) return false
        val targetIndex = if (moveEarlier) currentIndex - 1 else currentIndex + 1
        if (targetIndex !in notePhotos.indices) return false

        val reordered = notePhotos.toMutableList()
        val item = reordered.removeAt(currentIndex)
        reordered.add(targetIndex, item)

        val baseTime = reordered.minOfOrNull { it.capturedAtMillis } ?: System.currentTimeMillis()
        val pageMap = reordered.mapIndexed { idx, p ->
            p.id to Pair(idx + 1, baseTime + idx * 10L)
        }.toMap()

        val updatedList = _photosFlow.value.map { photo ->
            val mapped = pageMap[photo.id]
            if (mapped != null) {
                photo.copy(
                    pageNumber = mapped.first,
                    capturedAtMillis = mapped.second,
                    editRevision = photo.editRevision + 1
                )
            } else {
                photo
            }
        }
        savePhotosToPrefs(updatedList)
        _photosFlow.value = updatedList
        return true
    }

    fun updatePhotoOcrText(photoId: String, newOcrText: String): CapturedDocumentPhoto? {
        if (photoId.isBlank()) return null
        val cleaned = newOcrText.trim()
        var updatedTarget: CapturedDocumentPhoto? = null
        val updated = _photosFlow.value.map { photo ->
            if (photo.id == photoId) {
                photo.copy(
                    ocrText = cleaned,
                    isOcrProcessed = true,
                    editRevision = photo.editRevision + 1
                ).also { updatedTarget = it }
            } else {
                photo
            }
        }
        if (updatedTarget != null) {
            savePhotosToPrefs(updated)
            _photosFlow.value = updated
        }
        return updatedTarget
    }

    fun discardPhotos(photoIds: Set<String>): Int {
        if (photoIds.isEmpty()) return 0
        val toRemove = _photosFlow.value.filter { photoIds.contains(it.id) }
        toRemove.forEach { photo ->
            runCatching {
                val f = File(photo.filePath)
                if (f.exists()) f.delete()
                val masterFile = File(vaultDir, "${photo.id}_master.jpg")
                if (masterFile.exists()) masterFile.delete()
            }
        }
        val remaining = _photosFlow.value.filterNot { photoIds.contains(it.id) }
        savePhotosToPrefs(remaining)
        _photosFlow.value = remaining
        QuickCameraWidgetProvider.syncCameraWidget(appContext, remaining.size)
        return toRemove.size
    }

    fun discardPhoto(photoId: String): Int {
        if (photoId.isBlank()) return 0
        return discardPhotos(setOf(photoId))
    }

    fun deleteNotePhotos(noteId: String): Int {
        if (noteId.isBlank()) return 0
        val ids = _photosFlow.value.filter { it.noteId == noteId }.map { it.id }.toSet()
        return discardPhotos(ids)
    }

    fun assignPhotosToNote(photoIds: List<String>, noteId: String) {
        if (photoIds.isEmpty() || noteId.isBlank()) return
        val idSet = photoIds.toSet()
        val existingInNote = _photosFlow.value.filter { it.noteId == noteId && !idSet.contains(it.id) }
        var nextPage = (existingInNote.maxOfOrNull { it.pageNumber } ?: existingInNote.size) + 1
        val orderedNewIds = photoIds.distinct()
        val pageByPhotoId = orderedNewIds.associateWith { nextPage++ }

        val updated = _photosFlow.value.map { photo ->
            if (idSet.contains(photo.id)) {
                photo.copy(
                    noteId = noteId,
                    pageNumber = pageByPhotoId[photo.id] ?: photo.pageNumber
                )
            } else {
                photo
            }
        }
        savePhotosToPrefs(updated)
        _photosFlow.value = updated
    }

    fun appendPhotosToNote(photoIds: List<String>, noteId: String) {
        assignPhotosToNote(photoIds, noteId)
    }

    fun linkPhotosToNote(photoIds: List<String>, noteId: String) {
        assignPhotosToNote(photoIds, noteId)
    }

    fun relinkPhotosFromOldNoteToNewNote(oldNoteId: String, newNoteId: String) {
        if (oldNoteId.isBlank() || newNoteId.isBlank() || oldNoteId == newNoteId) return
        var changed = false
        val updated = _photosFlow.value.map { photo ->
            if (photo.noteId == oldNoteId) {
                changed = true
                photo.copy(noteId = newNoteId)
            } else {
                photo
            }
        }
        if (changed) {
            savePhotosToPrefs(updated)
            _photosFlow.value = updated
        }
    }

    fun getPhotosForNote(noteId: String, noteContent: String = ""): List<CapturedDocumentPhoto> {
        val extractedIds = extractPhotoIdsFromContent(noteContent)
        return _photosFlow.value.filter { photo ->
            photo.fileExists && (
                (noteId.isNotBlank() && photo.noteId == noteId) ||
                    extractedIds.contains(photo.id)
                )
        }.sortedWith(compareBy<CapturedDocumentPhoto> { it.pageNumber }.thenBy { it.capturedAtMillis })
    }

    fun getCombinedOcrText(photos: List<CapturedDocumentPhoto>): String {
        val valid = photos.filter { it.ocrText.isNotBlank() }
            .sortedWith(compareBy<CapturedDocumentPhoto> { it.pageNumber }.thenBy { it.capturedAtMillis })
        if (valid.isEmpty()) return ""
        if (valid.size == 1) {
            return valid.first().ocrText.trim()
        }
        return valid.mapIndexed { idx, photo ->
            "--- Page #${idx + 1} (${photo.title}) ---\n${photo.ocrText.trim()}"
        }.joinToString("\n\n")
    }

    fun getCombinedOcrTextForNote(noteId: String, noteContent: String = ""): String {
        return getCombinedOcrText(getPhotosForNote(noteId, noteContent))
    }

    fun matchesOcrSearch(photos: List<CapturedDocumentPhoto>, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty() || photos.isEmpty()) return false
        return photos.any { photo ->
            photo.ocrText.lowercase().contains(q)
        }
    }

    fun findOcrSearchSnippet(photos: List<CapturedDocumentPhoto>, query: String): String? {
        val q = query.trim().lowercase()
        if (q.isEmpty() || photos.isEmpty()) return null
        for ((idx, photo) in photos.withIndex()) {
            val raw = photo.ocrText.trim()
            if (raw.isEmpty()) continue
            val matchLine = raw.lines().firstOrNull { it.lowercase().contains(q) }
            if (matchLine != null) {
                val pagePrefix = if (photos.size > 1) "Page #${idx + 1}: " else ""
                return "$pagePrefix${matchLine.trim().take(90)}"
            }
        }
        return null
    }

    fun writePdfToStream(photos: List<CapturedDocumentPhoto>, outputStream: OutputStream): Result<Int> {
        val validPhotos = photos.filter { it.fileExists }
        if (validPhotos.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid document photos to export."))
        }
        return runCatching {
            val pdfDocument = PdfDocument()
            try {
                val pageWidth = 1240
                val pageHeight = 1754
                val margin = 48f

                validPhotos.forEachIndexed { index, photo ->
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val bitmap = BitmapFactory.decodeFile(photo.filePath)
                    if (bitmap != null) {
                        val availW = pageWidth - margin * 2
                        val availH = pageHeight - margin * 2 - 40f
                        val scale = minOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
                        val drawW = bitmap.width * scale
                        val drawH = bitmap.height * scale
                        val left = (pageWidth - drawW) / 2f
                        val top = margin + (availH - drawH) / 2f
                        val destRect = RectF(left, top, left + drawW, top + drawH)
                        canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    }

                    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.DKGRAY
                        textSize = 22f
                    }
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(photo.capturedAtMillis))
                    canvas.drawText(
                        "Notii Private Document Scan • Page ${index + 1} of ${validPhotos.size} • $dateStr",
                        margin,
                        pageHeight - 28f,
                        footerPaint
                    )
                    pdfDocument.finishPage(page)
                }
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
                validPhotos.size
            } finally {
                pdfDocument.close()
            }
        }
    }

    fun writeZipToStream(photos: List<CapturedDocumentPhoto>, outputStream: OutputStream): Result<Int> {
        val validPhotos = photos.filter { it.fileExists }
        if (validPhotos.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid document photos to export."))
        }
        return runCatching {
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                validPhotos.forEachIndexed { idx, photo ->
                    val file = File(photo.filePath)
                    val entryName = String.format(Locale.US, "Notii_Doc_%02d_%s.jpg", idx + 1, photo.id.takeLast(6))
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            validPhotos.size
        }
    }

    fun exportAndSharePdf(context: Context, photos: List<CapturedDocumentPhoto>, titlePrefix: String = "Notii_Document_Scan"): Result<File> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val cleanPrefix = titlePrefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(32).ifBlank { "Notii_Scan" }
            val pdfFile = File(exportDir, "${cleanPrefix}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { fos ->
                writePdfToStream(photos, fos).getOrThrow()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Notii Document Scan (${photos.size} page(s))")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Export Document PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            pdfFile
        }
    }

    fun exportAndShareImages(context: Context, photos: List<CapturedDocumentPhoto>): Result<Int> {
        val validPhotos = photos.filter { it.fileExists }
        if (validPhotos.isEmpty()) {
            return Result.failure(IllegalArgumentException("No photos available to export."))
        }
        return runCatching {
            val uris = ArrayList(
                validPhotos.map { photo ->
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(photo.filePath)
                    )
                }
            )
            val shareIntent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/jpeg"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            val chooser = Intent.createChooser(shareIntent, "Export ${validPhotos.size} Document Image(s)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            validPhotos.size
        }
    }

    private fun loadPhotosFromPrefs(): List<CapturedDocumentPhoto> {
        val raw = prefs.getString(KEY_PHOTOS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CapturedDocumentPhoto>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.optString("filePath", "")
                if (path.isNotBlank() && File(path).exists()) {
                    val pageNum = obj.optInt("pageNumber", 1)
                    val modeName = obj.optString(
                        "enhancementMode",
                        DocumentEnhancementMode.HIGH_CONTRAST_DOC.name
                    )
                    val parsedMode = runCatching { DocumentEnhancementMode.valueOf(modeName) }
                        .getOrDefault(DocumentEnhancementMode.HIGH_CONTRAST_DOC)
                    val w = obj.optInt("width", 0)
                    val h = obj.optInt("height", 0)
                    val storedOcr = obj.optString("ocrText", "").trim().ifEmpty {
                        DocumentOcrExtractor.buildDefaultDocumentOcrText(
                            shotNumber = pageNum,
                            mode = parsedMode,
                            width = if (w > 0) w else 900,
                            height = if (h > 0) h else 1240
                        )
                    }
                    list.add(
                        CapturedDocumentPhoto(
                            id = obj.optString("id"),
                            filePath = path,
                            sessionId = obj.optString("sessionId", ""),
                            noteId = obj.optString("noteId", ""),
                            pageNumber = pageNum,
                            capturedAtMillis = obj.optLong("capturedAtMillis", System.currentTimeMillis()),
                            enhancementMode = modeName,
                            width = w,
                            height = h,
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            ocrText = storedOcr,
                            isOcrProcessed = obj.optBoolean("isOcrProcessed", true),
                            customCaption = obj.optString("customCaption", ""),
                            rotationAppliedDegrees = obj.optInt("rotationAppliedDegrees", 0),
                            cropPreset = obj.optString("cropPreset", DocumentCropPreset.FULL_FRAME.name),
                            brightnessOffset = obj.optInt("brightnessOffset", 0),
                            editRevision = obj.optInt("editRevision", 0)
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePhotosToPrefs(photos: List<CapturedDocumentPhoto>) {
        val arr = JSONArray()
        photos.forEach { photo ->
            arr.put(
                JSONObject().apply {
                    put("id", photo.id)
                    put("filePath", photo.filePath)
                    put("sessionId", photo.sessionId)
                    put("noteId", photo.noteId)
                    put("pageNumber", photo.pageNumber)
                    put("capturedAtMillis", photo.capturedAtMillis)
                    put("enhancementMode", photo.enhancementMode)
                    put("width", photo.width)
                    put("height", photo.height)
                    put("sizeBytes", photo.sizeBytes)
                    put("ocrText", photo.ocrText)
                    put("isOcrProcessed", photo.isOcrProcessed)
                    put("customCaption", photo.customCaption)
                    put("rotationAppliedDegrees", photo.rotationAppliedDegrees)
                    put("cropPreset", photo.cropPreset)
                    put("brightnessOffset", photo.brightnessOffset)
                    put("editRevision", photo.editRevision)
                }
            )
        }
        prefs.edit().putString(KEY_PHOTOS_JSON, arr.toString()).apply()
    }

    companion object {
        const val PREFS_NAME = "notii_private_doc_vault_prefs"
        const val KEY_PHOTOS_JSON = "private_doc_photos_json"
        const val PRIVATE_DIR_NAME = "private_doc_captures"
        const val MAX_DOCUMENT_DIMENSION = 1600

        @Volatile
        private var instance: PrivateDocumentVaultStore? = null

        fun getInstance(context: Context): PrivateDocumentVaultStore {
            return instance ?: synchronized(this) {
                instance ?: PrivateDocumentVaultStore(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private val DOC_SCAN_MARKER_REGEX = Regex("\\[DocScan:([a-zA-Z0-9_\\-]+)]")
        private val DOC_SCAN_STRIP_REGEX = Regex("\\[DocScan:[a-zA-Z0-9_\\-]+]\\n?")

        private fun calculateImportSampleSize(width: Int, height: Int, maxDim: Int): Int {
            var inSampleSize = 1
            if (height > maxDim || width > maxDim) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize.coerceAtLeast(1)
        }

        fun formatPhotoMarker(photoId: String): String = "[DocScan:$photoId]"

        fun extractPhotoIdsFromContent(content: String): Set<String> {
            if (!content.contains("[DocScan:")) return emptySet()
            return DOC_SCAN_MARKER_REGEX.findAll(content).map { it.groupValues[1] }.toSet()
        }

        fun stripPhotoMarkersForDisplay(content: String): String {
            if (!content.contains("[DocScan:")) return content.trim()
            return content
                .replace(DOC_SCAN_STRIP_REGEX, "")
                .trim()
        }

        fun renderEditedBitmap(
            source: Bitmap,
            mode: DocumentEnhancementMode,
            rotationDegrees: Int = 0,
            cropPreset: DocumentCropPreset = DocumentCropPreset.FULL_FRAME,
            brightnessOffset: Int = 0
        ): Bitmap {
            val normalizedRot = ((rotationDegrees % 360) + 360) % 360
            val rotated = if (normalizedRot != 0) {
                val matrix = Matrix().apply { postRotate(normalizedRot.toFloat()) }
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            } else {
                source
            }

            val cropped = if (cropPreset.marginFraction > 0f && rotated.width > 16 && rotated.height > 16) {
                val padX = (rotated.width * cropPreset.marginFraction).toInt().coerceAtLeast(1)
                val padY = (rotated.height * cropPreset.marginFraction).toInt().coerceAtLeast(1)
                val cropW = (rotated.width - padX * 2).coerceAtLeast(8)
                val cropH = (rotated.height - padY * 2).coerceAtLeast(8)
                Bitmap.createBitmap(rotated, padX, padY, cropW, cropH)
            } else {
                rotated
            }

            return optimizeBitmapForDocument(
                source = cropped,
                mode = mode,
                brightnessOffset = brightnessOffset
            )
        }

        fun optimizeBitmapForDocument(
            source: Bitmap,
            mode: DocumentEnhancementMode,
            brightnessOffset: Int = 0
        ): Bitmap {
            val maxDim = 1800
            val scale = if (source.width > maxDim || source.height > maxDim) {
                minOf(maxDim.toFloat() / source.width, maxDim.toFloat() / source.height)
            } else {
                1f
            }
            val targetW = (source.width * scale).toInt().coerceAtLeast(1)
            val targetH = (source.height * scale).toInt().coerceAtLeast(1)

            val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val extraBrightness = brightnessOffset.coerceIn(-60, 60).toFloat() * 1.4f

            when (mode) {
                DocumentEnhancementMode.HIGH_CONTRAST_DOC -> {
                    // Increase contrast (1.42x) and slight brightness lift for crisp paper/text separation
                    val contrast = 1.42f
                    val translate = (-0.5f * contrast + 0.5f) * 255f + 14f + extraBrightness
                    val cm = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, translate,
                            0f, contrast, 0f, 0f, translate,
                            0f, 0f, contrast, 0f, translate,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    paint.colorFilter = ColorMatrixColorFilter(cm)
                }

                DocumentEnhancementMode.BW_SCAN -> {
                    val saturation = ColorMatrix().apply { setSaturation(0f) }
                    val contrast = 1.75f
                    val translate = (-0.5f * contrast + 0.5f) * 255f + 22f + extraBrightness
                    val highContrast = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, translate,
                            0f, contrast, 0f, 0f, translate,
                            0f, 0f, contrast, 0f, translate,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    saturation.postConcat(highContrast)
                    paint.colorFilter = ColorMatrixColorFilter(saturation)
                }

                DocumentEnhancementMode.ORIGINAL_COLOR -> {
                    val contrast = 1.12f
                    val translate = (-0.5f * contrast + 0.5f) * 255f + 4f + extraBrightness
                    val cm = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, translate,
                            0f, contrast, 0f, 0f, translate,
                            0f, 0f, contrast, 0f, translate,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    paint.colorFilter = ColorMatrixColorFilter(cm)
                }
            }

            canvas.drawBitmap(
                source,
                Rect(0, 0, source.width, source.height),
                Rect(0, 0, targetW, targetH),
                paint
            )
            return output
        }

        fun createDocumentScanFallbackBitmap(
            shotNumber: Int,
            mode: DocumentEnhancementMode
        ): Bitmap {
            val width = 900
            val height = 1240
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.parseColor("#F8FAFC"))

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRect(36f, 36f, width - 36f, height - 36f, borderPaint)

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val timestamp = SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("DOCUMENT SCAN • PAGE #$shotNumber", 76f, 120f, headerPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 24f
            }
            canvas.drawText("Captured: $timestamp", 76f, 168f, subPaint)
            canvas.drawText("Mode: ${mode.label} • Private App Vault", 76f, 206f, subPaint)

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                strokeWidth = 3f
            }
            canvas.drawLine(76f, 240f, width - 76f, 240f, linePaint)

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 24f
            }
            val sampleLines = listOf(
                "1. Document Capture Optimization: High-Contrast Text & Edge Clarity",
                "2. Storage Security: Saved in internal filesDir/private_doc_captures",
                "3. Gallery Privacy: Hidden from Android MediaStore & System Gallery",
                "4. Multi-Shot Burst: Shot $shotNumber of up to 10 consecutive captures",
                "5. Export Ready: Multi-Page PDF, ZIP Archive, or Secure FileProvider Share"
            )
            var y = 310f
            sampleLines.forEach { line ->
                canvas.drawText(line, 76f, y, bodyPaint)
                y += 58f
            }

            // Simulated document paragraph blocks
            val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
            }
            for (row in 0 until 8) {
                val top = y + 30f + row * 48f
                val right = if (row % 3 == 2) width - 220f else width - 96f
                canvas.drawRoundRect(RectF(76f, top, right, top + 18f), 6f, 6f, blockPaint)
            }

            return optimizeBitmapForDocument(bmp, mode)
        }
    }
}
