package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.screens.applicant.BottomNavigationAppl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Capture2Screen(navController: NavController) {
    val currentStep = remember { mutableStateOf(1) }
    var sessionId by remember { mutableStateOf("") } // 세션 ID 상태를 var로 선언
    var major by remember { mutableStateOf("") }    // SelectRoleScreen에서 전달받을 major
    var sub by remember { mutableStateOf("") }      // SelectRoleScreen에서 전달받을 sub
    var questions by remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Step ${currentStep.value}: ${getStep2Title(currentStep.value)}") }
                )
                LinearProgressIndicator(
                    progress = currentStep.value / 4f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        },
        bottomBar = {
            if (currentStep.value == 1) {
                BottomNavigationAppl(navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentStep.value) {
                1 -> InsertQuestionScreen(
                    navController = navController,
                    onNext = { selectedMajor, selectedSub, generatedSessionId, enteredQuestions ->
                        major = selectedMajor
                        sub = selectedSub
                        sessionId = generatedSessionId
                        questions = enteredQuestions // 이 값을 받아와서 업데이트
                        currentStep.value = 2
                    }
                )
                2 -> WarningScreen(onNext = { currentStep.value = 3 })
                3 -> CameraSetupScreen(
                    navController = navController,
                    fromInsert = false,
                    questionsJson = "[]", // 실제 질문 리스트를 전달하려면 여기에 `questions` 값을 넣어야 합니다
                    onNext = { currentStep.value = 4 }
                )
                4 -> CheckQuestionScreen(
                    navController = navController,
                    sessionId = sessionId,
                    major = major,
                    sub = sub,
                    questions = questions
                )
            }
        }
    }
}

fun getStep2Title(step: Int): String = when (step) {
    1 -> "정보입력"
    2 -> "주의사항"
    3 -> "카메라세팅"
    4 -> "영상촬영"
    else -> ""
}

@Preview(showBackground = true)
@Composable
fun PreviewCapture2Screen() {
    Capture2Screen(navController = rememberNavController())
}
