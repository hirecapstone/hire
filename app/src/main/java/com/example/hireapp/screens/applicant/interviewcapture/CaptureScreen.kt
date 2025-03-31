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

    Scaffold(
        bottomBar = {
            if (currentStep.value == 1) {
                BottomNavigationAppl(navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentStep.value) {
                1 -> SelectRoleScreen(onNext = { currentStep.value = 2 })
                2 -> WarningScreen(onNext = { currentStep.value = 3 })
                3 -> CameraSetupScreen(onNext = { currentStep.value = 4 })
                4 -> QuestionScreen(navController)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewCaptureScreen() {
    CaptureScreen(navController = rememberNavController())
}