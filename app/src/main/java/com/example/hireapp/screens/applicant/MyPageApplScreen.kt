package com.example.hireapp.screens.applicant

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MyPageApplScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current

    var userDoc by remember { mutableStateOf<DocumentSnapshot?>(null)}
    var isLoading by remember { mutableStateOf(true) }

    if (user == null) {
        Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        navController.navigate(Screen.Login.route)
        return
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { docs ->
                userDoc = docs
                isLoading = false
            }
            .addOnFailureListener { task ->
                Toast.makeText(context, "유저 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    if (isLoading) {
        Text("로딩 중 ...")
    } else {
        Scaffold(
            bottomBar = { BottomNavigationAppl(navController) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(userDoc?.getString("name")?:"정보 없음", modifier = Modifier.padding(16.dp))
                Text("생년월일: ${userDoc?.getString("birthDate")?:"정보 없음"}", modifier = Modifier.padding(16.dp))
                Text("분야: ${userDoc?.getString("category")?:"정보 없음"}", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyPageApplScreen() {
    MyPageApplScreen(navController = rememberNavController())
}