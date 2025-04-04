package com.example.hireapp.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.screens.login.LoginScreen
import com.example.hireapp.screens.login.SignUpScreen
import com.example.hireapp.screens.login.SignUpCommonScreen
import com.example.hireapp.screens.login.SignUpInterviewerScreen
import com.example.hireapp.screens.applicant.HomeApplScreen
import com.example.hireapp.screens.applicant.MyPageApplScreen
import com.example.hireapp.screens.applicant.interviewcapture.CaptureScreen
import com.example.hireapp.screens.interviewer.HomeIntrScreen
import com.example.hireapp.screens.interviewer.MyPageIntrScreen
import com.example.hireapp.screens.VideoDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

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
