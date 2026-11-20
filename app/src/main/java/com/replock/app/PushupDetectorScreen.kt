package com.replock.app

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PushupDetectorScreen(
    onPushupsCompleted: (Int) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var pushupCount by remember { mutableIntStateOf(0) }
    var sessionPushups by remember { mutableIntStateOf(0) }
    var currentState by remember { mutableStateOf(PushupState.UNKNOWN) }
    var feedback by remember { mutableStateOf("Stand in front of the camera. Full body visible.") }

    val poseDetector = remember {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            poseDetector.close()
            cameraExecutor.shutdown()
            // Award minutes when leaving the screen
            if (sessionPushups > 0) {
                onPushupsCompleted(sessionPushups)
            }
        }
    }

    if (!cameraPermission.status.isGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF03030A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Camera permission required",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("Grant permission")
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(imageProxy, poseDetector) { pose ->
                                    val result = detectPushup(pose, currentState)
                                    currentState = result.newState
                                    if (result.counted) {
                                        pushupCount++
                                        sessionPushups++
                                        val minutesEarned = sessionPushups / 2
                                        feedback = if (sessionPushups % 2 == 0) {
                                            "Good. +1 minute earned"
                                        } else {
                                            "Good form. One more for +1 minute"
                                        }
                                    } else {
                                        feedback = result.feedback
                                    }
                                }
                            }
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("PushupDetector", "Bind failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            Text(
                text = currentState.name,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Feedback
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = feedback,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Bottom counter
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF12121E).copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 40.dp, vertical = 22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Push-ups this session",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "$sessionPushups",
                    color = Color(0xFF8AB4FF),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "2 push-ups = 1 minute",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private enum class PushupState { UNKNOWN, UP, DOWN }

private data class PushupResult(
    val newState: PushupState,
    val counted: Boolean,
    val feedback: String
)

private fun detectPushup(pose: Pose, previous: PushupState): PushupResult {
    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
    val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
    val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
    val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
    val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

    if (leftShoulder == null || rightShoulder == null ||
        leftElbow == null || rightElbow == null ||
        leftWrist == null || rightWrist == null
    ) {
        return PushupResult(previous, false, "Make sure shoulders, elbows and wrists are visible")
    }

    val leftAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
    val rightAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
    val avgElbowAngle = (leftAngle + rightAngle) / 2.0

    val isUp = avgElbowAngle > 145
    val isDown = avgElbowAngle < 105

    return when {
        previous == PushupState.UP && isDown ->
            PushupResult(PushupState.DOWN, false, "Lower position. Push up.")
        previous == PushupState.DOWN && isUp ->
            PushupResult(PushupState.UP, true, "Good form")
        isUp -> PushupResult(PushupState.UP, false, "Ready. Lower your body.")
        isDown -> PushupResult(PushupState.DOWN, false, "Push up with control.")
        else -> PushupResult(previous, false, "Move with control")
    }
}

private fun calculateAngle(
    first: PoseLandmark,
    mid: PoseLandmark,
    last: PoseLandmark
): Double {
    val angle = Math.toDegrees(
        atan2(
            (last.position.y - mid.position.y).toDouble(),
            (last.position.x - mid.position.x).toDouble()
        ) - atan2(
            (first.position.y - mid.position.y).toDouble(),
            (first.position.x - mid.position.x).toDouble()
        )
    )
    var result = abs(angle)
    if (result > 180) result = 360 - result
    return result
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    poseDetector: com.google.mlkit.vision.pose.PoseDetector,
    onPose: (Pose) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    poseDetector.process(image)
        .addOnSuccessListener { onPose(it) }
        .addOnFailureListener { Log.e("PushupDetector", "Detection failed", it) }
        .addOnCompleteListener { imageProxy.close() }
}
