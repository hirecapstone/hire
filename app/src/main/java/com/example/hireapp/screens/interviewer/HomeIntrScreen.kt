package com.example.hireapp.screens.interviewer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.Screen
import com.example.hireapp.util.LoadingState
import com.example.hireapp.util.Paging
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun HomeIntrScreen(navController: NavController) {

    // 페이징용 변수
    var paging: Paging = Paging()
    var videos by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    /**
     * db의 면접 영상들을 필터 조건에 맞게 불러오는 메서드
     *
     * @param majorItem 대분류 필터 아이템(한 개)
     * @param subItems 소분류 필터 아이템(하나 이상)
     *
     * @return 필터를 거친 문서들의 영상 리스트 목록
     */
    fun loadVideos(
        majorItem: String? = null,
        subItems: List<String>? = null,
        size: Long = 10
    ): List<List<String>> {
        val db = Firebase.firestore
        val interviewRef = db.collection("interview")

        LoadingState.show()

        // 면접 게시물 별 영상들의 다운로드 url을 저장함.
        val filteredVideoList = mutableListOf<List<String>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 업로드 시간 기준으로 내림차순 정렬, size 단위로 페이징해서 정보를 불러옴
                var query = interviewRef
                    .orderBy("uploadTime", Query.Direction.DESCENDING)
                    .limit(size)

                // 대분류 조건이 있는 경우 조건 추가
                if (!majorItem.isNullOrBlank()) {
                    query = query.whereEqualTo("category.major", majorItem)
                }

                // 소분류 조건이 있는 경우 조건 추가
                if (!subItems.isNullOrEmpty()) {
                    query = query.whereIn("category.sub", subItems)
                }

                // 이전 페이지가 존재하는 경우 조건 추가
                if (paging.lastDocument != null) {
                    query = query.startAfter(paging.lastDocument)
                }

                val docs = query.get().await()

                withContext(Dispatchers.Main) {
                    docs.forEach { doc ->
                        try {
                            val fieldList = doc.get("videos") as? List<*>
                            val videoUrls = fieldList?.filterIsInstance<String>() ?: emptyList()
                            filteredVideoList.add(videoUrls)
                        } catch (e: Exception) {
                            Log.d("Load_page_video", "비디오 로드 중 오류 발생: ${doc.id}")
                        }
                    }
                    paging = Paging(lastDocument = docs.lastOrNull())
                    LoadingState.hide()
                }
            } catch (e: Exception) {
                // TODO: 새로고침 구현?
                Log.d("Load_page", "페이지 로드 중 오류가 발생했습니다: ${e.message}")
            }
        }

        return filteredVideoList.toList()
    }

    val videoList = remember {
        listOf( // 더미데이터
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
    }

    LaunchedEffect(Unit) {
        videos = loadVideos()
        Log.d("loadVideos", videos.toString())
    }

    Scaffold(
        bottomBar = { BottomNavigationIntr(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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

                        // 더미 UI - 실제 영상 썸네일/재생기로 교체 예정
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("영상 미리보기")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 좋아요 & 댓글 아이콘 (좋아요 기능은 아직 구현x)
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

                        // 영상 제목
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

@Preview(showBackground = true)
@Composable
fun PreviewHomeIntrScreen() {
    HomeIntrScreen(navController = rememberNavController())
}

data class VideoItem(val id: String, val title: String, val userName: String)
data class Comment(val user: String, val text: String)