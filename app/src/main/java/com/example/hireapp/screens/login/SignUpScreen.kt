package com.example.hireapp.screens.login

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.hireapp.data.Category
import com.example.hireapp.data.subCategory
import com.example.hireapp.navigation.Screen
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpModel : ViewModel() {
    var name by mutableStateOf("")
    var birthDate by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var role by mutableStateOf("")
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SignUpScreen(navController: NavController) {
    var role by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "어느 역할을 맡고 있나요?",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 면접자 선택 영역
                Card(
                    onClick = { role = "면접자" },
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (role == "면접자") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.speak),
                            contentDescription = "면접자 이미지",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("면접자", fontSize = 18.sp)
                    }
                }

                // 면접관 선택 영역
                Card(
                    onClick = { role = "면접관" },
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (role == "면접관") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = com.example.hireapp.R.drawable.hear),
                            contentDescription = "면접관 이미지",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("면접관", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (role == "면접자" || role == "면접관") {
                        navController.navigate("sign_up_common/$role")
                    } else {
                        Toast.makeText(context, "역할을 선택해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("다음")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("계정이 있으신가요?")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SignUpCommonScreen(navController: NavController, viewModel: SignUpModel, role: String) {
    var name by remember { mutableStateOf(viewModel.name) }
    var birthDate by remember { mutableStateOf(viewModel.birthDate) }
    var phoneNumber by remember { mutableStateOf(viewModel.phoneNumber) }
    var email by remember { mutableStateOf(viewModel.email) }
    var password by remember { mutableStateOf(viewModel.password) }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.hireapp.R.drawable.register),
                    contentDescription = "등록 이미지",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "회원가입",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = name, onValueChange = { name = it }, label = { Text("이름") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), leadingIcon = {
                Icon(Icons.Rounded.Person, contentDescription = "")
            }, modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),textStyle = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("생년월일 (YYYY/MM/DD)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), leadingIcon = {
                Icon(Icons.Rounded.Cake, contentDescription = "")
            }, modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),textStyle = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("전화번호") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), leadingIcon = {
                Icon(Icons.Rounded.Phone, contentDescription = "")
            }, modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),textStyle = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = email, onValueChange = { email = it }, label = { Text("이메일 (로그인 ID)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), leadingIcon = {
                Icon(Icons.Rounded.Email, contentDescription = "")
            }, modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),textStyle = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = password, onValueChange = { password = it }, label = { Text("비밀번호") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            }, trailingIcon = {
                val visibilityIcon = if (passwordVisible)
                    Icons.Default.Visibility
                else
                    Icons.Default.VisibilityOff

                val description = if (passwordVisible)
                    "비밀번호 숨기기"
                else
                    "비밀번호 보이기"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = visibilityIcon, contentDescription = description)
                }
            },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),textStyle = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("비밀번호 확인") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), leadingIcon = {
                Icon(Icons.Rounded.Lock, contentDescription = "")
            }, trailingIcon = {
                val visibilityIcon = if (confirmPasswordVisible)
                    Icons.Default.Visibility
                else
                    Icons.Default.VisibilityOff

                val description = if (confirmPasswordVisible)
                    "비밀번호 숨기기"
                else
                    "비밀번호 보이기"

                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = visibilityIcon, contentDescription = description)
                }
            },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),textStyle = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (password != confirmPassword) {
                    Toast.makeText(context, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (role == "면접관") {
                    viewModel.name = name
                    viewModel.birthDate = birthDate
                    viewModel.phoneNumber = phoneNumber
                    viewModel.email = email
                    viewModel.password = password
                    viewModel.role = role

                    navController.navigate(Screen.SignUpInterviewer.route)
                } else {
                    scope.launch {
                        val result = saveUser(
                            email = email,
                            password = password,
                            name = name,
                            birthDate = birthDate,
                            phoneNumber = phoneNumber,
                            role = role
                        )

                        if (result.isSuccess) {
                            navController.navigate(Screen.Login.route)
                            Toast.makeText(context, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Text(if (role == "면접자") "회원가입 완료" else "다음")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SignUpInterviewerScreen(navController: NavController, viewModel: SignUpModel) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedJob by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categories = Category.entries.map{it.label}
    val subJobsMap = subCategory

    Log.d("SignUp", "면접관 회원가입 시작, sc: ${selectedCategory}, sj: ${selectedJob}")

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 대분류 선택
            DropdownMenuWithFixedTextSize(
                label = "대분류",
                items = categories,
                selectedItem = selectedCategory,
                onItemSelected = { selectedCategory = it },
                width = 240.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 세부 직군
            DropdownMenuWithFixedTextSize(
                label = "세부 직군",
                items = subJobsMap[selectedCategory] ?: emptyList(),
                selectedItem = selectedJob,
                onItemSelected = { selectedJob = it },
                width = 240.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedCategory != null && selectedJob != null) {
                        Log.d("SignUp", "면접관 회원가입, sc: ${selectedCategory}, sj: ${selectedJob}")
                        val major = selectedCategory.toString()
                        val sub = selectedJob.toString()

                        Log.d("SignUp", "viewModel: ${viewModel.email}, ${viewModel.password}")

                        scope.launch {
                            val result = saveUser(
                                email = viewModel.email,
                                password = viewModel.password,
                                name = viewModel.name,
                                birthDate = viewModel.birthDate,
                                phoneNumber = viewModel.phoneNumber,
                                role = viewModel.role,
                                major = major,
                                sub = sub
                            )

                            if (result.isSuccess) {
                                navController.navigate(Screen.Login.route)
                                Toast.makeText(context, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = selectedCategory != null && selectedJob != null,
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
            ) {
                Text("회원가입 완료")
            }
        }
    }
}

@Composable
fun DropdownMenuWithFixedTextSize(
    label: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    width: Dp
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .width(width)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = selectedItem ?: "선택",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

suspend fun saveUser(
    email: String,
    password: String,
    name: String,
    birthDate: String,
    phoneNumber: String,
    role: String,
    major: String = "지정 없음",
    sub: String= "지정 없음"
    ): Result<Unit> {
    return try {
        val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("사용자 정보 없음")
        val userId = user.uid
        val db = Firebase.firestore

        val userData = mapOf(
            "name" to name,
            "birthDate" to birthDate,
            "phoneNumber" to phoneNumber,
            "email" to email,
            "role" to role,
            "category" to mapOf(
                "major" to major,
                "sub" to sub
            )
        )

        Log.d("SignUp", "유저데이터: $userData")

        db.collection("users").document(userId).set(userData).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("SignUp", "회원가입 실패: ${e.message}", e)
        Result.failure(e)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    SignUpScreen(navController = rememberNavController())
}
/*

@Preview(showBackground = true)
@Composable
fun PreviewSignUpCommonScreen() {
    val mockViewModel = SignUpModel().apply {
        name = "example"
        email = "test@example.com"
        role = "면접관"
    }

    SignUpCommonScreen(navController = rememberNavController(), viewModel = mockViewModel, role = "면접자")
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpInterviewerScreen() {
    val mockViewModel = SignUpModel().apply {
        name = "example"
        email = "tester@domain.com"
        role = "면접관"
        birthDate = "1995/05/18"
        phoneNumber = "01012345678"
        password = "password123"
    }

    SignUpInterviewerScreen(navController = rememberNavController(), viewModel = mockViewModel)
}*/
