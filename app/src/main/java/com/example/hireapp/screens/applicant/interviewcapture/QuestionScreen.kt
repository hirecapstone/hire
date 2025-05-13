@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hireapp.screens.applicant.interviewcapture

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
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
import android.Manifest
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan2
import androidx.camera.core.Preview as CameraPreview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.hireapp.util.LoadingState
import com.google.firebase.firestore.SetOptions

@Composable
fun QuestionScreen(navController: NavController, sessionId: String, major: String, sub: String) {
    Log.d("SessionIdCheck", "전달된 sessionId: $sessionId")
    Log.d("MajorSubCheck", "전달된 Major: $major, Sub: $sub")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val previewHeight = screenWidth * 6 / 5
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
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
                    if (!snapshots.isEmpty) {
                        aiQuestions = snapshots.documents.flatMap { document ->
                            val questions = document.get("questions") as? List<String>
                            questions ?: emptyList()
                        }
                        LoadingState.hide()
                        Log.d("FirestoreSuccess", "질문 로드 성공: $aiQuestions")
                    } else {
                        Log.w("FirestoreWarning", "해당 세션 ID에 대한 질문이 없습니다.")
                    }
                } else {
                    Log.e("FirestoreError", "스냅샷이 null입니다.")
                }
            }

        onDispose {
            listener.remove()
        }
    }

    // 전체 질문 리스트
    val allQuestions = remember { derivedStateOf { aiQuestions } }
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf("prepare") }
    var timeLeft by remember { mutableStateOf(30) }

    val videoCapture = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val recording = remember { mutableStateOf<Recording?>(null) }
    val recordedFiles = remember { mutableStateListOf<File>() }

    var showRetryDialog by remember { mutableStateOf(false) }
    var errorOccurred by remember { mutableStateOf(false) }

    var expression by remember { mutableStateOf("무표정") }
    var posture   by remember { mutableStateOf("정자세") }
    var gaze      by remember { mutableStateOf("정면") }

    val smileTimestamps      = remember { mutableStateListOf<Int>() }
    val badPostureTimestamps = remember { mutableStateListOf<Int>() }
    val notFrontTimestamps   = remember { mutableStateListOf<Int>() }
    var answerElapsed        by remember { mutableStateOf(0) }


    // Firestore 질문 로드 상태 확인
    if (aiQuestions.isEmpty()) {
        LoadingState.show("질문을 생성 중입니다.")
        return
    }

    // 권한 요청 상태 확인
    var hasPermission by remember { mutableStateOf(false) }

    // 권한 요청 콜백 함수
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    // 권한 요청
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        } else {
            hasPermission = true
        }
    }

    // 타이머 및 녹화/분석 로직 통합
    LaunchedEffect(phase, currentIndex) {
        if (phase == "prepare" || phase == "answer") {
            timeLeft = if (phase == "prepare") 30 else 60
            answerElapsed = 0
            smileTimestamps.clear()
            badPostureTimestamps.clear()
            notFrontTimestamps.clear()

            if (phase == "answer" && hasPermission) {
                startRecording(
                    context,
                    videoCapture.value,
                    recording,
                    recordedFiles,
                    sessionId,
                    currentIndex,
                    onError = {
                        // 오류 처리
                    },
                    permissionLauncher
                )
            }

            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
                if (phase == "answer") {
                    answerElapsed++
                    if (expression == "웃음")      smileTimestamps.add(answerElapsed)
                    if (posture == "구부정")       badPostureTimestamps.add(answerElapsed)
                    if (gaze == "정면아님")          notFrontTimestamps.add(answerElapsed)
                }
            }

            if (phase == "answer") {
                stopRecording(recording)
                saveMediapipeResult(
                    db, sessionId, currentIndex,
                    smileTimestamps, badPostureTimestamps, notFrontTimestamps
                )
            }

            if (phase == "prepare") {
                phase = "answer"
            } else {
                if (currentIndex < aiQuestions.lastIndex) {
                    currentIndex++
                    phase = "prepare"
                } else {
                    phase = "done"
                    showTitleDialog = true
                }
            }
        }
    }


    // 화면 구성
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
        ) {
            // 기존 VideoCapture용 PreviewView
            CameraPreviewViewWithVideo(
                modifier = Modifier.matchParentSize(),
                lifecycleOwner = lifecycleOwner,
                onVideoCaptureReady = { videoCapture.value = it },
                // → onAnalysis 콜백으로 실시간 결과 받기
                onAnalysis = { expr, post, gz ->
                    expression = expr
                    posture = post
                    gaze = gz
                }
            )
            // 분석 결과 텍스트
            Box(
                Modifier
                    .matchParentSize()
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 질문 번호 + 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.hireapp.R.drawable.document),
                    contentDescription = "질문 이미지",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "질문 ${currentIndex + 1} / ${allQuestions.value.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 질문 내용 + GPT 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.hireapp.R.drawable.gpt),
                    contentDescription = "GPT 이미지",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    allQuestions.value[currentIndex],
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 남은 시간 + 시계 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.hireapp.R.drawable.time),
                    contentDescription = "남은 시간 이미지",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("남은 시간: ${timeLeft}초")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (phase == "prepare") {
                        phase = "answer"
                    } else {
                        stopRecording(recording)
                        saveMediapipeResult(
                            db, sessionId, currentIndex,
                            smileTimestamps, badPostureTimestamps, notFrontTimestamps
                        )
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

    var isUploading by remember { mutableStateOf(false) } // 업로드 상태 관리
    var uploadError by remember { mutableStateOf<String?>(null) } // 업로드 에러 상태 관리


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
                    isUploading = true // 업로드 시작 시 로딩 상태 활성화
                    uploadError = null // 기존 에러 초기화

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // 업로드 로직 실행
                            uploadVideoAndSave(
                                recordedFiles = recordedFiles,
                                sessionId = sessionId,
                                major = major,
                                sub = sub,
                                videoTitle = videoTitle
                            )
                            withContext(Dispatchers.Main) {
                                // 업로드 완료 후 로딩 종료 및 피드백 화면으로 이동
                                isUploading = false
                                showTitleDialog = false
                                navController.navigate("feedback_screen/$sessionId")
                            }
                        } catch (e: Exception) {
                            Log.e("UploadError", "업로드 중 오류 발생: ${e.message}")
                            withContext(Dispatchers.Main) {
                                uploadError = e.message
                                isUploading = false
                            }
                        }
                    }
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                Button(onClick = {
                    // 제목 저장 없이 홈 화면 이동
                    recordedFiles.clear()
                    showTitleDialog = false
                    navController.navigate(Screen.HomeAppl.route)
                }) {
                    Text("취소")
                }
            }
        )
    }
    // 업로드 중 로딩 화면
    if (isUploading) {
        LoadingState.show("피드백을 생성 중입니다.")
        return
    }

    // 에러 메시지 표시
    uploadError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { uploadError = null },
            title = { Text("오류 발생") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { uploadError = null }) {
                    Text("확인")
                }
            }
        )
    }
}

