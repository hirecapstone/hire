package com.example.hireapp.screens.applicant

import android.net.Uri
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeApplScreen(navController: NavController) {
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

    // 공개 면접 영상 리스트 로드
    LaunchedEffect(Unit) {
        videoList = fetchPublicVideos(selectedMajor?.label, selectedSubs)
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
            val filterVisibilityAnim by animateFloatAsState(
                targetValue = if (isFilterVisible) 1f else 0f,
                label = "filterAnimation"
            )

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.no),
                            contentDescription = "없음 이미지",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("불러올 영상이 없습니다.", color = Color.Gray)
                    }
                }
            }
            else {
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

                            // 영상 미리보기
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                VideoPlayer(url = video.fileUrl!!)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 좋아요 & 댓글 아이콘
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
}

@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            exoPlayer.playWhenReady = true
                        }
                    )
                }
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeApplScreen() {
    HomeApplScreen(navController = rememberNavController())
}
