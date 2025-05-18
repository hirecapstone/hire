package com.example.hireapp.screens.applicant.interviewcapture

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.style.TextAlign

@Composable
fun FeedbackScreen(navController: NavController, sessionId: String) {
    val db = FirebaseFirestore.getInstance()
    var feedbackData by remember { mutableStateOf<List<String>?>(null) }
    var mediapipeData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingFeedback by remember { mutableStateOf(true) }
    var isLoadingMediapipe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) } // 로딩 진행 상태 (0.0 ~ 1.0)

    Log.d("FeedbackScreen", "FeedbackScreen 시작 - sessionId: $sessionId")

    // Firestore에서 데이터 가져오기
    LaunchedEffect(sessionId) {
        errorMessage = null

        // Mediapipe 데이터 가져오기
        launch {
            Log.d("MediapipeData", "Mediapipe 데이터를 가져오는 중입니다. sessionId: $sessionId")
            try {
                val snapshot = db.collection("interview").document(sessionId).get().await()
                if (snapshot.exists()) {
                    mediapipeData = snapshot.get("mediapipe") as? Map<String, Any>
                    Log.d("MediapipeData", "Mediapipe 데이터 가져오기 성공: $mediapipeData")
                } else {
                    Log.w("MediapipeData", "Mediapipe 데이터가 존재하지 않습니다. sessionId: $sessionId")
                    mediapipeData = null
                }
            } catch (e: Exception) {
                Log.e("MediapipeData", "Mediapipe 데이터 가져오기 실패: ${e.message}")
                errorMessage = "Mediapipe 데이터를 불러오는 중 오류가 발생했습니다."
            } finally {
                isLoadingMediapipe = false
                progress += 0.5f
                Log.d("MediapipeData", "Mediapipe 데이터 가져오기 완료. isLoadingMediapipe: $isLoadingMediapipe, 진행률: $progress")
            }
        }

        // 피드백 데이터 가져오기
        launch {
            Log.d("FeedbackData", "피드백 데이터를 가져오는 중입니다. sessionId: $sessionId")
            try {
                val document = db.collection("interview").document(sessionId).get().await()
                val feedbackRef = document.getDocumentReference("feedback")
                Log.d("FeedbackData", "피드백 참조: $feedbackRef")

                if (feedbackRef != null) {
                    val feedbackSnapshot = feedbackRef.get().await()
                    feedbackData = feedbackSnapshot?.get("feedbacks") as? List<String>
                    Log.d("FeedbackData", "피드백 데이터 가져오기 성공: $feedbackData")
                } else {
                    Log.e("FeedbackData", "피드백 참조가 null입니다. sessionId: $sessionId")
                    feedbackData = null
                }
            } catch (e: Exception) {
                Log.e("FeedbackData", "피드백 데이터 가져오기 실패: ${e.message}")
                errorMessage = "피드백 데이터를 불러오는 중 오류가 발생했습니다."
            } finally {
                isLoadingFeedback = false
                progress += 0.5f
                Log.d("FeedbackData", "피드백 데이터 가져오기 완료. isLoadingFeedback: $isLoadingFeedback, 진행률: $progress")
            }
        }
    }

    // 로딩 중 상태 처리
    if (isLoadingFeedback || isLoadingMediapipe) {
        Log.d("FeedbackScreen", "로딩 중... isLoadingFeedback: $isLoadingFeedback, isLoadingMediapipe: $isLoadingMediapipe, 진행률: $progress")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(progress = progress)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "피드백 데이터를 생성 중입니다... (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center // 텍스트 가운데 정렬
                )
            }
        }
        return
    }

    // 에러 처리
    if (errorMessage != null) {
        Log.e("FeedbackScreen", "오류 발생: $errorMessage")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMessage ?: "알 수 없는 오류")
        }
        return
    }

    // 피드백 데이터가 없을 경우 처리
    val feedbacks = feedbackData
    if (feedbacks.isNullOrEmpty() && mediapipeData == null) {
        Log.w("FeedbackScreen", "피드백 데이터와 Mediapipe 데이터가 모두 비어 있습니다.")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "피드백 데이터를 생성 중입니다. 잠시만 기다려주세요....(상당시간 소요가능)",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center // 텍스트 가운데 정렬
            )
        }
        return
    }

    // UI 구성
    Box(modifier = Modifier.fillMaxSize()) {
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

            if (feedbacks.isNullOrEmpty()) {
                Log.w("FeedbackScreen", "피드백 데이터가 비어 있습니다.")
                Text("영상이 너무 짧아 피드백이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Log.d("FeedbackScreen", "피드백 데이터를 렌더링합니다: $feedbacks")
                feedbacks.forEachIndexed { index, feedback ->
                    Text("${index + 1}. $feedback", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mediapipe 결과 시각화
            Text("Mediapipe 분석 결과", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            val smileCount = (mediapipeData?.get("video1") as? Map<*, *>)?.get("smile")?.let {
                (it as? Map<*, *>)?.get("count") as? Int ?: 0
            } ?: 0

            val postureCount = (mediapipeData?.get("video1") as? Map<*, *>)?.get("badposture")?.let {
                (it as? Map<*, *>)?.get("count") as? Int ?: 0
            } ?: 0

            val gazeCount = (mediapipeData?.get("video1") as? Map<*, *>)?.get("notfront")?.let {
                (it as? Map<*, *>)?.get("count") as? Int ?: 0
            } ?: 0

            Log.d("FeedbackScreen", "Mediapipe 데이터를 렌더링합니다: smileCount=$smileCount, postureCount=$postureCount, gazeCount=$gazeCount")

            Text("미소: ${smileCount}회", style = MaterialTheme.typography.bodyMedium)
            Text("구부정 자세: ${postureCount}회", style = MaterialTheme.typography.bodyMedium)
            Text("정면 미응시: ${gazeCount}회", style = MaterialTheme.typography.bodyMedium)
        }

        // 홈 화면으로 돌아가기 버튼
        Button(
            onClick = {
                Log.d("FeedbackScreen", "홈 화면으로 이동합니다.")
                navController.navigate(Screen.HomeAppl.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("홈 화면으로 돌아가기")
        }
    }
}