package com.example.hireapp.screens.applicant.interviewcapture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.hireapp.util.LoadingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import androidx.camera.core.Preview as CameraPreview

@Composable
fun QuestionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val previewHeight = screenWidth * 3 / 4
    val sessionId = remember { "session_${System.currentTimeMillis()}" }

    // 기본 질문 + AI 생성 질문
    val fixedQuestions = listOf(
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

    var showRetryDialog by remember { mutableStateOf(false) }
    var showReSaveDialog by remember { mutableStateOf(false) }
    var errorOccurred by remember { mutableStateOf(false) }

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
                currentIndex,
                onError = {
                    errorOccurred = true
                    showRetryDialog = true
                }
            )
        }

        while (timeLeft > 0 && !errorOccurred) {
            delay(1000L)
            timeLeft--
        }

        if (!errorOccurred) {
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
                    showDialog = true // 마지막 질문 끝 -> 저장 여부 팝업 표시
                }
            }
        }
    }

    // 영상 촬영 중 오류 발생시 재촬영 또는 촬영 중단
    if (showRetryDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("촬영 오류") },
            text = { Text("촬영 중 오류가 발생했습니다. 현재 질문부터 다시 촬영하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showRetryDialog = false
                    errorOccurred = false
                    phase = "prepare" // 현재 질문을 다시 촬영
                }) {
                    Text("다시 촬영")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showRetryDialog = false
                    showDialog = true // 마지막 질문 끝 -> 저장 여부 팝업 표시
                }) {
                    Text("촬영 중단")
                }
            }
        )
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
            Text(
                "질문 ${currentIndex + 1} / ${allQuestions.size}",
                style = MaterialTheme.typography.titleMedium
            )
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

    var reSaveFlag: Boolean = false

    // 영상 저장 여부 팝업
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("영상 저장") },
            text = { Text("영상을 저장하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false

                    // storage 에 영상 업로드 후 db에 저장
                    uploadVideoAndSave(
                        recordedFiles,
                        sessionId,
                        reSaveFlag,
                        navController,
                        onError = {
                            errorOccurred = true
                            showReSaveDialog = true
                        }
                    )
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

    if (showReSaveDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("영상 저장 오류") },
            text = { Text("영상 저장 중 오류가 발생했습니다. 다시 저장을 시도하시겠습니까?") },
            confirmButton = {
                Button(onClick = {
                    showReSaveDialog = false
                    errorOccurred = false
                    reSaveFlag = true

                    uploadVideoAndSave(
                        recordedFiles,
                        sessionId,
                        reSaveFlag,
                        navController,
                        onError = {
                            errorOccurred = true
                            showReSaveDialog = true
                        }
                    )
                }) {
                    Text("다시 저장")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showRetryDialog = false
                    navController.navigate(Screen.Capture.route) {
                        popUpTo(Screen.Capture.route) { inclusive = true }
                    }
                }) {
                    Text("저장 취소")
                }
            }
        )
    }
}

/**
 * 영상을 storage에 업로드 하고 db에 저장
 *
 * @param recordedFiles 촬영된 영상 목록
 * @param sessionId 영상 촬영 시작한 밀리초 기준, 영상 폴더의 path 명으로 사용함
 *
 * @throws onError storage 업로드 실패, db 저장 실패 시 재시도 요청
 */
fun uploadVideoAndSave(
    recordedFiles: SnapshotStateList<File>,
    sessionId: String,
    reSaveFlag: Boolean,
    navController: NavController,
    onError: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storageRef = storage.reference
    val VIDEO_PATH = "interview-films/${sessionId}"

    LoadingState.show()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 저장을 다시 시도하는 경우 기존에 저장돼있던 영상들 삭제
            if (reSaveFlag) {
                storageRef.child(VIDEO_PATH).listAll().addOnSuccessListener { listResult ->
                    listResult.items.forEach { it.delete() }
                }
            }

            val videoUrls = mutableListOf<String>()

            // storage에 업로드
            recordedFiles.forEach { file ->
                val fileUri = Uri.fromFile(file)
                val videoRef = storageRef.child("${VIDEO_PATH}/${file.name}")

                videoRef.putFile(fileUri).await()
                val downloadUrl = videoRef.downloadUrl.await()
                videoUrls.add(downloadUrl.toString())
            }

            // db에 저장
            val videoData = hashMapOf(
                "videoPath" to VIDEO_PATH,
                "videos" to videoUrls.map { mapOf("fileUrl" to it) }
            )

            db.collection("videos")
                .document(sessionId)
                .set(videoData)
                .await()

            // 저장 완료되면 기존 영상들 삭제
            withContext(Dispatchers.Main) {
                recordedFiles.forEach { it.delete() }
                navController.navigate(Screen.Capture.route) {
                    popUpTo(Screen.Capture.route) { inclusive = true }
                }
            }
        } catch (e: Exception) {
            Log.e("UploadError", "업로드 실패: ${e.message}")
            withContext(Dispatchers.Main) {
                onError()
            }
        } finally {
            LoadingState.hide()
        }
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
    questionIndex: Int,
    onError: () -> Unit
) {
    try {
        val fileName = "${sessionId}_q${questionIndex + 1}.mp4"
        val file = File(context.filesDir, fileName)

        Log.d("VideoCapture", " 녹화 시작 준비: $fileName")

        recordedFiles.add(file)

        val outputOptions = FileOutputOptions.Builder(file).build()

        val recording = videoCapture?.output
            ?.prepareRecording(context, outputOptions)
            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (event.hasError()) {
                        Log.e("VideoCapture", " 녹화 실패: ${event.error}")
                        onError()
                    } else {
                        Log.d("VideoCapture", " 녹화 완료: ${file.absolutePath}")
                    }
                }
            }

        if (recording == null) {
            Log.e("VideoCapture", " 녹화 시작 실패: videoCapture == null")
            onError()
        } else {
            Log.d("VideoCapture", " 녹화 시작됨: $fileName")
        }

        recordingRef.value = recording
    } catch (e: Exception) {
        Log.e("Recording", "녹화 중 오류 발생: ${e.message}")
        onError()
    }
}

// 녹화 중지
fun stopRecording(recordingRef: MutableState<Recording?>) {
    Log.d("VideoCapture", " 녹화 중지 요청됨")
    recordingRef.value?.stop()
    recordingRef.value = null
}