package com.example.hireapp.screens.applicant

import android.util.Log
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.models.MyPageVideoItem
import com.example.hireapp.navigation.Screen
import com.example.hireapp.util.LoadingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MyPageApplScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var userDoc by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Fields for editing
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    // 인터뷰 목록 불러올 변수
    var videoList by remember { mutableStateOf<List<MyPageVideoItem>>(emptyList()) }

    if (user == null) {
        Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        navController.navigate(Screen.Login.route)
        return
    }

    LaunchedEffect(Unit) {
        LoadingState.show("인터뷰 목록을 불러오는 중입니다.")

        try {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    userDoc = doc
                    name = doc.getString("name") ?: ""
                    email = doc.getString("email") ?: ""
                    phoneNumber = doc.getString("phoneNumber") ?: ""
                    birthDate = doc.getString("birthDate") ?: ""
                    role = doc.getString("role") ?: ""
                    isLoading = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }

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

            isLoading = false
        } catch(e: Exception) {
            Toast.makeText(context, "마이페이지 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bitmap = inputStream?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
            imageBitmap = bitmap?.asImageBitmap()
        }
    }

    if (!isLoading) {
        LoadingState.hide()

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
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.example.hireapp.R.drawable.info),
                        contentDescription = "User Image",
                        modifier = Modifier
                            .size(96.dp)
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


                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("로그아웃") },
                        text = { Text("로그아웃 하시겠습니까?") },
                        confirmButton = {
                            Button(onClick = {
                                FirebaseAuth.getInstance().signOut()
                                showLogoutDialog = false
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) // 백스택 제거
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

                // 개인정보 수정 다이얼로그
                if (showEditDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text("개인정보 수정") },
                        text = {
                            Column {
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("이름") }
                                )
                                TextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("이메일") }
                                )
                                TextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("전화번호") }
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                // Firestore 업데이트
                                db.collection("users").document(user.uid)
                                    .update(
                                        mapOf(
                                            "name" to name,
                                            "email" to email,
                                            "phoneNumber" to phoneNumber
                                        )
                                    )
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

                // 인터뷰 삭제시 알람
                var showDeleteDialog by remember { mutableStateOf(false) }
                var selectedVideoId by remember { mutableStateOf<String?>(null) }

                VideoListScreen(
                    videos = videoList,
                    onTogglePublic = { videoId, newStatus ->
                        videoList = videoList.map {
                            if (it.id == videoId) it.copy(isPublic = newStatus) else it
                        }
                    },
                    onDeleteRequest = { videoId ->
                        selectedVideoId = videoId
                        showDeleteDialog = true
                    }
                )

                // 면접 기록 삭제
                if (showDeleteDialog && selectedVideoId != null) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("면접 기록 삭제") },
                        text = { Text("삭제한 면접 기록은 복원할 수 없습니다. 정말로 삭제하시겠습니까?") },
                        confirmButton = {
                            /**
                             * DB 삭제 목록:
                             *  interview 문서
                             *  feedback 문서
                             *  storage에 저장된 면접 동영상
                             *
                             * TODO: 위에 작성된 것 외에 'AI 생성 면접 질문, mediapipe 결과 등...' 삭제하려면 db 구조 변경 필요
                             *  문제:? 트랜잭션 적용되지 않은 상태
                             */
                            Button(onClick = {
                                FirebaseFirestore.getInstance()
                                    .collection("interview")
                                    .document(selectedVideoId!!)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        val videoFileRef = FirebaseStorage.getInstance().reference.child("videos/${doc.id}")
                                        val feedbackRef = doc.getDocumentReference("feedback")

                                        FirebaseFirestore.getInstance()
                                            .collection("interview")
                                            .document(selectedVideoId!!)
                                            .delete()
                                            .addOnSuccessListener {
                                                // feedback 문서 삭제
                                                feedbackRef?.delete()?.addOnFailureListener {
                                                    Log.d(
                                                        "delete-interview",
                                                        "feedback 문서 삭제에 실패했습니다."
                                                    )
                                                }
                                            }

                                        // storage에서 동영상 파일 삭제
                                        videoFileRef.listAll()
                                            .addOnSuccessListener { listResult ->
                                                listResult.items.forEach { item ->
                                                    item.delete()
                                                }
                                            }
                                            .addOnFailureListener {
                                                Log.d("interview-delete", "storage 동영상 파일 삭제에 실패했습니다.")
                                            }

                                        Toast.makeText(
                                            context,
                                            "면접 기록 삭제가 완료되었습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        videoList = videoList.filterNot { it.id == selectedVideoId }
                                    }
                                showDeleteDialog = false
                            }) {
                                Text("확인")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDeleteDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 영상 목록을 띄우는 함수
 */
@Composable
fun VideoListScreen(
    videos: List<MyPageVideoItem>,
    onTogglePublic: (String, Boolean) -> Unit,
    onDeleteRequest: (String) -> Unit
) {
    val context = LocalContext.current
    var expandedMenuId by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(videos) { video ->
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

                    var iconButtonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentSize()
                    ) {
                        IconButton(
                            onClick = {
                                expandedMenuId = if (expandedMenuId == video.id) null else video.id
                            },
                            modifier = Modifier
                                .onGloballyPositioned {
                                    iconButtonCoordinates = it
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기 메뉴"
                            )
                        }

                        DropdownMenu(
                            expanded = expandedMenuId == video.id,
                            onDismissRequest = { expandedMenuId = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd) // 이게 핵심
                        ) {
                            // 공개 비공개 설정 항목
                            DropdownMenuItem(
                                text = { Text(if (video.isPublic) "비공개로 설정" else "공개로 설정") },
                                onClick = {
                                    expandedMenuId = null
                                    val newStatus = !video.isPublic
                                    FirebaseFirestore.getInstance()
                                        .collection("interview")
                                        .document(video.id)
                                        .update("public", newStatus)
                                        .addOnSuccessListener {
                                            onTogglePublic(video.id, newStatus)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "공개 상태 변경에 실패했습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            )
                            // 삭제 항목
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                onClick = {
                                    expandedMenuId = null
                                    onDeleteRequest(video.id)
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
fun PreviewMyPageApplScreen() {
    MyPageApplScreen(navController = rememberNavController())
}

