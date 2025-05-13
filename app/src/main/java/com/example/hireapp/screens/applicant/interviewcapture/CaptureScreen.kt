package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.screens.applicant.BottomNavigationAppl
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*

@Composable
fun CaptureScreen(navController: NavController) {
    val currentStep = remember { mutableStateOf(1) }
    var sessionId by remember { mutableStateOf("") } // 세션 ID 상태를 var로 선언
    var major by remember { mutableStateOf("") }    // SelectRoleScreen에서 전달받을 major
    var sub by remember { mutableStateOf("") }      // SelectRoleScreen에서 전달받을 sub

    Scaffold(
        bottomBar = {
            if (currentStep.value == 1) {
                BottomNavigationAppl(navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentStep.value) {
                1 -> SelectRoleScreen(onNext = { selectedMajor: String, selectedSub: String, generatedSessionId: String ->
                    major = selectedMajor
                    sub = selectedSub
                    sessionId = generatedSessionId // SelectRoleScreen에서 전달된 sessionId 저장
                    currentStep.value = 2
                })
                2 -> WarningScreen(onNext = { currentStep.value = 3 })
                3 -> CameraSetupScreen(
                    navController = navController,
                    fromInsert = false,
                    questionsJson = "[]",
                    onNext = { currentStep.value = 4 }
                )
                4 -> QuestionScreen(navController = navController, sessionId = sessionId, major = major, sub = sub) // major, sub, sessionId 전달
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCaptureScreen() {
    CaptureScreen(navController = rememberNavController())
}