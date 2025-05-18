package com.example.hireapp.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hireapp.models.Comment
import com.example.hireapp.models.VideoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.hireapp.navigation.Screen
import com.example.hireapp.screens.applicant.VideoPlayerViewModel

@Composable
fun VideoPlayer(url: String, playerVm: VideoPlayerViewModel ) {

    val exoPlayer = playerVm.getPlayer(url)

    DisposableEffect(exoPlayer) {
        // 자동재생
//        exoPlayer.playWhenReady = true
        onDispose {
            // 컴포저블이 사라질 때 재생 중지
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(5f / 6f)
            .pointerInput(Unit) {
                detectTapGestures { exoPlayer.playWhenReady = true }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(videoId: String, navController: NavController) {
    val playerVm: VideoPlayerViewModel = viewModel(
        factory = VideoPlayerViewModel.Factory(LocalContext.current)
    )
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var video by remember { mutableStateOf<VideoItem?>(null) }
    var comments = remember { mutableStateListOf<Comment>() }
    var inputText by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("me") }
    var currUserMajor by remember { mutableStateOf("지정 없음") }
    var currUserSub by remember { mutableStateOf("지정 없음") }
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var currentUserRole by remember { mutableStateOf("익명") }

    DisposableEffect(Unit) {
        onDispose { playerVm.releaseAllPlayers() }
    }
    // 현재 로그인한 유저 이름 가져오기
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    currentUserName = doc.getString("name") ?: "익명"
                    val categoryMap = doc.get("category") as? Map<*, *>
                    currUserMajor = categoryMap?.get("major") as? String ?: "지정 없음"
                    currUserSub = categoryMap?.get("sub") as? String ?: "지정 없음"
                    currentUserRole = doc.getString("role") ?: "익명"
                    Log.d("Comment", "로그인한 유저 ㅅcategory: ${categoryMap}, ${currUserMajor}, ${currUserSub}")
                }
                .addOnFailureListener {
                    Toast.makeText(context, "유저 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 영상 상세 정보 가져오기
    LaunchedEffect(videoId) {
        try {
            val doc = db.collection("interview").document(videoId).get().await()
            val title = doc.getString("title") ?: "제목 없음"

            val userField = doc.get("user")
            val userName = when (userField) {
                is DocumentReference -> {
                    try {
                        val snapshot = userField.get().await()
                        snapshot.getString("name")
                    } catch (e: Exception) {
                        null
                    }
                }
                is String -> {
                    try {
                        val snapshot = db.collection("users").document(userField).get().await()
                        snapshot.getString("name")
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> null
            }

            if (userName.isNullOrEmpty()) {
                Toast.makeText(context, "유저 이름을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }

            val videosList = doc.get("videos") as? List<Map<String, Any>>
            val fileUrls = videosList?.mapNotNull { it["fileUrl"] as? String } ?: emptyList()

            video = VideoItem(id = videoId, title = title, userName = userName, fileUrls = fileUrls)

        } catch (e: Exception) {
            Toast.makeText(context, "영상 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 댓글 불러오기
    LaunchedEffect(videoId) {
        db.collection("interview")
            .document(videoId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
//                  Toast.makeText(context, "댓글 불러오기 실패", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    comments.clear()
                    for (doc in snapshot.documents) {
                        val major = doc.getString("major") ?: "지정 없음"
                        val sub = doc.getString("sub") ?: "지정 없음"
                        val user = doc.getString("user") ?: "익명"
                        val text = doc.getString("text") ?: ""
                        val role  = doc.getString("role")  ?: "익명"
                        comments.add(Comment(user = user, major = major, sub = sub, text = text, role = role))
                    }
                }
            }
    }

    Scaffold (
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
                        .padding(horizontal = 8.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = com.example.hireapp.R.drawable.movie),
                                contentDescription = "영상 이미지",
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "면접 영상",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                contentPadding = PaddingValues(0.dp),
                                onClick = {
                                    playerVm.releaseAllPlayers()
                                    navController.navigate("${Screen.ResultScreen.route}/$videoId")
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = com.example.hireapp.R.drawable.detail2),
                                        contentDescription = "디테일 아이콘",
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "상세 피드백",
                                        color = Color.Black,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.example.hireapp.R.drawable.title),
                                    contentDescription = "제목 이미지",
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = video!!.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.example.hireapp.R.drawable.name),
                                    contentDescription = "이름 이미지",
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = video!!.userName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            state = listState,
                            flingBehavior = flingBehavior,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(5f / 6f)
                                .align(Alignment.CenterHorizontally),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            itemsIndexed(video!!.fileUrls) { index, url ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(5f / 6f)
                                        .align(Alignment.CenterHorizontally),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VideoPlayer(
                                        url = url,
                                        playerVm = playerVm
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    itemsIndexed(comments) { _, comment ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val avatarRes = when (comment.role) {
                                    "면접관" -> com.example.hireapp.R.drawable.speak
                                    "면접자" -> com.example.hireapp.R.drawable.hear         // 면접자 아이콘
                                    else      -> com.example.hireapp.R.drawable.hear
                                }
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = comment.role,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                                Text(text = comment.user, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${comment.major} | ${comment.sub}", color = Color.Gray)
                            }
                            Text(text = comment.text)
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
                            val newComment = Comment(currentUserName, currUserMajor, currUserSub, inputText, currentUserRole)
                            coroutineScope.launch {
                                try {
                                    db.collection("interview")
                                        .document(videoId)
                                        .collection("comments")
                                        .add(mapOf(
                                            "user" to newComment.user,
                                            "text" to newComment.text,
                                            "major" to newComment.major,
                                            "sub" to newComment.sub,
                                            "role"      to newComment.role,
                                            "timestamp" to System.currentTimeMillis()
                                        ))
                                    inputText = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, "댓글 등록 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
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



