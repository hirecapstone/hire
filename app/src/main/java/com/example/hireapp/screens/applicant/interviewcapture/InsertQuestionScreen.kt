// InsertQuestionScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hireapp.screens.applicant.interviewcapture

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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.google.gson.Gson

@Composable
fun InsertQuestionScreen(navController: NavController) {
    val questionList = remember { mutableStateListOf<String>() }
    if (questionList.isEmpty()) {
        questionList.add("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("질문 입력") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(questionList) { index, question ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = question,
                            onValueChange = { questionList[index] = it },
                            label = { Text("질문 ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { questionList.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { questionList.add("") }) {
                    Icon(Icons.Default.Add, contentDescription = "질문 추가")
                }
                Button(onClick = {
                    val gson = Gson()
                    val questionsJson = URLEncoder.encode(gson.toJson(questionList), StandardCharsets.UTF_8.toString())
                    navController.navigate("${Screen.Warning.route}?fromInsert=true&questions=$questionsJson")
                }) {
                    Text("다음")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInsertQuestionScreen() {
    InsertQuestionScreen(navController = rememberNavController())
}