package com.example.hireapp.screens.applicant.interviewcapture

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun QuestionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val previewHeight = screenWidth * 3 / 4
    val sessionId = remember { "session_${System.currentTimeMillis()}" }

    // 기본 질문 + AI 생성 질문
    val fixedQuestions = listOf( // 공통 질문 정한거 기억 안나서 임의로 작성했어요
        "자기소개 해주세요.",
        "지원 동기는 무엇인가요?",
        "본인의 장단점을 말해주세요."
    )
    val allQuestions = remember { fixedQuestions + List(6) { "AI 생성 질문 ${it + 1}" } }

    var currentIndex by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf("prepare") } // "prepare" 또는 "answer"
    var timeLeft by remember { mutableStateOf(30) }

    val videoCapture = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recording = remember { mutableStateOf<Recording?>(null) }
    val recordedFiles = remember { mutableStateListOf<File>() }

    var showDialog by remember { mutableStateOf(false) }

    // 질문 단계별 타이머 및 녹화
    LaunchedEffect(phase, currentIndex) {
        timeLeft = if (phase == "prepare") 30 else 60

        if (phase == "answer") {
            startRecording(
                context,
                videoCapture.value,
                recording,
                recordedFiles,
                sessionId,
                currentIndex
            )
        }

        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }

        if (phase == "answer") {
            stopRecording(recording)
        }

        if (phase == "prepare") {
            phase = "answer"
        } else {
            if (currentIndex < allQuestions.lastIndex) {
                currentIndex++
                phase = "prepare"
            } else {
                showDialog = true // 마지막 질문 끝 → 저장 여부 팝업 표시
            }
        }
    }

    // 화면 구성
    Column(modifier = Modifier.fillMaxSize()) {

        // 카메라 미리보기 (4:3 비율)
        CameraPreviewViewWithVideo(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight),
            lifecycleOwner = lifecycleOwner,
            onVideoCaptureReady = { videoCapture.value = it }
        )

        // 질문 + 타이머 + 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("질문 ${currentIndex + 1} / ${allQuestions.size}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(allQuestions[currentIndex], style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text("남은 시간: ${timeLeft}초")
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (phase == "prepare") {
                        phase = "answer"
                    } else {
                        stopRecording(recording)
                        if (currentIndex < allQuestions.lastIndex) {
                            currentIndex++
                            phase = "prepare"
                        } else {
                            showDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(if (phase == "prepare") "준비 완료" else "답변 완료")
            }
        }
    }

    // 영상 저장 여부 팝업
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("영상 저장") },
            text = { Text("영상을 저장하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    navController.navigate(Screen.Capture.route) {
                        popUpTo(Screen.Capture.route) { inclusive = true }
                    }
                }) {
                    Text("예")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // 영상 삭제 처리
                    recordedFiles.forEach { it.delete() }
                    showDialog = false
                    navController.navigate(Screen.Capture.route) {
                        popUpTo(Screen.Capture.route) { inclusive = true }
                    }
                }) {
                    Text("아니오")
                }
            }
        )
    }
}

@Composable
fun CameraPreviewViewWithVideo(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    // 카메라 뷰 연결 (CameraX)
    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = CameraPreview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)

            onVideoCaptureReady(videoCapture)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

// 녹화 시작
fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    recordingRef: MutableState<Recording?>,
    recordedFiles: SnapshotStateList<File>,
    sessionId: String,
    questionIndex: Int
) {
    val fileName = "${sessionId}_q${questionIndex + 1}.mp4"
    val file = File(context.cacheDir, fileName)

    Log.d("VideoCapture", " 녹화 시작 준비: $fileName")

    recordedFiles.add(file)

    val outputOptions = FileOutputOptions.Builder(file).build()

    val recording = videoCapture?.output
        ?.prepareRecording(context, outputOptions)
        ?.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError()) {
                    Log.e("VideoCapture", " 녹화 실패: ${event.error}")
                } else {
                    Log.d("VideoCapture", " 녹화 완료: ${file.absolutePath}")
                }
            }
        }

    if (recording == null) {
        Log.e("VideoCapture", " 녹화 시작 실패: videoCapture == null")
    } else {
        Log.d("VideoCapture", " 녹화 시작됨: $fileName")
    }

    recordingRef.value = recording
}


fun stopRecording(recordingRef: MutableState<Recording?>) {
    Log.d("VideoCapture", " 녹화 중지 요청됨")
    recordingRef.value?.stop()
    recordingRef.value = null
}

