package com.example.hireapp.screens.login

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.navigation.AppNavHost
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    fun login() {
        // Log.d("NAVIGATION", "HomeAppl route: ${Screen.HomeAppl.route}").toString()
        if (email.isEmpty()) {
            Toast.makeText(context, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(context, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser;
                if (user != null) {
                    db.collection("users").document(user.uid).get()
                        .addOnSuccessListener { doc ->
                            val role = doc.getString("role") ?: ""
                            // Log.d("Role", role)

                            when (role) {
                                "면접자" -> navController.navigate(Screen.HomeAppl.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }

                                "면접관" -> navController.navigate(Screen.HomeIntr.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }

                                else -> Toast.makeText(context, "역할 정보가 없습니다.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    Toast.makeText(context, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                val errorMessage = getFirebaseErrorMessage(task.exception)
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "로그인", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = email, onValueChange = { email = it }, label = { Text("이메일") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { login() }) {
                Text("로그인")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("signup") }) {
                Text("회원가입")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(navController = rememberNavController())
}

fun getFirebaseErrorMessage(exception: Exception?): String {
    return when (exception?.message) {
        "The email address is badly formatted." -> "이메일 형식이 올바르지 않습니다."
        "There is no user record corresponding to this identifier. The user may have been deleted." -> "가입되지 않은 이메일입니다."
        "The password is invalid or the user does not have a password." -> "비밀번호가 올바르지 않습니다."
        "We have blocked all requests from this device due to unusual activity. Try again later." -> "너무 많은 로그인 시도로 인해 잠시 후 다시 시도하세요."
        else -> "알 수 없는 오류가 발생했습니다."
    }
}