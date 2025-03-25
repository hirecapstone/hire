package com.example.hireapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.screens.login.LoginScreen
import com.example.hireapp.screens.login.SignUpCommonScreen
import com.example.hireapp.screens.login.SignUpInterviewerScreen
import com.example.hireapp.screens.login.SignUpScreen
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase 초기화
        Firebase.auth
        Firebase.firestore

        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignUpScreen(navController) }
        composable("signup_common/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: ""
            SignUpCommonScreen(navController, role)
        }
        composable("signup_interviewer") { SignUpInterviewerScreen(navController) }
    }
}