// 권한 요청을 처리하는 함수
private fun requestPermissions(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
) {
    // 권한 요청: 오디오 및 카메라 권한 요청
    permissionLauncher.launch(
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )
}

// 녹화 시작 함수
fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    recordingRef: MutableState<Recording?>,
    recordedFiles: SnapshotStateList<File>,
    sessionId: String,
    questionIndex: Int,
    onError: () -> Unit,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        try {
            val fileName = "${sessionId}_q${questionIndex + 1}.mp4"
            val file = File(context.filesDir, fileName)

            recordedFiles.add(file)

            val outputOptions = FileOutputOptions.Builder(file).build()

            val recording = videoCapture?.output
                ?.prepareRecording(context, outputOptions)
                ?.apply {
                    withAudioEnabled()  // 오디오 캡처를 활성화
                }
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
    } else {
        // 오디오 권한이 없으면 권한을 요청
        requestPermissions(context, permissionLauncher)
    }
}

// 권한 요청 콜백 함수 설정
@Composable
fun PermissionRequester(onPermissionGranted: () -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            Log.d("Permissions", "All required permissions granted")
            onPermissionGranted()  // 권한이 허용되면 이후 로직 실행
        } else {
            Log.d("Permissions", "Required permissions denied")
        }
    }
    // 권한 요청
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }
}

