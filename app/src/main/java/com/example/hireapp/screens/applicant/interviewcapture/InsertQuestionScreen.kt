// InsertQuestionScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.google.gson.Gson
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// Import 구문 추가
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun InsertQuestionScreen(
    navController: NavController,
    onNext: (selectedMajor: String, selectedSub: String, generatedSessionId: String, enteredQuestions: List<String>) -> Unit // enteredQuestions 추가
) {
    // 타입 명시적으로 지정
    val questionList = remember { mutableStateListOf<String>() }
    if (questionList.isEmpty()) {
        questionList.add("")
    }

    // 추가된 값들
    var selectedMajor = remember { mutableStateOf("컴퓨터공학") }
    var selectedSub = remember { mutableStateOf("AI") }
    var generatedSessionId = remember { mutableStateOf("session123") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.question),
                            contentDescription = "질문 입력 이미지",
                            modifier = Modifier
                                .size(45.dp)
                                .padding(end = 8.dp)
                        )
                        Text("질문 입력", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // LazyColumn으로 질문 목록을 렌더링
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(questionList) { index, question ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 질문을 위한 TextField
                        TextField(
                            value = question,
                            onValueChange = { questionList[index] = it },
                            label = { Text("질문 ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        // 삭제 버튼
                        IconButton(onClick = { questionList.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제 아이콘")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // 각 질문 항목 사이에 간격 추가
                }

                // 마지막 항목에 질문 추가 버튼을 추가
                item {
                    Spacer(modifier = Modifier.height(16.dp)) // 질문과 추가 버튼 사이에 간격 추가
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { questionList.add("") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.example.hireapp.R.drawable.add),
                                    contentDescription = "질문 추가 이미지",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("질문 추가", color = Color.Black)
                            }
                        }
                    }
                }
            }

            // "다음" 버튼을 하단에 배치
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        // "다음" 버튼 클릭 시 onNext 콜백 호출, 값 전달
                        onNext(selectedMajor.value, selectedSub.value, generatedSessionId.value, questionList)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.next),
                            contentDescription = "다음 이미지",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("다음", color = Color.Black)
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewInsertQuestionScreen() {
    // Preview를 위한 onNext 정의
    InsertQuestionScreen(
        navController = rememberNavController(),
        onNext = { selectedMajor, selectedSub, generatedSessionId, enteredQuestions ->
            // 임시로 값 출력해보기
            println("Selected Major: $selectedMajor")
            println("Selected Sub: $selectedSub")
            println("Generated Session ID: $generatedSessionId")
            println("Entered Questions: $enteredQuestions")
        }
    )
}

