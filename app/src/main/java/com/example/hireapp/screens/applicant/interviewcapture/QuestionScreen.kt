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
import androidx.core.app.ActivityCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


@Composable
fun QuestionScreen(navController: NavController, sessionId: String, major: String, sub: String) {
    Log.d("SessionIdCheck", "전달된 sessionId: $sessionId")
    Log.d("MajorSubCheck", "전달된 Major: $major, Sub: $sub")
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
                    if (!snapshots.isEmpty) {
                        aiQuestions = snapshots.documents.flatMap { document ->
                            val questions = document.get("questions") as? List<String>
                            questions ?: emptyList()
                        }
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("질문을 생성 중입니다.", style = MaterialTheme.typography.bodyMedium)
            }
        }
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

    // 질문 단계별 타이머 및 녹화
    LaunchedEffect(phase, currentIndex) {
        timeLeft = if (phase == "prepare") 30 else 60

        if (phase == "answer" && hasPermission) {
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
                },
                permissionLauncher
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
                    showTitleDialog = true
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
                    navController.navigate(Screen.HomeAppl.route)
                    showTitleDialog = false
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                Button(onClick = {
                    navController.navigate(Screen.HomeAppl.route)
                    showTitleDialog = false

                }) {
                    Text("취소")
                }
            }
        )
    }
}

// 권한 요청을 처리하는 함수
private fun requestPermissions(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
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
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
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

            // Recorder 설정
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD)) // 영상 품질 설정
                .build()

            // VideoCapture 객체 생성 및 오디오 캡처 활성화
            val videoCapture = VideoCapture.withOutput(recorder)

            // 오디오 캡처 활성화
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
    val VIDEO_PATH = "videos/${sessionId}"

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val videoUrls = mutableListOf<String>()

            // 각 파일을 Firebase Storage에 업로드하고 URL을 저장
            recordedFiles.forEach { file ->
                try {
                    val fileUri = Uri.fromFile(file)
                    val videoRef = storageRef.child("${VIDEO_PATH}/${file.name}")
                    Log.d("UploadFile", "파일 업로드 시작: ${file.name}")
                    // 파일 업로드
                    videoRef.putFile(fileUri).await()
                    Log.d("UploadFile", "파일 업로드 성공: ${file.name}")
                    try {
                        val downloadUrl = videoRef.downloadUrl.await()
                        Log.d("DownloadUrl", "다운로드 URL 가져오기 성공: ${downloadUrl.toString()}")
                        videoUrls.add(downloadUrl.toString()) // URL 추가
                    } catch (e: Exception) {
                        Log.e("DownloadError", "다운로드 URL 가져오기 실패: ${e.message}")
                    }

                } catch (e: Exception) {
                    Log.e("UploadError", "파일 업로드 실패: ${e.message}, 파일: ${file.name}")
                }
            }

            val currentUser = auth.currentUser
            val userName = currentUser?.uid?.let { userId ->
                // uid로 Firestore에서 사용자 이름을 가져옵니다.
                db.collection("users").document(userId)
                    .get()
                    .await()
                    .getString("name") ?: "Unknown User" // "name" 필드를 가져옵니다.
            } ?: "Unknown User"

            // Firestore에서 sessionId와 일치하는 interview_questions 문서 가져오기
            val latestQuestionRef = db.collection("interview_questions")
                .document(sessionId) // sessionId로 문서를 찾음
                .get()
                .await()

            val questions = latestQuestionRef.get("questions") as? List<String> ?: listOf()

            // Firestore에 저장할 데이터 생성
            val videoData = hashMapOf(
                "category" to hashMapOf(
                    "major" to major,
                    "sub" to sub
                ),
                "question" to questions,
                "title" to videoTitle,
                "uploadTime" to FieldValue.serverTimestamp(),
                "user" to userName,
                "videos" to videoUrls.map { mapOf("fileUrl" to it) },
                "public" to true
            )




            val dbpath = "interview/${sessionId.replace("/", "")}"

            // Firestore에 데이터 저장
            Log.d("Upload", "Firestore에 데이터 저장 시작: $dbpath")
            db.collection("interview")
                .document(sessionId) // sessionId 그대로 사용
                .set(videoData)
                .addOnSuccessListener {
                    Log.d("Upload", "Firestore에 데이터 저장 성공: $dbpath")
                }
                .addOnFailureListener { e ->
                    Log.e("Upload", "Firestore에 데이터 저장 실패: ${e.message}, 경로: $dbpath")
                }





//            withContext(Dispatchers.Main) {
//                // 로컬 파일 삭제 및 마이페이지로 이동
//                recordedFiles.forEach { it.delete() }
//                navController.navigate(Screen.MyPageAppl.route) {
//                    popUpTo(Screen.MyPageAppl.route) { inclusive = true }
//                }
//            }
        } catch (e: Exception) {
            Log.e("UploadError", "업로드 실패: ${e.message} 문서이름은 ${sessionId}",)
        }
    }
}


