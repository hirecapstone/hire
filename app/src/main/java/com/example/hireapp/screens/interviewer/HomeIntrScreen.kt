package com.example.hireapp.screens.interviewer

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.data.fetchPublicVideos
import com.example.hireapp.models.VideoItem
import com.example.hireapp.navigation.Screen
import com.example.hireapp.screens.VideoPlayer
import com.example.hireapp.screens.applicant.LikeSection
import com.example.hireapp.screens.applicant.VideoPlayerViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeIntrScreen(navController: NavController) {
    val playerVm: VideoPlayerViewModel = viewModel(
        factory = VideoPlayerViewModel.Factory(LocalContext.current)
    )
    val context = LocalContext.current
    var videoList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }


    DisposableEffect(Unit) {
        onDispose { playerVm.releaseAllPlayers() }
    }

    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser

        if(user != null) {
            try {
                val doc = Firebase.firestore.collection("users").document(user.uid).get().await()

                val categoryMap = doc.get("category") as? Map<*, *>
                val major = categoryMap?.get("major") as? String ?: "지정 없음"
                val sub = categoryMap?.get("sub") as? String ?: "지정 없음"

                Log.d("Inter-home", "${categoryMap}")
                Log.d("Inter-home", "major = ${major}, sub = ${sub}")

                videoList = fetchPublicVideos(
                    major = if (!major.isNullOrEmpty()) major else null,
                    sub = if (!sub.isNullOrEmpty()) listOf(sub) else null
                )

            } catch (e: Exception) {
                Log.e("Firestore", "유저 정보 가져오기 실패: ${e.message}")
            }
        } else {
            Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            navController.navigate(Screen.Login.route)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavigationIntr(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.hireapp.R.drawable.user),
                                contentDescription = "유저 프로필 이미지",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${video.userName}  (${video.major} / ${video.sub})",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(5f / 6f)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            VideoPlayer(
                                url = video.fileUrl!!,
                                playerVm = playerVm
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 좋아요
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "좋아요", fontSize = 14.sp)
                                LikeSection(videoId = video.id)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // 댓글
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    playerVm.releaseAllPlayers()
                                    navController.navigate("${Screen.VideoDetail.route}/${video.id}")
                                }
                            ) {
                                Text(text = "댓글", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "댓글",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = com.example.hireapp.R.drawable.title2),
                                contentDescription = "타이틀 이미지",
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = video.title,
                                modifier = Modifier.clickable {
                                    playerVm.releaseAllPlayers()
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
fun PreviewHomeIntrScreen() {
    HomeIntrScreen(navController = rememberNavController())
}
