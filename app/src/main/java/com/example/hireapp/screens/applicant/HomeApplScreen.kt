package com.example.hireapp.screens.applicant

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.models.VideoItem
import com.example.hireapp.navigation.Screen
import com.example.hireapp.screens.applicant.VideoPlayerViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(
    url: String,
    viewModel: VideoPlayerViewModel = viewModel(
        factory = VideoPlayerViewModel.Factory(LocalContext.current)
    )
) {
    // (수정) ViewModel 에서 ExoPlayer 관리
    val exoPlayer: ExoPlayer = viewModel.getPlayer(url)

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .pointerInput(Unit) {
                detectTapGestures { exoPlayer.playWhenReady = true }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeApplScreen(navController: NavController) {
    var videoList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        val resultList = mutableListOf<VideoItem>()

        try {
            val interviewDocs = db.collection("interview")
                .whereEqualTo("public", true)
                .get()
                .await()

            Log.d("FirestoreDebug", "총 interview 문서 수: ${interviewDocs.size()}")

            for (doc in interviewDocs) {
                val title = doc.getString("title") ?: continue

                val userField = doc.get("user")
                val userName = when (userField) {
                    is DocumentReference -> try {
                        val snapshot = userField.get().await()
                        snapshot.getString("name")
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "문서 ${doc.id} → user 문서 불러오기 실패: ${e.message}")
                        null
                    }
                    is String -> try {
                        val snapshot = db.collection("users").document(userField).get().await()
                        snapshot.getString("name")
                    } catch (e: Exception) {
                        Log.e("FirestoreDebug", "문서 ${doc.id} → UUID로 유저 조회 실패: ${e.message}")
                        null
                    }
                    else -> null
                }

                if (userName.isNullOrEmpty()) continue

                val videos = doc.get("videos") as? List<Map<String, Any>>
                val fileUrl = videos?.firstOrNull()?.get("fileUrl") as? String
                if (fileUrl.isNullOrEmpty()) continue

                resultList.add(VideoItem(id = doc.id, title = title, userName = userName, fileUrl = fileUrl))
            }

            videoList = resultList
            Log.d("FirestoreDebug", "최종 videoList 크기: ${videoList.size}")

        } catch (e: Exception) {
            Log.e("FirestoreDebug", "인터뷰 문서 로딩 실패: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.hire),
                            contentDescription = "앱 로고",
                            modifier = Modifier
                                .size(60.dp)
                                .padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("하이어", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { BottomNavigationAppl(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (videoList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.no),
                            contentDescription = "없음 이미지",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("불러올 영상이 없습니다.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    items(
                        items = videoList,
                        key = { it.id }  // (추가) key 지정으로 재사용 보장
                    ) { video ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                VideoPlayer(url = video.fileUrl!!)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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

@Preview(showBackground = true)
@Composable
fun PreviewHomeApplScreen() {
    HomeApplScreen(navController = rememberNavController())
}
