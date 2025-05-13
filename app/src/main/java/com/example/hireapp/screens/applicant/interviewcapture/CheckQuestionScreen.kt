@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hireapp.screens.applicant.interviewcapture

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
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
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import kotlin.math.abs
import kotlin.math.atan2
import java.util.ArrayList
import java.io.File

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CheckQuestionScreen(
    navController: NavController,
    sessionId: String,
    major: String,
    sub: String,
    questions: List<String>  // questions를 받도록 추가
) {
    // 질문, 인덱스, 타이머
    //5/13 capture2screen
//    val questions    = navController.previousBackStackEntry
//        ?.savedStateHandle
//        ?.get<ArrayList<String>>("questions") ?: arrayListOf()
    var currentIndex by remember { mutableStateOf(0) }
    var isReady      by remember { mutableStateOf(true) }
    var timer        by remember { mutableStateOf(30) }

    // 녹화 관련 상태
    val sessionId        = remember { "session_${System.currentTimeMillis()}" }
    val recordedFiles    = remember { mutableStateListOf<File>() }
    val context = LocalContext.current
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var showTitleDialog  by remember { mutableStateOf(false) }
    var videoTitle       by remember { mutableStateOf("") }

    // ML Kit 클라이언트
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

    // 상태 변수
    var expression by remember { mutableStateOf("무표정") }
    var posture   by remember { mutableStateOf("정자세") }
    var gaze      by remember { mutableStateOf("정면") }

    // Mediapipe 결과 저장용
    val db                    = FirebaseFirestore.getInstance()
    val smileTimestamps      = remember { mutableStateListOf<Int>() }
    val badPostureTimestamps = remember { mutableStateListOf<Int>() }
    val notFrontTimestamps   = remember { mutableStateListOf<Int>() }
    var answerElapsed        by remember { mutableStateOf(0) }

    // 카메라 준비
    val lifecycleOwner = LocalLifecycleOwner.current
    val isInPreview    = LocalInspectionMode.current
    val previewView    = remember { PreviewView(context) }
    val cameraFuture   = remember { ProcessCameraProvider.getInstance(context) }
    val scope          = rememberCoroutineScope()
    val recorder     = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    // 권한
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    LaunchedEffect(sessionId, questions) {
        if (questions.isNotEmpty()) {
            db.collection("interview_questions")
                .document(sessionId)
                .set(mapOf("questions" to questions))
                .addOnSuccessListener {
                    Log.d("Firestore", "질문 저장 성공: $sessionId")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "질문 저장 실패", e)
                }
        }
    }

    // 타이머
    LaunchedEffect(currentIndex, isReady) {
        if(!isReady){
            answerElapsed = 0
            smileTimestamps.clear()
            badPostureTimestamps.clear()
            notFrontTimestamps.clear()
            val file = File(context.cacheDir, "${sessionId}_q${currentIndex+1}.mp4")
            recordedFiles.add(file)
            currentRecording = videoCapture.output
                .prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { /* no-op */ }
        } else{
            currentRecording?.stop()
            currentRecording = null
        }
        timer = if (isReady) 30 else 60
        while (timer > 0) {
            delay(1_000L)
            timer--
            if (!isReady) {
                // 1초마다 분석 결과 저장
                answerElapsed++
                if (expression == "웃음")      smileTimestamps.add(answerElapsed)
                if (posture    == "구부정")    badPostureTimestamps.add(answerElapsed)
                if (gaze       == "정면아님") notFrontTimestamps.add(answerElapsed)

                saveMediapipeResult(
                    db, sessionId, currentIndex,
                    smileTimestamps, badPostureTimestamps, notFrontTimestamps
                )
            }
        }
        if (!isReady) {
            // 답변 종료 후 녹화 중지 및 최종 저장
            currentRecording?.stop()
            saveMediapipeResult(
                db, sessionId, currentIndex,
                smileTimestamps, badPostureTimestamps, notFrontTimestamps
            )
        }

        if (isReady) {
            isReady = false
        } else {
            if (currentIndex < questions.size - 1) {
                currentIndex++; isReady = true
            } else {
                showTitleDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
        if (!hasAudioPermission) audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

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
                preview, analysis,videoCapture
            )
        }, ContextCompat.getMainExecutor(context))
    }

    // UI
    Scaffold(
        bottomBar = {
            Button(
                onClick = {
                    if(!isReady){
                        currentRecording?.stop()
                        currentRecording = null
                    }
                    if (isReady) isReady = false
                    else if (currentIndex < questions.size - 1) {
                        currentIndex++; isReady = true
                    } else {
                        showTitleDialog = true
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
                    .fillMaxHeight(0.7f)
            ) {
                when {
                    isInPreview -> Box(
                        Modifier
                            .fillMaxWidth()
                    ) { }

                    !hasPermission -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("카메라 권한 필요")
                    }

                    else -> AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .fillMaxWidth()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .fillMaxHeight(0.3f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        "질문 ${currentIndex + 1}/${questions.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.example.hireapp.R.drawable.write),
                        contentDescription = "입력한 텍스트 이미지",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp)) // 수정: height → width
                    Text(
                        questions.getOrNull(currentIndex) ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.example.hireapp.R.drawable.time),
                        contentDescription = "남은 시간 이미지",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp)) // 수정: height → width
                    Text(
                        if (isReady) "준비시간: $timer 초" else "남은 시간: $timer 초",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        }
        if (showTitleDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("영상 제목 작성") },
                text = {
                    Column {
                        Text("영상의 제목을 작성해주세요:")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = videoTitle,
                            onValueChange = { videoTitle = it },
                            placeholder = { Text("제목 입력") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        uploadResults(
                            db               = db,
                            auth             = FirebaseAuth.getInstance(),
                            sessionId        = sessionId,
                            questions        = questions,
                            videoTitle       = videoTitle,
                            recordedFiles    = recordedFiles,
                            navController    = navController
                        )
                        showTitleDialog = false
                        navController.navigate("${Screen.ResultScreen.route}/$sessionId")
                    }) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        recordedFiles.clear()
                        showTitleDialog = false
                        navController.navigate(Screen.HomeAppl.route)
                    }) {
                        Text("취소")
                    }
                }
            )
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

