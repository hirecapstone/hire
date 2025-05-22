package com.example.hireapp.screens.applicant

import android.net.Uri
import android.provider.ContactsContract.Data
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.models.VideoItem
import com.example.hireapp.navigation.Screen
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.hireapp.data.Category
import com.example.hireapp.data.fetchPublicVideos
import com.example.hireapp.data.subCategory
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.hireapp.screens.VideoPlayer
import com.google.firebase.auth.ktx.auth

data class FilterItems(var major:Category?, var subs: List<String>?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeApplScreen(navController: NavController) {
    val playerVm: VideoPlayerViewModel = viewModel(
        factory = VideoPlayerViewModel.Factory(LocalContext.current)
    )
    val coroutineScope = rememberCoroutineScope()

    var videoList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    var selectedMajor by remember { mutableStateOf<Category?>(null) }
    var selectedSubs by remember { mutableStateOf<List<String>?>(null) }

    var isFilterVisible by remember { mutableStateOf(false) }
    var isFilterApplied by remember { mutableStateOf(false) }

    val onApplyFilter: () -> Unit = {
        coroutineScope.launch {
            videoList = fetchPublicVideos(
                selectedMajor?.label,
                selectedSubs
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { playerVm.releaseAllPlayers() }
    }

    // 공개 면접 영상 리스트 로드
    LaunchedEffect(Unit) {
        videoList = fetchPublicVideos(selectedMajor?.label, selectedSubs)
    }

    val listState = rememberLazyListState()
    var currentFilter: FilterItems? by rememberSaveable { mutableStateOf(null) }

    // 필터 변경 시 스크롤 최상단 이동
    LaunchedEffect(currentFilter) {
        listState.animateScrollToItem(0)
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
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFilterApplied) {
                            Text(
                                text = "필터 적용중",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(onClick = { isFilterVisible = !isFilterVisible }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "필터",
                                tint = if (isFilterApplied) Color(0xFF1976D2) else Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = { BottomNavigationAppl(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = isFilterVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FilterSection(
                    majorOptions = Category.entries,
                    subOptionsMap = subCategory,
                    selectedMajor = selectedMajor,
                    onMajorSelected = {
                        selectedMajor = it
                        selectedSubs = emptyList()
                    },
                    selectedSubs = selectedSubs,
                    onSubToggled = {
                        selectedSubs = selectedSubs?.toMutableList()?.apply {
                            if (contains(it)) remove(it) else add(it)
                        }
                    },
                    onApplyFilter = {
                        coroutineScope.launch {
                            videoList = fetchPublicVideos(selectedMajor?.label, selectedSubs)
                            isFilterVisible = false
                            isFilterApplied = true
                            currentFilter = FilterItems(major = selectedMajor, subs = selectedSubs)
                        }
                    },
                    onResetFilter = {
                        coroutineScope.launch {
                            videoList = fetchPublicVideos(null, null)
                            selectedMajor = null
                            selectedSubs = null
                            isFilterVisible = false
                            isFilterApplied = false
                            currentFilter = FilterItems(major = selectedMajor, subs = selectedSubs)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (videoList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.no),
                            contentDescription = "없음 이미지",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("불러올 영상이 없습니다.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    items(
                        items = videoList,
                        key = { it.id }  // (추가) key 지정으로 재사용 보장
                    ) { video ->
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
}

@Composable
fun LikeSection(videoId: String) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid ?: return
    val likesRef = db.collection("interview").document(videoId).collection("likes")

    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0) }

    LaunchedEffect(videoId) {
        val snapshot = likesRef.get().await()
        likeCount = snapshot.size()
        isLiked = snapshot.documents.any { it.id == userId }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "좋아요",
            tint = if (isLiked) Color.Red else Color.Gray,
            modifier = Modifier
                .size(24.dp) // 원하는 크기로 조절
                .clickable {
                    val userLikeRef = likesRef.document(userId)
                    if (isLiked) {
                        userLikeRef.delete()
                        isLiked = false
                        likeCount--
                    } else {
                        userLikeRef.set(mapOf("likedAt" to System.currentTimeMillis()))
                        isLiked = true
                        likeCount++
                    }
                }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "$likeCount", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeApplScreen() {
    HomeApplScreen(navController = rememberNavController())
}