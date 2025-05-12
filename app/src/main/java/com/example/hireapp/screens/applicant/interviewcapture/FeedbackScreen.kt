package com.example.hireapp.screens.applicant.interviewcapture

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun FeedbackScreen(navController: NavController, sessionId: String) {
    val db = FirebaseFirestore.getInstance()
    var feedbackData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var mediapipeData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var videoTitle by remember { mutableStateOf("") }

    // Compose에서 context를 가져오기
    val context = LocalContext.current

    // Firestore에서 데이터 가져오기
    LaunchedEffect(sessionId) {
        isLoading = true
        errorMessage = null
        try {
            // interview 컬렉션에서 데이터 가져오기
            val document = db.collection("interview").document(sessionId).get().await()
            feedbackData = document.get("feedback") as? Map<String, Any>
            mediapipeData = document.get("mediapipe") as? Map<String, Any>
        } catch (e: Exception) {
            Log.e("FirestoreError", "데이터 로드 실패: ${e.message}")
            errorMessage = "데이터를 불러오는 중 오류가 발생했습니다."
        } finally {
            isLoading = false
        }
    }

    // 로딩 중 상태 처리
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("피드백을 생성 중입니다...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    // 추가: 데이터가 null일 경우 메시지 표시
    if (feedbackData == null && mediapipeData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("피드백 데이터를 가져오지 못했습니다.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // 에러 처리
    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMessage ?: "알 수 없는 오류")
        }
        return
    }

    // 제목 작성 다이얼로그
    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
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
                    if (videoTitle.isBlank()) {
                        // Compose 환경에서 Toast 메시지를 표시하기 위해 LocalContext를 사용
                        Toast.makeText(context, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    saveTitleAndNavigate(navController, sessionId, videoTitle)
                    showTitleDialog = false
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                Button(onClick = { showTitleDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // UI 구성
    Box(modifier = Modifier.fillMaxSize()) {
        // 피드백 내용
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // 피드백 텍스트 표시
            Text("피드백 결과", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            val feedbacks = feedbackData?.get("feedbacks") as? List<*>
            if (feedbacks.isNullOrEmpty()) {
                Text("피드백이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                feedbacks.forEachIndexed { index, feedback ->
                    Text("${index + 1}. $feedback", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mediapipe 결과 시각화
            Text("Mediapipe 분석 결과", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            val smileCount = (mediapipeData?.get("smile") as? Map<*, *>)?.get("count") as? Int ?: 0
            val postureCount = (mediapipeData?.get("badposture") as? Map<*, *>)?.get("count") as? Int ?: 0
            val gazeCount = (mediapipeData?.get("notfront") as? Map<*, *>)?.get("count") as? Int ?: 0

            Text("미소: ${smileCount}회", style = MaterialTheme.typography.bodyMedium)
            Text("구부정 자세: ${postureCount}회", style = MaterialTheme.typography.bodyMedium)
            Text("정면 미응시: ${gazeCount}회", style = MaterialTheme.typography.bodyMedium)
        }

        // '다음' 버튼
        Button(
            onClick = { showTitleDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("다음")
        }
    }
}

// 제목 저장 함수
private fun saveTitleAndNavigate(navController: NavController, sessionId: String, videoTitle: String) {
    val db = FirebaseFirestore.getInstance()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            db.collection("interview")
                .document(sessionId)
                .update("title", videoTitle)
                .await()
            withContext(Dispatchers.Main) {
                navController.navigate("home")
            }
        } catch (e: Exception) {
            Log.e("SaveTitleError", "제목 저장 실패: ${e.message}")
        }
    }
}