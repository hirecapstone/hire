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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ResultScreen(
    navController: NavController,
    sessionId: String
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // 사용자 역할 및 별점 상태
    var userRole by remember { mutableStateOf<String?>(null) }
    var roleLoading by remember { mutableStateOf(true) }
    var userRating by remember { mutableStateOf<Int?>(null) }
    var averageRating by remember { mutableStateOf(0.0) }

    var isLoading by remember { mutableStateOf(true) }
    var mediapipeData by remember { mutableStateOf<Map<String, Map<String, Map<String, Any>>>>(emptyMap()) }
    var feedbackData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var questionsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var answersList by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            navController.navigate("login")
        } else {
            try {
                val doc = db.collection("users").document(uid).get().await()
                userRole = doc.getString("role")
            } catch (e: Exception) {
                userRole = null
            }
        }
        roleLoading = false
    }

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

        val qRef = db.collection("interview_questions").document(sessionId)
        while (!qRef.get().await().exists()) delay(1000L)
        val qSnap = qRef.get().await()
        questionsList = (qSnap.get("questions") as? List<*>)
            ?.mapNotNull { it as? String } ?: emptyList()

        val aRef = db.collection("interview_answers").document(sessionId)
        while (!aRef.get().await().exists()) delay(1000L)
        val aSnap = aRef.get().await()
        answersList = (aSnap.get("text") as? List<*>)
            ?.mapNotNull { it as? String } ?: emptyList()

        // 별점 불러오기
        val ratingsCol = interviewRef.collection("ratings")
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val myRating = ratingsCol.document(uid).get().await().getLong("rating")?.toInt()
            userRating = myRating
        }
        val allRatings = ratingsCol.get().await().documents
            .mapNotNull { it.getLong("rating")?.toInt() }
        averageRating = if (allRatings.isNotEmpty()) allRatings.average() else 0.0

        isLoading = false
    }
    fun saveRating(rating: Int) {
        scope.launch {
            val ratingsCol = db.collection("interview").document(sessionId).collection("ratings")
            auth.currentUser?.uid?.let { uid ->
                ratingsCol.document(uid)
                    .set(mapOf("rating" to rating))
                    .await()
                // 평균 재계산
                val all = ratingsCol.get().await().documents
                    .mapNotNull { it.getLong("rating")?.toInt() }
                averageRating = if (all.isNotEmpty()) all.average() else 0.0
                userRating = rating
            }
        }
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = """
                            정확한 피드백을 위해 영상을 분석 중입니다.
                            1~2분 정도 소요될 수 있어요.
                            """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
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
                val question = questionsList.getOrNull(idx) ?: ""
                val answer = answersList.getOrNull(idx) ?: ""
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${idx + 1}번 영상 결과", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = com.example.hireapp.R.drawable.q),
                                contentDescription = "질문 아이콘",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = " $question",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = com.example.hireapp.R.drawable.a),
                                contentDescription = "답변 아이콘",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$answer",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(Modifier.height(12.dp))
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "총점: $tot/5",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row {
                                for (i in 1..5) {
                                    val starResId = if (i <= tot) {
                                        com.example.hireapp.R.drawable.star // 채워진 별
                                    } else {
                                        com.example.hireapp.R.drawable.emptystar // 빈 별
                                    }

                                    Image(
                                        painter = painterResource(id = starResId),
                                        contentDescription = "별점 이미지",
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 2.dp)
                                    )
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(8.dp))
                        // 비언어적 피드백
                        Text(text = "▶ 비언어적 피드백", style = MaterialTheme.typography.titleSmall)
                        Text(text = "- 자세: $postureFb")
                        Text(text = "- 시선: $gazeFb")
                        Text(text = "- 표정: $expressionFb")
                    }
                }
                // 별점 UI
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 평균 별점: 별점 주고 나서만 표시
                    if (userRating != null) {
                        Text(
                            text = "전체 별점 평균: ${"%.1f".format(averageRating)} / 5",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Row {
                            val filled = averageRating.toInt()
                            for (i in 1..5) {
                                val res = if (i <= filled) com.example.hireapp.R.drawable.star else com.example.hireapp.R.drawable.emptystar
                                Image(
                                    painter = painterResource(id = res),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "별점을 남기면 평균을 확인할 수 있어요.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("나의 평가", style = MaterialTheme.typography.titleSmall)
                    Row {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { if (userRating != i) saveRating(i) },
                                enabled = (userRating != i)
                            ) {
                                val res = if (userRating != null && i <= userRating!!) com.example.hireapp.R.drawable.star else com.example.hireapp.R.drawable.emptystar
                                Image(
                                    painter = painterResource(id = res),
                                    contentDescription = "${i}점",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
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
