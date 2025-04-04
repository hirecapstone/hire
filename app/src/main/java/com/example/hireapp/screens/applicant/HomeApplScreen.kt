package com.example.hireapp.screens.applicant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.Screen
import com.example.hireapp.screens.interviewer.VideoItem

@Composable
fun HomeApplScreen(navController: NavController) {
    val videoList = remember {
        listOf( // 더미데이터
            VideoItem("1", "IT 직무 면접", "이름0"),
            VideoItem("2", "디자인 직무 면접", "이름1"),
            VideoItem("3", "경영/사무 직무 면접", "이름2"),
            VideoItem("4", "생산/기술 직무 면접", "이름3"),
            VideoItem("5", "9급 공무원 면접", "이름4"),
            VideoItem("6", "소방 공무원 면접", "이름5"),
            VideoItem("7", "초등교사 면접", "이름6"),
            VideoItem("8", "강사 면접", "이름7"),
            VideoItem("9", "학부 입시 면접", "이름8"),
            VideoItem("10", "편입 면접", "이름9")
        )
    }

    Scaffold(
        bottomBar = { BottomNavigationAppl(navController) }
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

                        // 더미 UI - 실제 영상 썸네일/재생기로 교체 예정
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("영상 미리보기")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 좋아요 & 댓글 아이콘 (좋아요 기능은 아직 구현x)
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

data class VideoItem(val id: String, val title: String, val userName: String)
data class Comment(val user: String, val text: String)


@Preview(showBackground = true)
@Composable
fun PreviewHomeApplScreen() {
    HomeApplScreen(navController = rememberNavController())
}
