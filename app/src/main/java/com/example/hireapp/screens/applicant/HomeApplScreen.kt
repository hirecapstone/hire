package com.example.hireapp.screens.applicant

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.models.VideoItem
import com.example.hireapp.navigation.Screen
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun HomeApplScreen(navController: NavController) {
    var videoList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    // Firestore에서 모든 interview 문서 불러오기
    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        val resultList = mutableListOf<VideoItem>()

        try {
            val interviewDocs = db.collection("interview")
                .whereEqualTo("public", true)
                .get()
                .await()

            Log.d("FirestoreDebug", "총 interview 문서 수: ${interviewDocs.size()}")

            interviewDocs.forEach { doc ->
                val title = doc.getString("title") ?: run {
                    Log.w("FirestoreDebug", "문서 ${doc.id} → title 없음")
                    return@forEach
                }

                val userField = doc.get("user")
                val userName = when (userField) {
                    is DocumentReference -> {
                        try {
                            val snapshot = userField.get().await()
                            snapshot.getString("name") ?: "이름 없음"
                        } catch (e: Exception) {
                            Log.e("FirestoreDebug", "문서 ${doc.id} → user 문서 불러오기 실패: ${e.message}")
                            "이름 조회 실패"
                        }
                    }
                    is String -> userField
                    else -> "알 수 없음"
                }

                val videos = doc.get("videos") as? List<Map<String, Any>>
                val fileUrl = videos?.firstOrNull()?.get("fileUrl") as? String

                Log.d("FirestoreDebug", "문서 ${doc.id} → 사용자 이름: $userName, fileUrl: $fileUrl")

                resultList.add(
                    VideoItem(
                        id = doc.id,
                        title = title,
                        userName = userName,
                        fileUrl = fileUrl
                    )
                )
            }

            videoList = resultList
            Log.d("FirestoreDebug", "최종 videoList 크기: ${videoList.size}")

        } catch (e: Exception) {
            Log.e("FirestoreDebug", "인터뷰 문서 로딩 실패: ${e.message}")
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationAppl(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (videoList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("불러올 영상이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    items(videoList) { video ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            // 유저 이름
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = video.userName, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 영상 미리보기
                            if (video.fileUrl != null) {
                                VideoPlayer(url = video.fileUrl)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("영상 없음")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 좋아요 & 댓글 아이콘
                            Row {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "좋아요")
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = "댓글",
                                    modifier = Modifier.clickable {
                                        navController.navigate("${Screen.VideoDetail.route}/${video.id}")
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 영상제목
                            Text(
                                text = video.title,
                                modifier = Modifier.clickable {
                                    navController.navigate("${Screen.VideoDetail.route}/${video.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            exoPlayer.playWhenReady = true // 터치하면 재생 시작
                        }
                    )
                }
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeApplScreen() {
    HomeApplScreen(navController = rememberNavController())
}