// 녹화 중지 함수
fun stopRecording(recordingRef: MutableState<Recording?>) {
    recordingRef.value?.stop()
    recordingRef.value = null
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewViewWithVideo(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onAnalysis: (expression: String, posture: String, gaze: String) -> Unit,
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scope = rememberCoroutineScope()

    // ML Kit 설정
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

    AndroidView(factory = { previewView }, modifier = modifier) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = CameraPreview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            // VideoCapture (녹화)
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)
            onVideoCaptureReady(videoCapture)

            // ImageAnalysis (실시간 얼굴·자세 분석)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        imageProxy.image?.let { mediaImage ->
                            val input = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scope.launch {
                                // 얼굴
                                val faces = faceDetector.process(input).await()
                                val expr = faces.firstOrNull()?.let { f ->
                                    val l = f.leftEyeOpenProbability ?: 0f
                                    val r = f.rightEyeOpenProbability ?: 0f
                                    if ((f.smilingProbability ?: 0f) > 0.3f && l > 0.4f && r > 0.4f)
                                        "웃음" else "무표정"
                                } ?: "무표정"
                                val gz = faces.firstOrNull()?.let { f ->
                                    if (abs(f.headEulerAngleY) < 15f) "정면" else "정면아님"
                                } ?: "정면"

                                // 어깨 좌우 기울기만 판단
                                val pose = poseDetector.process(input).await()
                                val ls = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                val rs = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                                val post = if (ls != null && rs != null) {
                                    val dx = rs.position.x - ls.position.x
                                    val dy = rs.position.y - ls.position.y
                                    val ang = abs(Math.toDegrees(atan2(dy, dx).toDouble()))
                                    val slope = abs(dy / (dx.takeIf { it != 0f } ?: 1f))
                                    if (ang < 10 || slope < 0.1f) "정자세" else "구부정"
                                } else "정자세"

                                onAnalysis(expr, post, gz)
                                imageProxy.close()
                            }
                        } ?: imageProxy.close()
                    }
                }

            // 바인딩
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                videoCapture,
                analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }
}

fun uploadVideoAndSave(
    recordedFiles: List<File>,
    sessionId: String,
    major: String,
    sub: String,
    videoTitle: String
) {
    val db = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference
    val auth = FirebaseAuth.getInstance()
    val VIDEO_PATH = "videos/$sessionId"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1) 동영상 업로드
            val videoUrls = mutableListOf<String>()
            for ((index, file) in recordedFiles.withIndex()) {
                if (!file.exists()) {
                    Log.e("UploadError", "파일이 존재하지 않습니다: ${file.absolutePath}")
                    continue
                }

                val fileUri = Uri.fromFile(file)
                val videoRef = storageRef.child("$VIDEO_PATH/${sessionId}_q${index + 1}.mp4")
                try {
                    Log.d("UploadFile", "파일 업로드 시작: ${file.name}")
                    videoRef.putFile(fileUri).await()
                    Log.d("UploadFile", "파일 업로드 성공: ${file.name}")
                    val downloadUrl = videoRef.downloadUrl.await()
                    videoUrls.add(downloadUrl.toString())
                } catch (e: Exception) {
                    Log.e("UploadError", "파일 업로드 실패: ${e.message}")
                    throw e // 문제 발생 시 중단
                }
            }

            // 2) Firestore 데이터 저장
            val questions = db.collection("interview_questions")
                .document(sessionId)
                .get()
                .await()
                .get("questions") as? List<String> ?: emptyList()

            val mediapipeData = db.collection("interview_mediapipe")
                .document(sessionId)
                .get()
                .await()
                .data ?: emptyMap<String, Any>()

            val feedbackRef = db.collection("interview_feedback").document(sessionId)

            val videoData = mapOf(
                "category" to mapOf(
                    "major" to major,
                    "sub" to sub
                ),
                "question" to questions,
                "title" to videoTitle,
                "uploadTime" to FieldValue.serverTimestamp(),
                "user" to auth.currentUser?.uid,
                "videos" to videoUrls.map { mapOf("fileUrl" to it) },
                "mediapipe" to mediapipeData,
                "feedback" to feedbackRef,
                "public" to true
            )

            db.collection("interview")
                .document(sessionId)
                .set(videoData, SetOptions.merge())
                .await()
            Log.d("UploadResults", "데이터 저장 성공")
        } catch (e: Exception) {
            Log.e("UploadError", "업로드 실패: ${e.message}")
            throw e // 호출한 곳으로 전달
        }
    }
}

private fun saveMediapipeResult(
    db: FirebaseFirestore,
    sessionId: String,
    videoIndex: Int,
    smiles: List<Int>,
    bads: List<Int>,
    nots: List<Int>
) {
    val field = "video${videoIndex + 1}"
    val data = mapOf(
        field to mapOf(
            "smile" to mapOf("timestamps" to smiles, "count" to smiles.size),
            "badposture" to mapOf("timestamps" to bads, "count" to bads.size),
            "notfront" to mapOf("timestamps" to nots, "count" to nots.size)
        )
    )
    db.collection("interview_mediapipe")
        .document(sessionId)
        .set(data, SetOptions.merge())
        .addOnSuccessListener { Log.d("Mediapipe","저장 성공: $field") }
        .addOnFailureListener { e -> Log.e("Mediapipe","저장 실패", e) }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuestionScreen() {
    // 임시 NavController, sessionId, major, sub
    QuestionScreen(
        navController = rememberNavController(),
        sessionId = "test",
        major     = "전공",
        sub       = "세부전공"
    )
}
