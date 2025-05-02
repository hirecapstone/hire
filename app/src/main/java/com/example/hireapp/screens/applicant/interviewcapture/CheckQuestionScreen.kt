@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hireapp.screens.applicant.interviewcapture

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen
import kotlin.math.abs
import kotlin.math.atan2
import java.util.ArrayList

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CheckQuestionScreen(navController: NavController) {
    // 1) 질문, 인덱스, 타이머
    val questions    = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<ArrayList<String>>("questions") ?: arrayListOf()
    var currentIndex by remember { mutableStateOf(0) }
    var isReady      by remember { mutableStateOf(true) }
    var timer        by remember { mutableStateOf(30) }

    // 2) ML Kit 클라이언트
    val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    // 3) 상태 변수
    var expression by remember { mutableStateOf("무표정") }
    var posture   by remember { mutableStateOf("정자세") }
    var gaze      by remember { mutableStateOf("정면") }

    // 4) 타이머
    LaunchedEffect(currentIndex, isReady) {
        timer = if (isReady) 30 else 60
        while (timer > 0) {
            delay(1_000L)
            timer--
        }
        if (isReady) {
            isReady = false
        } else {
            if (currentIndex < questions.size - 1) {
                currentIndex++; isReady = true
            } else {
                navController.navigate(Screen.HomeAppl.route)
            }
        }
    }

    // 5) 권한
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    // 6) 카메라 준비
    val lifecycleOwner = LocalLifecycleOwner.current
    val isInPreview    = LocalInspectionMode.current
    val previewView    = remember { PreviewView(context) }
    val cameraFuture   = remember { ProcessCameraProvider.getInstance(context) }
    val scope          = rememberCoroutineScope()

    LaunchedEffect(cameraFuture, hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        cameraFuture.addListener({
            val provider = cameraFuture.get()
            val preview  = CameraPreview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        // 1) InputImage 생성
                        imageProxy.image?.let { mediaImage ->
                            val input = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            // 2) 코루틴으로 분석 대기
                            scope.launch {
                                // 얼굴
                                val faces = faceDetector.process(input).await()
                                faces.firstOrNull()?.let { f ->
                                    val l = f.leftEyeOpenProbability  ?: 0f
                                    val r = f.rightEyeOpenProbability ?: 0f
                                    expression =
                                        if ( (f.smilingProbability ?: 0f) > 0.3f && l > 0.4f && r > 0.4f )
                                            "웃음"
                                        else "무표정"
                                    gaze =
                                        if (abs(f.headEulerAngleY) < 15f) "정면" else "정면아님"
                                } ?: run {
                                    expression = "무표정"; gaze = "정면"
                                }
                                // 자세
                                val pose = poseDetector.process(input).await()
                                val ls   = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                val rs   = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                                posture = if (ls != null && rs != null) {
                                    val dx = rs.position.x - ls.position.x
                                    val dy = rs.position.y - ls.position.y
                                    val ang = abs(Math.toDegrees(atan2(dy, dx).toDouble()))
                                    val slope = abs(dy / (dx.takeIf { it != 0f } ?: 1f))
                                    if (ang < 10 || slope < 0.1)"정자세" else "구부정"

                                } else "정자세"
                                imageProxy.close()
                            }
                        } ?: imageProxy.close()
                    }
                }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA,
                preview, analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    // 7) UI
    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    if (isReady) isReady = false
                    else if (currentIndex < questions.size - 1) {
                        currentIndex++; isReady = true
                    } else {
                        navController.navigate(Screen.HomeAppl.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(if (isReady) "준비 완료" else "답변 완료")
            }
        }
    ) { ip ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(ip)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isInPreview -> Box(Modifier.matchParentSize()) { }
                    !hasPermission -> Box(
                        Modifier
                            .matchParentSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("카메라 권한 필요") }
                    else -> AndroidView(
                        factory = { previewView },
                        modifier = Modifier.matchParentSize()
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expression,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = posture,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = gaze,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("질문 ${currentIndex+1}/${questions.size}",
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(questions.getOrNull(currentIndex) ?: "",
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isReady) "준비시간: $timer 초" else "남은 시간: $timer 초",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCheckQuestionScreen() {
    CheckQuestionScreen(
        navController = androidx.navigation.compose.rememberNavController()
    )
}
