@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun ResultScreen(
    navController: NavController,
    sessionId: String
) {
    val db = FirebaseFirestore.getInstance()
    var isLoading by remember { mutableStateOf(true) }
    var mediapipeData by remember { mutableStateOf<Map<String, Map<String, Map<String, Any>>>>(emptyMap()) }
    var feedbackData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    LaunchedEffect(sessionId) {
        val interviewRef = db.collection("interview").document(sessionId)
        while (!interviewRef.get().await().exists()) delay(1000L)
        val interviewDoc = interviewRef.get().await()
        mediapipeData = interviewDoc.get("mediapipe")
                as? Map<String, Map<String, Map<String, Any>>> ?: emptyMap()
        val rawFb = interviewDoc.get("feedback")
        val fbRef: DocumentReference? = when (rawFb) {
            is DocumentReference -> rawFb
            is String -> db.document(rawFb)
            else -> null
        }
        fbRef?.let { ref -> while (!ref.get().await().exists()) delay(1000L) }
        feedbackData = fbRef?.get()?.await()?.data ?: emptyMap()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.result),
                            contentDescription = "결과 아이콘",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("인터뷰 결과")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate(Screen.HomeAppl.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent) // 배경색 제거
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = com.example.hireapp.R.drawable.home),
                        contentDescription = "홈 아이콘",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("홈으로 돌아가기", color = Color.Black)
                }
            }
        }

    )
    { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val videoKeys = mediapipeData.keys.sorted()
            itemsIndexed(videoKeys) { idx, key ->
                val mp = mediapipeData[key] ?: emptyMap()

                // 비언어적 metrics
                val badCount = (mp["badposture"]?.get("count") as? Number)?.toInt() ?: 0
                val notFrontCount = (mp["notfront"]?.get("count") as? Number)?.toInt() ?: 0
                val smileCount = (mp["smile"]?.get("count") as? Number)?.toInt() ?: 0
                val postureFb = if (badCount > 3) "구부정한 자세가 많았습니다. 좀 더 정자세를 유지해주세요." else "자세가 안정적이었습니다. 잘 하셨습니다."
                val gazeFb = if (notFrontCount > 3) "시선이 많이 벗어났습니다. 정면을 좀 더 응시해주세요." else "시선을 잘 유지하였습니다."
                val expressionFb = if (smileCount > 5) "웃는 표정을 잘 유지하였습니다." else "표정이 딱딱하게 느껴졌습니다. 좀 더 자연스럽게 미소 지어보세요."

                // 언어적 feedbackData
                val fbList = (feedbackData["feedbacks"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val scores = (feedbackData["scores_details"] as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()
                val speedArr = (feedbackData["speed"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val totalArr = (feedbackData["total_scores"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
                val spd = speedArr.getOrNull(idx)
                val speedFb = when (spd) {
                    "slow" -> "말이 느렸습니다. 조금 더 또렷하게 말해보세요."
                    "normal" -> "말 속도가 적절했습니다."
                    "fast" -> "말이 너무 빨랐습니다. 조금 천천히 말하는 게 좋겠습니다."
                    else -> ""
                }
                val criteriaMap = mapOf(
                    "clarity" to "명료성",
                    "length" to "분량",
                    "completeness" to "충실성",
                    "logic" to "논리성",
                    "relevance" to "적절성"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${idx + 1}번 영상 결과", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // 언어적 피드백
                        Text(text = "▶ 언어적 피드백", style = MaterialTheme.typography.titleSmall)
                        Text(text = "AI 피드백: ${fbList.getOrNull(idx) ?: "피드백 없음"}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "평가 항목:")
                        scores.getOrNull(idx)?.let { critMap ->
                            for ((k, v) in critMap) {
                                val label = criteriaMap[k] ?: k
                                Text(text = "- $label: $v")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "말 빠르기: $speedFb")
                        val tot = totalArr.getOrNull(idx) ?: 0
                        Text(text = "총점: $tot/5")

                        Spacer(modifier = Modifier.height(8.dp))
                        // 비언어적 피드백
                        Text(text = "▶ 비언어적 피드백", style = MaterialTheme.typography.titleSmall)
                        Text(text = "- 자세: $postureFb")
                        Text(text = "- 시선: $gazeFb")
                        Text(text = "- 표정: $expressionFb")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewResultScreen() {
    Text("ResultScreen Preview")
}
