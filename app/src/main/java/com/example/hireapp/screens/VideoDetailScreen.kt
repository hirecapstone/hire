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
import com.example.hireapp.models.Comment
import com.example.hireapp.models.VideoItem
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(videoId: String, navController: NavController) {
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var video by remember { mutableStateOf<VideoItem?>(null) }
    var comments = remember { mutableStateListOf<Comment>() }
    var inputText by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("me") }

    // 유저 정보 가져오기
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

    // 영상 정보 불러오기
    LaunchedEffect(videoId) {
        try {
            val doc = db.collection("interview").document(videoId).get().await()
            val title = doc.getString("title") ?: "제목 없음"
            val userField = doc.get("user")
            val userName = when (userField) {
                is DocumentReference -> {
                    try {
                        val snapshot = userField.get().await()
                        snapshot.getString("name") ?: "이름 없음"
                    } catch (e: Exception) {
                        "이름 조회 실패"
                    }
                }
                is String -> userField
                else -> "알 수 없음"
            }
            video = VideoItem(id = videoId, title = title, userName = userName)
        } catch (e: Exception) {
            Toast.makeText(context, "영상 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
            if (video != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Text(
                            text = video!!.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(text = video!!.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
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
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
