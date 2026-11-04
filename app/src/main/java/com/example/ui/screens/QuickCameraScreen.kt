package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.CapturedDocumentPhoto
import com.example.data.local.DocumentCropPreset
import com.example.data.local.DocumentEnhancementMode
import com.example.data.local.PrivateDocumentVaultStore
import com.example.ui.theme.ProvideSafeTextToolbar
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MAX_CONSECUTIVE_SHOTS = 10

@Composable
fun QuickDocumentCameraScreen(
    vaultStore: PrivateDocumentVaultStore,
    isAppendingToNote: Boolean = false,
    onCompleteWithKeptPhotos: (
        keptPhotos: List<CapturedDocumentPhoto>,
        manualTitle: String,
        manualContent: String
    ) -> Unit,
    onCancelOrClose: () -> Unit,
    onStatusMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val cleanupReviewTextFocus = {
        runCatching { textToolbar.hide() }
        runCatching { focusManager.clearFocus(force = true) }
        runCatching { keyboardController?.hide() }
    }

    val sessionId = remember { "cam_session_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    val capturedPhotos = remember { mutableStateListOf<CapturedDocumentPhoto>() }
    var selectedForDiscardIds by remember { mutableStateOf(emptySet<String>()) }
    var isReviewStage by remember { mutableStateOf(false) }
    var manualTitle by remember { mutableStateOf("") }
    var manualNoteText by remember { mutableStateOf("") }
    var enhancementMode by remember { mutableStateOf(DocumentEnhancementMode.HIGH_CONTRAST_DOC) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isCapturingShot by remember { mutableStateOf(false) }
    var statusHint by remember {
        mutableStateOf("Align document inside the frame • Up to 10 consecutive shots (saved automatically)")
    }
    var inspectedPhoto by remember { mutableStateOf<CapturedDocumentPhoto?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var isCameraBound by remember { mutableStateOf(false) }

    DisposableEffect(hasCameraPermission, lifecycleOwner) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    val selector = when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                            CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> null
                    }

                    if (selector != null) {
                        cameraProvider.unbindAll()
                        val cam = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            capture
                        )
                        boundCamera = cam
                        imageCapture = capture
                        isCameraBound = true

                        // Document auto-focus optimization on center of document frame
                        previewView.post {
                            runCatching {
                                val factory = previewView.meteringPointFactory
                                val centerPoint = factory.createPoint(
                                    previewView.width / 2f,
                                    previewView.height / 2f
                                )
                                val action = FocusMeteringAction.Builder(
                                    centerPoint,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                            }
                        }
                    } else {
                        isCameraBound = false
                    }
                }.onFailure {
                    isCameraBound = false
                }
            }, executor)
        }
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    val saveShotAndAdvance: (Bitmap, Int) -> Unit = { rawBitmap, rotation ->
        coroutineScope.launch {
            val nextShotNum = capturedPhotos.size + 1
            val savedPhoto = withContext(Dispatchers.IO) {
                vaultStore.saveCapturedDocumentBitmapWithOcr(
                    rawBitmap = rawBitmap,
                    mode = enhancementMode,
                    shotNumber = nextShotNum,
                    sessionId = sessionId,
                    rotationDegrees = rotation
                )
            }
            capturedPhotos.add(savedPhoto)
            isCapturingShot = false
            if (capturedPhotos.size >= MAX_CONSECUTIVE_SHOTS) {
                statusHint = "Reached 10 / 10 consecutive shots! OCR indexed • Reviewing captured pictures..."
                isReviewStage = true
            } else {
                statusHint = "Saved shot #${capturedPhotos.size} privately + OCR indexed (${capturedPhotos.size}/$MAX_CONSECUTIVE_SHOTS) • Tap shutter for next shot or Done"
            }
        }
    }

    val triggerCaptureShot = {
        if (!isCapturingShot && capturedPhotos.size < MAX_CONSECUTIVE_SHOTS) {
            isCapturingShot = true
            val nextShotNum = capturedPhotos.size + 1
            val activeCapture = imageCapture
            val previewBmp = runCatching { previewView.bitmap }.getOrNull()

            if (isCameraBound && activeCapture != null) {
                activeCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val rotation = image.imageInfo.rotationDegrees
                            val decoded = decodeBitmapFromImageProxy(image)
                            image.close()
                            val finalBmp = decoded
                                ?: previewBmp
                                ?: PrivateDocumentVaultStore.createDocumentScanFallbackBitmap(
                                    shotNumber = nextShotNum,
                                    mode = enhancementMode
                                )
                            saveShotAndAdvance(finalBmp, if (decoded != null) rotation else 0)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            val fallback = previewBmp
                                ?: PrivateDocumentVaultStore.createDocumentScanFallbackBitmap(
                                    shotNumber = nextShotNum,
                                    mode = enhancementMode
                                )
                            saveShotAndAdvance(fallback, 0)
                        }
                    }
                )
            } else {
                val fallback = previewBmp
                    ?: PrivateDocumentVaultStore.createDocumentScanFallbackBitmap(
                        shotNumber = nextShotNum,
                        mode = enhancementMode
                    )
                saveShotAndAdvance(fallback, 0)
            }
        }
    }

    BackHandler {
        if (isReviewStage) {
            cleanupReviewTextFocus()
            if (capturedPhotos.isNotEmpty()) {
                onCompleteWithKeptPhotos(
                    capturedPhotos.toList(),
                    manualTitle.trim(),
                    manualNoteText.trim()
                )
            } else {
                onCancelOrClose()
            }
        } else if (capturedPhotos.isNotEmpty()) {
            isReviewStage = true
        } else {
            onCancelOrClose()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("quick_camera_screen")
    ) { innerPadding ->
        if (!isReviewStage) {
            // PHASE 1: Document-Optimized Consecutive Camera Viewfinder (Up to 10 Shots)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0B1120))
            ) {
                // Top Camera Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (capturedPhotos.isNotEmpty()) {
                                    isReviewStage = true
                                } else {
                                    onCancelOrClose()
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .testTag("camera_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close camera",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "Quick Document Camera",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Hidden from Android Gallery • App-Only Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Torch / Flash button for document lighting
                        IconButton(
                            onClick = {
                                val nextTorch = !isTorchEnabled
                                isTorchEnabled = nextTorch
                                runCatching {
                                    boundCamera?.cameraControl?.enableTorch(nextTorch)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTorchEnabled) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.12f)
                                )
                                .testTag("camera_torch_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle document torch",
                                tint = if (isTorchEnabled) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Consecutive Shot Counter Badge (0/10 .. 10/10)
                        Surface(
                            color = Color(0xFFF59E0B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("camera_shot_counter_badge")
                        ) {
                            Text(
                                text = "${capturedPhotos.size} / $MAX_CONSECUTIVE_SHOTS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1300),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Document Enhancement Mode Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentEnhancementMode.entries.forEach { mode ->
                        val selected = enhancementMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { enhancementMode = mode },
                            label = {
                                Text(
                                    text = mode.shortLabel,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                                )
                            },
                            modifier = Modifier.testTag("doc_mode_${mode.name}")
                        )
                    }
                }

                // Viewfinder with Document Framing Guide Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    // Re-trigger center document auto-focus on tap
                                    runCatching {
                                        val factory = previewView.meteringPointFactory
                                        val pt = factory.createPoint(
                                            previewView.width / 2f,
                                            previewView.height / 2f
                                        )
                                        boundCamera?.cameraControl?.startFocusAndMetering(
                                            FocusMeteringAction.Builder(pt).build()
                                        )
                                    }
                                }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Document Scanner Ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Grant camera permission for live viewfinder or tap the Shutter below to capture high-contrast document scans.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("grant_camera_permission_button")
                            ) {
                                Text("Grant Camera Permission", color = Color(0xFFFBBF24))
                            }
                        }
                    }

                    // Document Framing Guide Corner Brackets & A4 Alignment Reticle
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val padX = size.width * 0.09f
                        val padY = size.height * 0.08f
                        val left = padX
                        val top = padY
                        val right = size.width - padX
                        val bottom = size.height - padY
                        val bracketLen = minOf(size.width, size.height) * 0.09f
                        val stroke = 3.5.dp.toPx()
                        val guideColor = Color(0xFFFBBF24)

                        // Subtle inner frame
                        drawRect(
                            color = Color.White.copy(alpha = 0.22f),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Top-left corner
                        drawLine(guideColor, Offset(left, top), Offset(left + bracketLen, top), stroke)
                        drawLine(guideColor, Offset(left, top), Offset(left, top + bracketLen), stroke)

                        // Top-right corner
                        drawLine(guideColor, Offset(right - bracketLen, top), Offset(right, top), stroke)
                        drawLine(guideColor, Offset(right, top), Offset(right, top + bracketLen), stroke)

                        // Bottom-left corner
                        drawLine(guideColor, Offset(left, bottom - bracketLen), Offset(left, bottom), stroke)
                        drawLine(guideColor, Offset(left, bottom), Offset(left + bracketLen, bottom), stroke)

                        // Bottom-right corner
                        drawLine(guideColor, Offset(right - bracketLen, bottom), Offset(right, bottom), stroke)
                        drawLine(guideColor, Offset(right, bottom - bracketLen), Offset(right, bottom), stroke)
                    }

                    // Bottom status pill inside viewfinder
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = statusHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Live Consecutive Thumbnail Strip (when 1..10 shots have been taken)
                if (capturedPhotos.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState())
                            .testTag("camera_live_thumbnails_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        capturedPhotos.forEachIndexed { idx, photo ->
                            DocumentPhotoThumbnail(
                                photo = photo,
                                badgeLabel = "#${idx + 1}",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                                    .clickable { isReviewStage = true }
                            )
                        }
                    }
                }

                // Bottom Camera Shutter & Done Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (capturedPhotos.isNotEmpty()) {
                                isReviewStage = true
                            } else {
                                onCancelOrClose()
                            }
                        },
                        modifier = Modifier.testTag("camera_cancel_or_review_button")
                    ) {
                        Text(
                            text = if (capturedPhotos.isEmpty()) "Cancel" else "Review (${capturedPhotos.size})",
                            color = Color.White
                        )
                    }

                    // Main Consecutive Shutter Button (Up to 10 shots)
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color(0xFFF59E0B), CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (capturedPhotos.size < MAX_CONSECUTIVE_SHOTS && !isCapturingShot) {
                                    Color.White
                                } else {
                                    Color.Gray
                                }
                            )
                            .clickable(
                                enabled = capturedPhotos.size < MAX_CONSECUTIVE_SHOTS && !isCapturingShot
                            ) {
                                triggerCaptureShot()
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take document shot",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "${capturedPhotos.size}/$MAX_CONSECUTIVE_SHOTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Done Button -> Transitions to Review & Discard screen
                    Button(
                        onClick = { isReviewStage = true },
                        enabled = capturedPhotos.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF1E1300)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("camera_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Done (${capturedPhotos.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // PHASE 2: Post-Capture Review, Discard Selected, & Export Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("camera_review_screen")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (capturedPhotos.size < MAX_CONSECUTIVE_SHOTS) {
                            IconButton(
                                onClick = { isReviewStage = false },
                                modifier = Modifier.testTag("back_to_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Take more shots"
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Captured Pictures (${capturedPhotos.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Saved privately in Notii • Tap picture(s) you want to discard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (capturedPhotos.isNotEmpty()) {
                        val allMarkedForDiscard = capturedPhotos.all { selectedForDiscardIds.contains(it.id) }
                        TextButton(
                            onClick = {
                                selectedForDiscardIds = if (allMarkedForDiscard) {
                                    emptySet()
                                } else {
                                    capturedPhotos.map { it.id }.toSet()
                                }
                            },
                            modifier = Modifier.testTag("review_select_all_discard_button")
                        ) {
                            Text(if (allMarkedForDiscard) "Keep All" else "Select All")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info Banner about Gallery Privacy & Discard Selection
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (selectedForDiscardIds.isEmpty()) {
                                    "All ${capturedPhotos.size} shot(s) are saved in hidden app storage. Tap any shot to mark for discard."
                                } else {
                                    "${selectedForDiscardIds.size} shot(s) selected to discard • ${capturedPhotos.size - selectedForDiscardIds.size} will be kept."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (capturedPhotos.size < MAX_CONSECUTIVE_SHOTS) {
                            AssistChip(
                                onClick = { isReviewStage = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = { Text("+ Shoot (${MAX_CONSECUTIVE_SHOTS - capturedPhotos.size} left)") },
                                modifier = Modifier.testTag("review_shoot_more_chip")
                            )
                        }
                    }

                    // Hidden-by-default On-Device OCR Preview Toggle in Review Header
                    var showReviewOcrText by remember { mutableStateOf(false) }
                    val keptForOcr = capturedPhotos.filterNot { selectedForDiscardIds.contains(it.id) }
                    val combinedReviewOcr = remember(keptForOcr) {
                        vaultStore.getCombinedOcrText(keptForOcr)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "On-device OCR extracted automatically (hidden by default • searchable)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showReviewOcrText = !showReviewOcrText },
                            modifier = Modifier.testTag("review_toggle_ocr_button")
                        ) {
                            Icon(
                                imageVector = if (showReviewOcrText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showReviewOcrText) "Hide OCR Text" else "Show OCR Text",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (showReviewOcrText) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("review_ocr_text_box")
                        ) {
                            Text(
                                text = combinedReviewOcr.ifBlank { "No OCR text extracted for selected shots." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (capturedPhotos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "All captured pictures were discarded.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { isReviewStage = false },
                                    modifier = Modifier.testTag("retake_after_discard_all_button")
                                ) {
                                    Text("Take New Shots")
                                }
                                Button(
                                    onClick = onCancelOrClose,
                                    modifier = Modifier.testTag("close_after_discard_all_button")
                                ) {
                                    Text("Return to Notes")
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("camera_review_grid")
                    ) {
                        itemsIndexed(capturedPhotos, key = { _, item -> item.id }) { index, photo ->
                            val isMarkedDiscard = selectedForDiscardIds.contains(photo.id)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMarkedDiscard) {
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isMarkedDiscard) 2.5.dp else 1.dp,
                                        color = if (isMarkedDiscard) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        selectedForDiscardIds = if (isMarkedDiscard) {
                                            selectedForDiscardIds - photo.id
                                        } else {
                                            selectedForDiscardIds + photo.id
                                        }
                                    }
                                    .testTag("review_photo_card_${photo.id}")
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        DocumentPhotoThumbnail(
                                            photo = photo,
                                            badgeLabel = "Shot #${index + 1}",
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Zoom / Inspect button
                                        IconButton(
                                            onClick = { inspectedPhoto = photo },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .testTag("inspect_photo_${photo.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription = "Inspect document scan",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Discard / Keep selection status pill
                                        Surface(
                                            color = if (isMarkedDiscard) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                Color(0xFF0F172A).copy(alpha = 0.78f)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isMarkedDiscard) {
                                                        Icons.Default.DeleteOutline
                                                    } else {
                                                        Icons.Default.CheckCircle
                                                    },
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (isMarkedDiscard) "Selected to Discard" else "Saved (Tap to Discard)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Manual Note Text Input Section (Empty by default — No automatic filled text)
                    if (!isAppendingToNote) {
                        ProvideSafeTextToolbar {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("camera_manual_text_section")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Add Note Text Manually (Optional)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    OutlinedTextField(
                                        value = manualTitle,
                                        onValueChange = { manualTitle = it },
                                        placeholder = {
                                            Text(
                                                text = "Add title manually (optional)...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("camera_manual_title_input")
                                    )

                                    OutlinedTextField(
                                        value = manualNoteText,
                                        onValueChange = { manualNoteText = it },
                                        placeholder = {
                                            Text(
                                                text = "Add note text manually after taking pictures (optional)...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        minLines = 2,
                                        maxLines = 4,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("camera_manual_note_input")
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Action Bar for Review Screen: Discard Selected | Export | Save & Keep
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val idsToDiscard = selectedForDiscardIds
                                    if (idsToDiscard.isNotEmpty()) {
                                        val removedCount = vaultStore.discardPhotos(idsToDiscard)
                                        capturedPhotos.removeAll { idsToDiscard.contains(it.id) }
                                        selectedForDiscardIds = emptySet()
                                        onStatusMessage("Discarded $removedCount picture(s).")
                                    }
                                },
                                enabled = selectedForDiscardIds.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("discard_selected_photos_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = if (selectedForDiscardIds.isNotEmpty()) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Discard (${selectedForDiscardIds.size})",
                                    color = if (selectedForDiscardIds.isNotEmpty()) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            OutlinedButton(
                                onClick = { showExportDialog = true },
                                enabled = capturedPhotos.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("camera_review_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export")
                            }
                        }

                        Button(
                            onClick = {
                                cleanupReviewTextFocus()
                                // If any pictures are still marked to discard when user taps Finish, discard them first
                                if (selectedForDiscardIds.isNotEmpty()) {
                                    vaultStore.discardPhotos(selectedForDiscardIds)
                                    capturedPhotos.removeAll { selectedForDiscardIds.contains(it.id) }
                                    selectedForDiscardIds = emptySet()
                                }
                                if (capturedPhotos.isNotEmpty()) {
                                    onCompleteWithKeptPhotos(
                                        capturedPhotos.toList(),
                                        manualTitle.trim(),
                                        manualNoteText.trim()
                                    )
                                } else {
                                    onCancelOrClose()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_camera_session_button")
                        ) {
                            val keptCount = (capturedPhotos.size - selectedForDiscardIds.size).coerceAtLeast(0)
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    selectedForDiscardIds.isNotEmpty() && isAppendingToNote ->
                                        "Discard ${selectedForDiscardIds.size} & Add $keptCount Picture(s) to Note"
                                    isAppendingToNote ->
                                        "Add ${capturedPhotos.size} Picture(s) to Note"
                                    selectedForDiscardIds.isNotEmpty() ->
                                        "Discard ${selectedForDiscardIds.size} & Keep $keptCount Picture(s)"
                                    else ->
                                        "Keep All ${capturedPhotos.size} Picture(s) in App"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Document Scan Inspection & Editing Dialog
    val targetInspect = inspectedPhoto
    if (targetInspect != null) {
        EditDocumentPhotoDialog(
            vaultStore = vaultStore,
            photo = targetInspect,
            noteId = targetInspect.noteId,
            canMoveLeft = false,
            canMoveRight = false,
            onPhotoUpdated = { updated ->
                val idx = capturedPhotos.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    capturedPhotos[idx] = updated
                }
                inspectedPhoto = updated
            },
            onPhotoDiscarded = { discardedId ->
                capturedPhotos.removeAll { it.id == discardedId }
                selectedForDiscardIds = selectedForDiscardIds - discardedId
                inspectedPhoto = null
                onStatusMessage("Discarded picture.")
            },
            onDismiss = { inspectedPhoto = null },
            onStatusMessage = onStatusMessage
        )
    }

    if (showExportDialog) {
        val photosToExport = capturedPhotos.filterNot { selectedForDiscardIds.contains(it.id) }
            .ifEmpty { capturedPhotos.toList() }
        ExportDocumentPhotosDialog(
            vaultStore = vaultStore,
            photos = photosToExport,
            defaultTitle = "Notii_Document_Scan",
            onDismiss = { showExportDialog = false },
            onStatusMessage = onStatusMessage
        )
    }
}

@Composable
fun EditDocumentPhotoDialog(
    vaultStore: PrivateDocumentVaultStore,
    photo: CapturedDocumentPhoto,
    noteId: String = photo.noteId,
    canMoveLeft: Boolean = false,
    canMoveRight: Boolean = false,
    onMovePhoto: ((moveLeft: Boolean) -> Unit)? = null,
    onInsertOcrIntoNote: ((String) -> Unit)? = null,
    onPhotoUpdated: (CapturedDocumentPhoto) -> Unit = {},
    onPhotoDiscarded: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val cleanupFocus = {
        runCatching { textToolbar.hide() }
        runCatching { focusManager.clearFocus(force = true) }
        runCatching { keyboardController?.hide() }
    }

    var currentPhoto by remember(photo.id, photo.editRevision) { mutableStateOf(photo) }
    var captionInput by remember(photo.id) { mutableStateOf(photo.customCaption) }
    var ocrInput by remember(photo.id, photo.ocrText) { mutableStateOf(photo.ocrText) }
    var showOcrEditor by remember(photo.id) { mutableStateOf(false) }

    val selectedMode = remember(currentPhoto.enhancementMode) {
        runCatching { DocumentEnhancementMode.valueOf(currentPhoto.enhancementMode) }
            .getOrDefault(DocumentEnhancementMode.HIGH_CONTRAST_DOC)
    }
    val selectedCrop = remember(currentPhoto.cropPreset) {
        runCatching { DocumentCropPreset.valueOf(currentPhoto.cropPreset) }
            .getOrDefault(DocumentCropPreset.FULL_FRAME)
    }
    var brightnessOffset by remember(currentPhoto.id, currentPhoto.brightnessOffset) {
        mutableIntStateOf(currentPhoto.brightnessOffset)
    }

    AlertDialog(
        onDismissRequest = {
            cleanupFocus()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = currentPhoto.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onMovePhoto != null && (canMoveLeft || canMoveRight)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onMovePhoto(true) },
                            enabled = canMoveLeft,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("edit_photo_move_left_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move picture earlier",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMovePhoto(false) },
                            enabled = canMoveRight,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("edit_photo_move_right_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move picture later",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.82f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        DocumentPhotoThumbnail(
                            photo = currentPhoto,
                            badgeLabel = "${selectedMode.shortLabel} • ${currentPhoto.rotationAppliedDegrees}°",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "Private Vault (${currentPhoto.width}×${currentPhoto.height}px • Hidden from Android Gallery)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Rotate & Re-Scan Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextRot = (currentPhoto.rotationAppliedDegrees - 90 + 360) % 360
                                vaultStore.applyPhotoEdits(
                                    photoId = currentPhoto.id,
                                    rotationDegrees = nextRot,
                                    customCaption = captionInput,
                                    updatedOcrText = ocrInput
                                ).onSuccess { updated ->
                                    currentPhoto = updated
                                    onPhotoUpdated(updated)
                                    onStatusMessage("Rotated picture 90° left.")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_photo_rotate_left_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateLeft,
                                contentDescription = "Rotate left",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate Left", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                val nextRot = (currentPhoto.rotationAppliedDegrees + 90) % 360
                                vaultStore.applyPhotoEdits(
                                    photoId = currentPhoto.id,
                                    rotationDegrees = nextRot,
                                    customCaption = captionInput,
                                    updatedOcrText = ocrInput
                                ).onSuccess { updated ->
                                    currentPhoto = updated
                                    onPhotoUpdated(updated)
                                    onStatusMessage("Rotated picture 90° right.")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_photo_rotate_right_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate right",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate Right", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Filter / Enhancement Mode Selector
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Document Filter Mode",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DocumentEnhancementMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = selectedMode == mode,
                                    onClick = {
                                        vaultStore.applyPhotoEdits(
                                            photoId = currentPhoto.id,
                                            newMode = mode,
                                            customCaption = captionInput,
                                            updatedOcrText = ocrInput
                                        ).onSuccess { updated ->
                                            currentPhoto = updated
                                            onPhotoUpdated(updated)
                                            onStatusMessage("Applied ${mode.shortLabel} filter.")
                                        }
                                    },
                                    label = { Text(mode.shortLabel) },
                                    modifier = Modifier.testTag("edit_photo_mode_${mode.name}")
                                )
                            }
                        }
                    }

                    // Crop & Margin Trim Selector
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Crop & Framing",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DocumentCropPreset.entries.forEach { crop ->
                                FilterChip(
                                    selected = selectedCrop == crop,
                                    onClick = {
                                        vaultStore.applyPhotoEdits(
                                            photoId = currentPhoto.id,
                                            cropPreset = crop,
                                            customCaption = captionInput,
                                            updatedOcrText = ocrInput
                                        ).onSuccess { updated ->
                                            currentPhoto = updated
                                            onPhotoUpdated(updated)
                                            onStatusMessage("Applied ${crop.shortLabel}.")
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Crop,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = { Text(crop.shortLabel) },
                                    modifier = Modifier.testTag("edit_photo_crop_${crop.name}")
                                )
                            }
                        }
                    }

                    // Brightness / Exposure Adjustment
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Brightness & Contrast Lift",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Darker (-25)" to -25,
                                "Standard (0)" to 0,
                                "Brighter (+25)" to 25,
                                "Ultra Crisp (+45)" to 45
                            ).forEach { (label, offsetVal) ->
                                FilterChip(
                                    selected = brightnessOffset == offsetVal,
                                    onClick = {
                                        brightnessOffset = offsetVal
                                        vaultStore.applyPhotoEdits(
                                            photoId = currentPhoto.id,
                                            brightnessOffset = offsetVal,
                                            customCaption = captionInput,
                                            updatedOcrText = ocrInput
                                        ).onSuccess { updated ->
                                            currentPhoto = updated
                                            onPhotoUpdated(updated)
                                        }
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("edit_photo_brightness_$offsetVal")
                                )
                            }
                        }
                    }

                    // Picture Caption / Label Input
                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Picture Caption / Label (Optional)") },
                        placeholder = { Text("Document Page #${currentPhoto.pageNumber}") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_photo_caption_input")
                    )

                    // On-Device OCR Section (Hidden by default, toggleable to View/Edit)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                                Text(
                                    text = "On-Device OCR Text (Search Indexed)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { showOcrEditor = !showOcrEditor },
                                    modifier = Modifier.testTag("inspect_toggle_ocr_button")
                                ) {
                                    Icon(
                                        imageVector = if (showOcrEditor) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showOcrEditor) "Hide OCR" else "Show / Edit OCR")
                                }
                            }
                            if (showOcrEditor) {
                                OutlinedTextField(
                                    value = ocrInput,
                                    onValueChange = { ocrInput = it },
                                    label = { Text("Extracted OCR Text") },
                                    minLines = 2,
                                    maxLines = 5,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("inspect_ocr_text_container")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            vaultStore.applyPhotoEdits(
                                                photoId = currentPhoto.id,
                                                customCaption = captionInput,
                                                rerunOcr = true
                                            ).onSuccess { updated ->
                                                currentPhoto = updated
                                                ocrInput = updated.ocrText
                                                onPhotoUpdated(updated)
                                                onStatusMessage("Re-scanned on-device OCR text.")
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("edit_photo_rescan_ocr_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Re-Scan OCR", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (onInsertOcrIntoNote != null && ocrInput.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                onInsertOcrIntoNote(ocrInput.trim())
                                                onStatusMessage("Inserted picture OCR text into note.")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("edit_photo_insert_ocr_button")
                                        ) {
                                            Text("Insert into Note", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        cleanupFocus()
                        vaultStore.discardPhotos(setOf(currentPhoto.id))
                        onPhotoDiscarded(currentPhoto.id)
                    },
                    modifier = Modifier.testTag("edit_photo_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Pic", color = MaterialTheme.colorScheme.tertiary)
                }

                TextButton(
                    onClick = {
                        cleanupFocus()
                        onDismiss()
                    }
                ) {
                    Text("Cancel")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    cleanupFocus()
                    vaultStore.applyPhotoEdits(
                        photoId = currentPhoto.id,
                        customCaption = captionInput,
                        updatedOcrText = ocrInput
                    ).onSuccess { updated ->
                        currentPhoto = updated
                        onPhotoUpdated(updated)
                        onStatusMessage("Saved picture edits.")
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("edit_photo_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Edits")
            }
        },
        modifier = Modifier.testTag("edit_photo_dialog")
    )
}

private object DocumentThumbnailCache {
    private val cache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(96)

    fun getOrDecode(photo: CapturedDocumentPhoto): androidx.compose.ui.graphics.ImageBitmap? {
        if (photo.filePath.isBlank()) return null
        val key = "${photo.filePath}_${photo.editRevision}_${photo.sizeBytes}"
        synchronized(cache) {
            cache.get(key)?.let { return it }
        }
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photo.filePath, boundsOpts)
            val targetMaxDim = 360
            var sampleSize = 1
            if (boundsOpts.outHeight > targetMaxDim || boundsOpts.outWidth > targetMaxDim) {
                val halfH = boundsOpts.outHeight / 2
                val halfW = boundsOpts.outWidth / 2
                while ((halfH / sampleSize) >= targetMaxDim && (halfW / sampleSize) >= targetMaxDim) {
                    sampleSize *= 2
                }
            } else {
                sampleSize = 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeFile(photo.filePath, decodeOpts) ?: return null
            val imageBitmap = decoded.asImageBitmap()
            synchronized(cache) {
                cache.put(key, imageBitmap)
            }
            imageBitmap
        }.getOrNull()
    }
}

@Composable
fun DocumentPhotoThumbnail(
    photo: CapturedDocumentPhoto,
    badgeLabel: String?,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(photo.filePath, photo.editRevision, photo.sizeBytes) {
        DocumentThumbnailCache.getOrDecode(photo)
    }

    Box(
        modifier = modifier.background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Captured document scan",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }

        if (!badgeLabel.isNullOrBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ExportDocumentPhotosDialog(
    vaultStore: PrivateDocumentVaultStore,
    photos: List<CapturedDocumentPhoto>,
    defaultTitle: String = "Notii_Document_Scan",
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            vaultStore.writePdfToStream(photos, out).getOrThrow()
                        } ?: throw IllegalStateException("Unable to open output stream")
                    }
                }
                res.onSuccess { count ->
                    onStatusMessage("Exported $count document page(s) to PDF.")
                    onDismiss()
                }.onFailure { err ->
                    onStatusMessage(err.localizedMessage ?: "Failed to export PDF")
                }
            }
        }
    }

    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            vaultStore.writeZipToStream(photos, out).getOrThrow()
                        } ?: throw IllegalStateException("Unable to open ZIP output stream")
                    }
                }
                res.onSuccess { count ->
                    onStatusMessage("Exported $count document image(s) to ZIP archive.")
                    onDismiss()
                }.onFailure { err ->
                    onStatusMessage(err.localizedMessage ?: "Failed to export ZIP archive")
                }
            }
        }
    }

    val timestampSuffix = remember {
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    }
    val cleanBase = remember(defaultTitle) {
        defaultTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(28).ifBlank { "Notii_Doc_Scan" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.IosShare,
                contentDescription = "Export private document scans",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Export ${photos.size} Document Picture(s)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "These pictures are hidden from the Android Gallery and stored privately inside Notii. Choose how you want to export them:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        savePdfLauncher.launch("${cleanBase}_$timestampSuffix.pdf")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_save_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as Multi-Page PDF Document")
                }

                OutlinedButton(
                    onClick = {
                        vaultStore.exportAndSharePdf(context, photos, cleanBase)
                            .onSuccess {
                                onStatusMessage("Prepared PDF document (${photos.size} page(s)) for sharing.")
                                onDismiss()
                            }
                            .onFailure { err ->
                                onStatusMessage(err.localizedMessage ?: "Unable to share PDF")
                            }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_share_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share as Multi-Page PDF")
                }

                OutlinedButton(
                    onClick = {
                        saveZipLauncher.launch("${cleanBase}_$timestampSuffix.zip")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_save_zip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as ZIP Archive (JPEGs)")
                }

                OutlinedButton(
                    onClick = {
                        vaultStore.exportAndShareImages(context, photos)
                            .onSuccess { count ->
                                onStatusMessage("Sharing $count document image(s).")
                                onDismiss()
                            }
                            .onFailure { err ->
                                onStatusMessage(err.localizedMessage ?: "Unable to share images")
                            }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_share_images_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share JPEG Image(s)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_export_dialog_button")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("export_documents_dialog")
    )
}

@Composable
fun PrivateCameraVaultDialog(
    vaultStore: PrivateDocumentVaultStore,
    allPhotos: List<CapturedDocumentPhoto>,
    onLaunchQuickCamera: () -> Unit,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVaultOcrText by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Private Camera Captures",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Private Camera Captures (${allPhotos.size})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "All document captures are hidden from the Android Gallery (.nomedia internal vault) and automatically OCR-indexed for search (OCR hidden by default).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (allPhotos.isNotEmpty()) {
                    val targetOcrPhotos = if (selectedIds.isNotEmpty()) {
                        allPhotos.filter { selectedIds.contains(it.id) }
                    } else {
                        allPhotos
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extracted OCR Text (${targetOcrPhotos.size} doc${if (targetOcrPhotos.size == 1) "" else "s"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showVaultOcrText = !showVaultOcrText },
                            modifier = Modifier.testTag("vault_toggle_ocr_button")
                        ) {
                            Icon(
                                imageVector = if (showVaultOcrText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showVaultOcrText) "Hide OCR Text" else "Show OCR Text",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (showVaultOcrText) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_ocr_text_box")
                        ) {
                            Text(
                                text = vaultStore.getCombinedOcrText(targetOcrPhotos)
                                    .ifBlank { "No extracted OCR text available." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onLaunchQuickCamera()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("vault_dialog_take_shots_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Quick Camera",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (allPhotos.isNotEmpty()) {
                        val allSelected = allPhotos.all { selectedIds.contains(it.id) }
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) emptySet() else allPhotos.map { it.id }.toSet()
                            }
                        ) {
                            Text(if (allSelected) "Deselect All" else "Select All")
                        }
                    }
                }

                if (allPhotos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No private document photos yet. Tap '+ Quick Camera' to capture up to 10 consecutive shots.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        itemsIndexed(allPhotos, key = { _, p -> p.id }) { idx, photo ->
                            val isSelected = selectedIds.contains(photo.id)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.78f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedIds = if (isSelected) selectedIds - photo.id else selectedIds + photo.id
                                    }
                                    .testTag("vault_photo_item_${photo.id}")
                            ) {
                                DocumentPhotoThumbnail(
                                    photo = photo,
                                    badgeLabel = "#${idx + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val removed = vaultStore.discardPhotos(selectedIds)
                                selectedIds = emptySet()
                                onStatusMessage("Discarded $removed private picture(s).")
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vault_discard_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discard (${selectedIds.size})")
                        }

                        Button(
                            onClick = { showExportDialog = true },
                            enabled = allPhotos.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vault_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val countToExport = if (selectedIds.isNotEmpty()) selectedIds.size else allPhotos.size
                            Text("Export ($countToExport)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.testTag("private_camera_vault_dialog")
    )

    if (showExportDialog) {
        val targetPhotos = if (selectedIds.isNotEmpty()) {
            allPhotos.filter { selectedIds.contains(it.id) }
        } else {
            allPhotos
        }
        ExportDocumentPhotosDialog(
            vaultStore = vaultStore,
            photos = targetPhotos,
            defaultTitle = "Notii_Private_Vault_Docs",
            onDismiss = { showExportDialog = false },
            onStatusMessage = onStatusMessage
        )
    }
}

private fun decodeBitmapFromImageProxy(image: ImageProxy): Bitmap? {
    return runCatching {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer: ByteBuffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