private fun uploadResults(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    sessionId: String,
    questions: List<String>,
    videoTitle: String,
    recordedFiles: List<File>,
    navController: NavController
) {
    val storageRef = FirebaseStorage.getInstance().reference
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 1) 동영상 업로드
            val videoUrls = mutableListOf<String>()
            recordedFiles.forEachIndexed { idx, file ->
                val uri      = Uri.fromFile(file)
                val videoRef = storageRef.child("videos/$sessionId/${sessionId}_q${idx+1}.mp4")
                videoRef.putFile(uri).await()
                videoUrls += videoRef.downloadUrl.await().toString()
            }

            // 2) mediapipe 데이터 읽기
            val mediapipeData = db.collection("interview_mediapipe")
                .document(sessionId)
                .get()
                .await()
                .data
                ?: emptyMap<String, Any>()

            // 3) feedback 참조 저장
            val feedbackRef = db.collection("interview_feedback").document(sessionId)

            val categoryData = mapOf(
                "major" to "지정 없음",
                "sub" to "지정 없음"
            )

            // 4) 인터뷰 문서 저장
            val videoData = mapOf(
                "question" to questions,
                "title" to videoTitle,
                "category" to categoryData,
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

            withContext(Dispatchers.Main) {
                navController.navigate("${Screen.ResultScreen.route}/$sessionId")
            }
        } catch (e: Exception) {
            Log.e("UploadError", "업로드 실패", e)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCheckQuestionScreen() {
    CheckQuestionScreen(
        navController = rememberNavController(),
        sessionId = "dummySessionId",
        major = "컴퓨터공학",
        sub = "AI",
        questions = listOf("What is AI?", "Explain machine learning.", "What is your favorite programming language?") // 임의의 질문 리스트 추가
    )
}


