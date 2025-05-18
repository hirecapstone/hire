package com.example.hireapp.screens.interviewer

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyPageIntrScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var userDoc by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    var likedVideos by remember { mutableStateOf<List<MyPageVideoItem>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        imageUri?.let {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    if (user == null) {
        Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        navController.navigate("login")
        return
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { docs ->
                userDoc = docs
                name = docs.getString("name") ?: ""
                email = docs.getString("email") ?: ""
                phoneNumber = docs.getString("phoneNumber") ?: ""
                birthDate = docs.getString("birthDate") ?: ""
                role = docs.getString("role") ?: ""

                db.collection("interview").get()
                    .addOnSuccessListener { allDocs ->
                        val tempList = mutableListOf<MyPageVideoItem>()
                        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.getDefault())

                        allDocs.forEach { doc ->
                            doc.reference.collection("likes").document(user.uid).get()
                                .addOnSuccessListener { likeDoc ->
                                    if (likeDoc.exists() && doc.getBoolean("public") == true) {
                                        val category = doc.get("category") as? Map<*, *>
                                        val date = doc.getTimestamp("uploadTime")?.toDate()
                                        val formattedDate = date?.let { dateFormat.format(it) } ?: "날짜 없음"

                                        tempList.add(
                                            MyPageVideoItem(
                                                id = doc.id,
                                                title = doc.getString("title") ?: "제목 없음",
                                                date = formattedDate,
                                                major = category?.get("major") as? String ?: "대분류 없음",
                                                minor = category?.get("sub") as? String ?: "소분류 없음",
                                                isPublic = true
                                            )
                                        )
                                        likedVideos = tempList.toList()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "좋아요 데이터 조회 실패", Toast.LENGTH_SHORT).show()
                                }
                        }
                        isLoading = false
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "영상 데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
    }

    if (isLoading) {
        Text("로딩 중 ...")
    } else {
        Scaffold(
            bottomBar = { BottomNavigationIntr(navController) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.example.hireapp.R.drawable.info),
                        contentDescription = "User Image",
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("이름: $name", style = MaterialTheme.typography.titleMedium)
                        Text("생년월일: $birthDate", style = MaterialTheme.typography.bodyMedium)
                        Text("이메일: $email", style = MaterialTheme.typography.bodyMedium)
                        Text("전화번호: $phoneNumber", style = MaterialTheme.typography.bodyMedium)
                        Text("역할: ${if (role == "면접자") "면접자" else "면접관"}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("개인정보 수정")
                    }
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("로그아웃")
                    }
                }

                // 좋아요 영상 리스트 표시
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(likedVideos) { video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    navController.navigate("${Screen.VideoDetail.route}/${video.id}")
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("촬영일: ${video.date}")
                                Text("대분류: ${video.major} / 소분류: ${video.minor}")
                                Text(if (video.isPublic) "공개" else "비공개")
                            }
                        }
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
                                navController.navigate("login")
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

                if (showEditDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text("개인정보 수정") },
                        text = {
                            Column {
                                TextField(value = name, onValueChange = { name = it }, label = { Text("이름") })
                                TextField(value = email, onValueChange = { email = it }, label = { Text("이메일") })
                                TextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("전화번호") })
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                db.collection("users").document(user.uid)
                                    .update(mapOf("name" to name, "email" to email, "phoneNumber" to phoneNumber))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "수정 완료", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Text("저장")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showEditDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyPageIntrScreen() {
    MyPageIntrScreen(navController = rememberNavController())
}
