package com.example.hireapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.hireapp.screens.applicant.Comment
import com.example.hireapp.screens.interviewer.VideoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// 더미데이터
val sampleVideos = listOf(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(videoId: String, navController: NavController) {
    val video = sampleVideos.find { it.id == videoId }
    if (video == null) {
        Text("해당 영상을 찾을 수 없습니다.")
        return
    }

    // 더미데이터
    val comments = remember {
        mutableStateListOf(
            Comment("user1", "피드백1"),
            Comment("user2", "피드백2"),
            Comment("user3", "피드백3"),
            Comment("user4", "피드백4"),
            Comment("user5", "피드백5"),
        )
    }
    var inputText by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var currentUserName by remember { mutableStateOf("me") }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    currentUserName = doc.getString("name") ?: "익명"
                }
                .addOnFailureListener {
                    Toast.makeText(context, "유저 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 영상 + 댓글 목록 (스크롤 영역)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Text(
                        text = video.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(text = video.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box( // 더미 UI - 실제 영상 썸네일/재생기로 교체 예정
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.LightGray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("영상 미리보기")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(comments) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(text = it.user, fontWeight = FontWeight.Bold)
                        Text(text = it.text)
                    }
                }
            }

            // 댓글 입력창 (고정)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("댓글을 입력하세요") }
                )
                IconButton(onClick = {
                    if (inputText.isNotBlank()) {
                        comments.add(Comment(currentUserName, inputText))
                        inputText = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "댓글 전송")
                }
            }
        }
    }
}


