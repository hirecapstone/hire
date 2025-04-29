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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.hireapp.models.MyPageVideoItem
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

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

    // 인터뷰 목록 불러올 변수
    var videoList by remember { mutableStateOf<List<MyPageVideoItem>>(emptyList()) }
    // 공개 비공개 드롭다운
    var expanded by remember { mutableStateOf(false) }

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
        val documents = db.collection("interview")
            .whereEqualTo("user", user.uid)
            .get()
            .await()

        videoList = documents.map { doc ->
            val category = doc.get("category") as? Map<*, *>

            val date = doc.getTimestamp("uploadTime")?.toDate()
            val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault())
            val formattedDate = date?.let { dateFormat.format(it) } ?: "날짜 없음"

            MyPageVideoItem(
                id = doc.id,
                title = doc.getString("title") ?: "제목 없음",
                date = formattedDate,
                major = category?.get("major") as? String ?: "대분류 없음",
                minor = category?.get("sub") as? String ?: "소분류 없음",
                isPublic = doc.getBoolean("public") ?: false
            )
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(videoList) { video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "촬영일: ${video.date}")
                                    Text(text = "대분류: ${video.major} / 소분류: ${video.minor}")
                                    Text(text = if (video.isPublic) "공개" else "비공개")
                                }

                                IconButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "더보기 메뉴"
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        // TODO: 설정 변경시 바로 적용되도록 새로고침 추가
                                        text = { Text(if (video.isPublic) "비공개로 설정" else "공개로 설정") },
                                        onClick = {
                                            expanded = false
                                            FirebaseFirestore.getInstance()
                                                .collection("interview")
                                                .document(video.id)
                                                .update("public", !video.isPublic)
                                        }
                                    )
                                }
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
fun PreviewMyPageApplScreen() {
    MyPageApplScreen(navController = rememberNavController())
}