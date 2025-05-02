package com.example.hireapp.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.hireapp.screens.login.*
import com.example.hireapp.screens.applicant.*
import com.example.hireapp.screens.applicant.interviewcapture.CaptureScreen
import com.example.hireapp.screens.applicant.interviewcapture.QuestionScreen
import com.example.hireapp.screens.interviewer.*
import com.example.hireapp.screens.VideoDetailScreen
import com.example.hireapp.screens.applicant.interviewcapture.CaptureOptionScreen
import com.example.hireapp.screens.applicant.interviewcapture.CheckQuestionScreen
import com.example.hireapp.screens.applicant.interviewcapture.InsertQuestionScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), userType: String? = null) {
    val startDestination = when (userType) {
        "면접자" -> Screen.HomeAppl.route
        "면접관" -> Screen.HomeIntr.route
        else -> Screen.Login.route  // 로그인 화면
    }

    Log.d("UserType", userType.toString())
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.SignUp.route) { SignUpScreen(navController) }
        composable(Screen.SignUpCommon.route + "/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: ""
            SignUpCommonScreen(navController, role)
        }
        composable(Screen.SignUpInterviewer.route) { SignUpInterviewerScreen(navController) }
        composable(Screen.HomeAppl.route) { HomeApplScreen(navController) }
        composable(Screen.HomeIntr.route) { HomeIntrScreen(navController) }
        composable(Screen.MyPageAppl.route) { MyPageApplScreen(navController) }
        composable(Screen.MyPageIntr.route) { MyPageIntrScreen(navController) }
        composable(Screen.Capture.route) { CaptureScreen(navController) }
        composable(Screen.CaptureOption.route) { CaptureOptionScreen(navController) }
        composable(Screen.InsertQuestion.route) { InsertQuestionScreen(navController) }
        composable(Screen.CheckQuestion.route) { CheckQuestionScreen(navController) }

        // 질문 화면 추가 (세션 ID, major, sub 필요)
        composable(
            route = "${Screen.QuestionScreen.route}/{sessionId}/{major}/{sub}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("major") { type = NavType.StringType },
                navArgument("sub") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val major = backStackEntry.arguments?.getString("major") ?: return@composable
            val sub = backStackEntry.arguments?.getString("sub") ?: return@composable

            QuestionScreen(navController = navController, sessionId = sessionId, major = major, sub = sub)
        }

        // 영상 상세 보기 (댓글 포함)
        composable(
            route = "${Screen.VideoDetail.route}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("id") ?: return@composable
            VideoDetailScreen(videoId = videoId, navController = navController)
        }
    }
}
