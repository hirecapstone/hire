package com.example.hireapp.screens.applicant

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.InputStream

data class Video(val id: String, val url: String)

@Composable
fun MyPageApplScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var userDoc by remember { mutableStateOf<DocumentSnapshot?>(null)}
    var isLoading by remember { mutableStateOf(true) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (user == null) {
        Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        navController.navigate(Screen.Login.route)
        return
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { docs ->
                userDoc = docs
                isLoading = false
            }
            .addOnFailureListener { task ->
                Toast.makeText(context, "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) {
        try {
            val videosCollection = db.collection("videos")
            val querySnapshot = videosCollection.whereEqualTo("uploader", "정인턴").get().await()
            val videoList = querySnapshot.documents.mapNotNull { document ->
                val videoId = document.id
                val videoUrl = storage.reference.child("interview-films/session_1743959687524/$videoId").downloadUrl.await().toString()
                Video(videoId, videoUrl)
            }
            videos = videoList
            println("Videos loaded: $videos")  // 디버깅을 위해 로그 추가
        } catch (e: Exception) {
            Toast.makeText(context, "영상을 불러오는 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            println("Error loading videos: ${e.message}")  // 디버깅을 위해 로그 추가
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bitmap = inputStream?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
            imageBitmap = bitmap?.asImageBitmap()
        }
    }

    if (isLoading) {
        Text("로딩 중 ...")
    } else {
        Scaffold(
            bottomBar = { BottomNavigationAppl(navController) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // 사용자 정보 표시
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(64.dp)
                    ) {
                        imageBitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "User Image",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { launcher.launch("image/*") }
                                    .background(Color.Gray, CircleShape)
                            )
                        } ?: Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { launcher.launch("image/*") }
                                .background(Color.Gray, CircleShape)
                        ) {
                            Text(
                                text = "이미지 추가",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(userDoc?.getString("name") ?: "정보 없음", style = MaterialTheme.typography.titleMedium)
                        Text(userDoc?.getString("email") ?: "정보 없음", style = MaterialTheme.typography.bodyMedium)
                        Text("전화번호: ${userDoc?.getString("phoneNumber") ?: "정보 없음"}", style = MaterialTheme.typography.bodyMedium)
                        Text("직책: ${userDoc?.getString("jobTitle") ?: "정보 없음"}", style = MaterialTheme.typography.bodyMedium)
                        Text("생년월일: ${userDoc?.getString("birthDate") ?: "정보 없음"}", style = MaterialTheme.typography.bodyMedium)
                        Text("분야: ${userDoc?.getString("category") ?: "정보 없음"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // 로그아웃 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = { showLogoutDialog = true }) {
                        Text("로그아웃")
                    }
                }

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("로그아웃") },
                        text = { Text("정말 로그아웃하시겠습니까?") },
                        confirmButton = {
                            Button(onClick = {
                                showLogoutDialog = false
                                auth.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }) {
                                Text("예")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showLogoutDialog = false }) {
                                Text("아니오")
                            }
                        }
                    )
                }

                // 영상 목록 표시
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    items(videos) { video ->
                        Text("${video.id}: ${video.url}", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyPageApplScreen() {
    MyPageApplScreen(navController = rememberNavController())
}