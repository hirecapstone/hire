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

@Composable
fun FeedbackScreen(navController: NavController, sessionId: String) {
    val db = FirebaseFirestore.getInstance()
    var feedbackData by remember { mutableStateOf<List<String>?>(null) }
    var mediapipeData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingFeedback by remember { mutableStateOf(true) } // 피드백 데이터 로드 상태
    var isLoadingMediapipe by remember { mutableStateOf(true) } // Mediapipe 데이터 로드 상태
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Firestore에서 데이터 가져오기
    LaunchedEffect(sessionId) {
        errorMessage = null

        // Mediapipe 데이터 가져오기
        db.collection("interview").document(sessionId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreError", "Mediapipe 데이터 로드 실패: ${e.message}")
                    errorMessage = "Mediapipe 데이터를 불러오는 중 오류가 발생했습니다."
                    isLoadingMediapipe = false
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    mediapipeData = snapshot.get("mediapipe") as? Map<String, Any>
                    isLoadingMediapipe = false
                } else {
                    mediapipeData = null
                    isLoadingMediapipe = false
                }
            }

        // 피드백 데이터 가져오기
        try {
            val document = db.collection("interview").document(sessionId).get().await()
            val feedbackRef = document.getDocumentReference("feedback")

            if (feedbackRef != null) {
                feedbackRef.get().addOnSuccessListener { feedbackSnapshot ->
                    feedbackData = feedbackSnapshot?.get("feedbacks") as? List<String>
                    isLoadingFeedback = false
                }.addOnFailureListener {
                    Log.e("FirestoreError", "피드백 데이터 로드 실패: ${it.message}")
                    errorMessage = "피드백 데이터를 불러오는 중 오류가 발생했습니다."
                    isLoadingFeedback = false
                }
            } else {
                feedbackData = null
                isLoadingFeedback = false
            }
        } catch (e: Exception) {
            Log.e("FirestoreError", "피드백 데이터 로드 실패: ${e.message}")
            errorMessage = "피드백 데이터를 불러오는 중 오류가 발생했습니다."
            isLoadingFeedback = false
        }
    }

    // 로딩 중 상태 처리
    if (isLoadingFeedback || isLoadingMediapipe) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("피드백을 생성 중입니다...", style = MaterialTheme.typography.bodyMedium)
            }
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

    // 피드백 데이터가 없을 경우 처리
    val feedbacks = feedbackData
    if (feedbacks.isNullOrEmpty() && mediapipeData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "피드백 데이터를 생성 중입니다. 잠시만 기다려주세요....(상당시간 소요가능)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center) // 가운데 정렬
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
                Text("영상이 너무 짧아 피드백이 없습니다.", style = MaterialTheme.typography.bodyMedium)
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

        // 홈 화면으로 돌아가기 버튼
        Button(
            onClick = { navController.navigate(Screen.HomeAppl.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("홈 화면으로 돌아가기")
        }
    }
}