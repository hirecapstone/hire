package com.example.hireapp.screens.applicant.interviewcapture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query

@Composable
fun QuestionScreen(navController: NavController, sessionId: String, major: String, sub: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val previewHeight = screenWidth * 3 / 4

    // Firestore 참조
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // 질문 리스트
    val fixedQuestions = listOf(
        "자기소개 해주세요.",
        "지원 동기는 무엇인가요?",
        "본인의 장단점을 말해주세요."
    )
    var aiQuestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // 제목 입력 관련 상태
    var videoTitle by remember { mutableStateOf("") }
    var showTitleDialog by remember { mutableStateOf(false) }

    // Firestore에서 특정 세션 ID에 해당하는 질문만 가져오기
    DisposableEffect(sessionId) {
        val listener = db.collection("interview_questions")
            .whereEqualTo("sessionId", sessionId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("FirestoreError", "질문 로드 실패: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    aiQuestions = snapshots.documents.flatMap { document ->
                        val questions = document.get("questions") as? List<String>
                        questions ?: emptyList()
                    }
                }
            }

        onDispose {
            listener.remove()
        }
    }

    // 전체 질문 리스트
    val allQuestions = remember { derivedStateOf { fixedQuestions + aiQuestions } }

    var currentIndex by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf("prepare") }
    var timeLeft by remember { mutableStateOf(30) }

    val videoCapture = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recording = remember { mutableStateOf<Recording?>(null) }
    val recordedFiles = remember { mutableStateListOf<File>() }

    var showRetryDialog by remember { mutableStateOf(false) }
    var errorOccurred by remember { mutableStateOf(false) }

    // Firestore 질문 로드 상태 확인
    if (aiQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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
                if (currentIndex < allQuestions.value.lastIndex) {
                    currentIndex++
                    phase = "prepare"
                } else {
                    showTitleDialog = true // 제목 작성 다이얼로그 표시
                }
            }
        }
    }

    // 화면 구성
    Column(modifier = Modifier.fillMaxSize()) {
        CameraPreviewViewWithVideo(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight),
            lifecycleOwner = lifecycleOwner,
            onVideoCaptureReady = { videoCapture.value = it }
        )

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
                "질문 ${currentIndex + 1} / ${allQuestions.value.size}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(allQuestions.value[currentIndex], style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text("남은 시간: ${timeLeft}초")
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (phase == "prepare") {
                        phase = "answer"
                    } else {
                        stopRecording(recording)
                        if (currentIndex < allQuestions.value.lastIndex) {
                            currentIndex++
                            phase = "prepare"
                        } else {
                            showTitleDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(if (phase == "prepare") "준비 완료" else "답변 완료")
            }
        }
    }

    // 제목 작성 다이얼로그
    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("영상 제목 작성") },
            text = {
                Column {
                    Text("영상의 제목을 작성해주세요:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = videoTitle,
                        onValueChange = { videoTitle = it },
                        label = { Text("제목 입력") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    uploadVideoAndSave(recordedFiles, sessionId, major, sub, videoTitle, navController)
                    showTitleDialog = false
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showTitleDialog = false
                }) {
                    Text("취소")
                }
            }
        )
    }
}

// 녹화 시작 함수
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

        recordedFiles.add(file)

        val outputOptions = FileOutputOptions.Builder(file).build()

        val recording = videoCapture?.output
            ?.prepareRecording(context, outputOptions)
            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (event.hasError()) {
                        Log.e("VideoCapture", "녹화 실패: ${event.error}")
                        onError()
                    } else {
                        Log.d("VideoCapture", "녹화 완료: ${file.absolutePath}")
                    }
                }
            }

        if (recording == null) {
            Log.e("VideoCapture", "녹화 시작 실패: videoCapture == null")
            onError()
        } else {
            Log.d("VideoCapture", "녹화 시작됨: $fileName")
        }

        recordingRef.value = recording
    } catch (e: Exception) {
        Log.e("Recording", "녹화 중 오류 발생: ${e.message}")
        onError()
    }
}

// 녹화 중지 함수
fun stopRecording(recordingRef: MutableState<Recording?>) {
    recordingRef.value?.stop()
    recordingRef.value = null
}

// 카메라 미리보기 설정
@Composable
fun CameraPreviewViewWithVideo(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
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

// 영상 업로드 및 Firestore 저장
fun uploadVideoAndSave(
    recordedFiles: List<File>,
    sessionId: String,
    major: String,
    sub: String,
    videoTitle: String, // 제목 추가
    navController: NavController
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storageRef = storage.reference
    val VIDEO_PATH = "videos/$sessionId"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val videoUrls = mutableListOf<String>()

            // 파일을 Firebase Storage에 업로드
            recordedFiles.forEach { file ->
                val fileUri = Uri.fromFile(file)
                val videoRef = storageRef.child("$VIDEO_PATH/${file.name}")

                videoRef.putFile(fileUri).await()
                val downloadUrl = videoRef.downloadUrl.await()
                videoUrls.add(downloadUrl.toString())
            }

            // Firestore에서 가장 최근 생성된 interview_questions 문서 가져오기
            val latestQuestionRef = db.collection("interview_questions")
                .orderBy("uploadTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val questionPath = latestQuestionRef?.reference?.path ?: ""

            // Firestore에 저장할 데이터 생성
            val videoData = hashMapOf(
                "category" to hashMapOf(
                    "major" to major, // selectrolescreen에서 전달된 category
                    "sub" to sub      // selectrolescreen에서 전달된 job
                ),
                "question" to db.document(questionPath), // 가장 최근 문서 참조
                "title" to videoTitle, // 영상 제목 저장
                "uploadTime" to FieldValue.serverTimestamp(),
                "user" to db.document("/users/${auth.currentUser?.uid}"),
                "videos" to videoUrls.map { mapOf("fileUrl" to it) },
                "feedback" to " ",
                "public" to "True"
            )

            // Firestore에 데이터 저장
            db.collection("interview")
                .document(sessionId)
                .set(videoData)
                .await()

            withContext(Dispatchers.Main) {
                // 로컬 파일 삭제 및 마이페이지로 이동
                recordedFiles.forEach { it.delete() }
                navController.navigate(Screen.MyPageAppl.route) {
                    popUpTo(Screen.MyPageAppl.route) { inclusive = true }
                }
            }
        } catch (e: Exception) {
            Log.e("UploadError", "업로드 실패: ${e.message}")
        }
    }
}