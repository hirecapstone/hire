package com.example.hireapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.AppNavHost
import com.example.hireapp.screens.login.LoginScreen
import com.example.hireapp.screens.login.SignUpCommonScreen
import com.example.hireapp.screens.login.SignUpInterviewerScreen
import com.example.hireapp.screens.login.SignUpScreen
import com.example.hireapp.util.GlobalLoadingScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.installations.ktx.installations

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase 초기화
        Firebase.auth
        Firebase.firestore

        setContent {
            MyApp()
            GlobalLoadingScreen()
        }
    }
}

@Composable
fun MyApp() {
    val user = FirebaseAuth.getInstance().currentUser;
    var userType = ""
    if(user != null) {
        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                userType = doc.getString("role").orEmpty();
            }
    }
    val navController = rememberNavController()
    AppNavHost(navController, userType)
}