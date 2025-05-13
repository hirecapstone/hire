package com.example.hireapp.screens

import android.content.Context
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

class VideoPlayerViewModel(
    private val url: String,
    private val context: Context
) : ViewModel() {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        prepare()
        playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(videoId: String, navController: NavController) {
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var video by remember { mutableStateOf<VideoItem?>(null) }
    var comments = remember { mutableStateListOf<Comment>() }
    var inputText by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("me") }
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 현재 로그인한 유저 이름 가져오기
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
                    Toast.makeText(context, "댓글 불러오기 실패", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    comments.clear()
                    for (doc in snapshot.documents) {
                        val user = doc.getString("user") ?: "익명"
                        val text = doc.getString("text") ?: ""
                        comments.add(Comment(user, text))
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
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.hireapp.R.drawable.movie),
                                contentDescription = "영상 이미지",
                                modifier = Modifier.size(45.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "면접 영상",
                                fontSize = 35.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    navController.navigate("${Screen.ResultScreen.route}/$videoId")
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = com.example.hireapp.R.drawable.detail),
                                        contentDescription = "결과 아이콘",
                                        modifier = Modifier
                                            .size(35.dp)
                                            .offset(y = 12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "상세 피드백",
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        modifier = Modifier.offset(y = 12.dp)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.example.hireapp.R.drawable.title),
                                    contentDescription = "제목 이미지",
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = video!!.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.example.hireapp.R.drawable.name),
                                    contentDescription = "이름 이미지",
                                    modifier = Modifier.size(68.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = video!!.userName,
                                    fontSize = 20.sp,
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
                                .aspectRatio(9f / 16f)
                        ) {
                            itemsIndexed(video!!.fileUrls) { index, url ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(9f / 16f)
                                ) {
                                    VideoPlayer(url = url)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    itemsIndexed(comments) { index, comment ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(text = comment.user, fontWeight = FontWeight.Bold)
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
                            val newComment = Comment(currentUserName, inputText)
                            coroutineScope.launch {
                                try {
                                    db.collection("interview")
                                        .document(videoId)
                                        .collection("comments")
                                        .add(mapOf(
                                            "user" to newComment.user,
                                            "text" to newComment.text,
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

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val viewModel: VideoPlayerViewModel = viewModel(
        key = url,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VideoPlayerViewModel(url, context) as T
            }
        }
    )
    val exoPlayer = viewModel.exoPlayer

    // ⬇️ 이 DisposableEffect 블록을 추가하세요.
    DisposableEffect(exoPlayer) {
        onDispose {
            // Composable 이 빠져나갈 때(예: 회전) 재생 일시정지
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { exoPlayer.playWhenReady = true }
            }
    )
}

