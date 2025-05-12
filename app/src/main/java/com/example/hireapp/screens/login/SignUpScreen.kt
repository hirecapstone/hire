package com.example.hireapp.screens.login

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
                            contentDescription = "면접자 아이콘",
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
                            contentDescription = "면접관 아이콘",
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
fun SignUpCommonScreen(navController: NavController, role: String) {
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.hireapp.R.drawable.register),
                    contentDescription = "등록 아이콘",
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

                Firebase.auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = Firebase.auth.currentUser
                            val userId = user?.uid ?: ""
                            val db = Firebase.firestore
                            val userData = hashMapOf(
                                "name" to name,
                                "birthDate" to birthDate,
                                "phoneNumber" to phoneNumber,
                                "email" to email,
                                "role" to role
                            )

                            db.collection("users").document(userId).set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                    if (role == "면접자") {
                                        navController.navigate("login")
                                    } else {
                                        navController.navigate("login") //signup_interviewer 오류나서 login으로 수정
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "회원가입에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "회원가입에 실패했습니다: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
fun SignUpInterviewerScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedJob by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val user = Firebase.auth.currentUser
    val userId = user?.uid ?: ""

    val categories = listOf(
        "IT/소프트웨어",
        "디자인",
        "마케팅",
        "영업",
        "금융",
        "인사",
        "의료",
        "교육",
        "제조",
        "법률"
    )

    val subJobsMap = mapOf(
        "IT/소프트웨어" to listOf("프로그래머", "데이터 엔지니어", "AI 전문가"),
        "디자인" to listOf("UX 디자이너", "그래픽 디자이너", "영상 편집자"),
        "마케팅" to listOf("디지털 마케터", "브랜드 매니저", "SEO 전문가"),
        "영업" to listOf("B2B 영업", "해외 영업", "세일즈 매니저"),
        "금융" to listOf("회계사", "재무 분석가", "투자 컨설턴트"),
        "인사" to listOf("채용 담당자", "HRBP", "교육 담당자"),
        "의료" to listOf("의사", "간호사", "물리치료사"),
        "교육" to listOf("교사", "강사", "콘텐츠 제작자"),
        "제조" to listOf("기계 엔지니어", "전자 엔지니어", "품질 전문가"),
        "법률" to listOf("변호사", "법무사", "컨설턴트")
    )

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
                        val db = Firebase.firestore
                        val interviewerData = mapOf(
                            "category" to selectedCategory,
                            "job" to selectedJob
                        )

                        db.collection("users").document(userId).update(interviewerData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "회원가입에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                enabled = selectedCategory != null && selectedJob != null,
                modifier = Modifier.width(200.dp).height(48.dp)
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

@Preview(showBackground = true)
@Composable
fun PreviewSignUpScreen() {
    SignUpScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpCommonScreen() {
    SignUpCommonScreen(navController = rememberNavController(), role = "면접자")
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUpInterviewerScreen() {
    SignUpInterviewerScreen(navController = rememberNavController())
}