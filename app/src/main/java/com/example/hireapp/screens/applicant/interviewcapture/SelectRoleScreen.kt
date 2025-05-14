package com.example.hireapp.screens.applicant.interviewcapture

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hireapp.data.Category
import com.example.hireapp.data.subCategory
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun SelectRoleScreen(onNext: (String, String, String) -> Unit) { // 세 개의 매개변수로 수정
    val categories = Category.entries.map { c -> c.label}
    val jobMap = subCategory

    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubcategory by remember { mutableStateOf("") }

    var contact by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var career by remember { mutableStateOf("") }
    var achievements by remember { mutableStateOf("") }
    var certificates by remember { mutableStateOf("") }
    var projects by remember { mutableStateOf("") }
    var roles by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownMenuBox("대분류", categories, selectedCategory) {
            selectedCategory = it
            selectedSubcategory = ""
        }

        DropdownMenuBox("세부 직군", jobMap[selectedCategory] ?: emptyList(), selectedSubcategory) {
            selectedSubcategory = it
        }

        Text("1. 개인정보", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("연락처(이메일, 전화번호)") }, modifier = Modifier.fillMaxWidth())

        Text("2. 학력", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = major, onValueChange = { major = it }, label = { Text("전공") }, modifier = Modifier.fillMaxWidth())

        Text("3. 경력", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = career, onValueChange = { career = it }, label = { Text("직무 및 담당 업무") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = achievements, onValueChange = { achievements = it }, label = { Text("주요 성과") }, modifier = Modifier.fillMaxWidth())

        Text("4. 자격증", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = certificates, onValueChange = { certificates = it }, label = { Text("관련 자격증") }, modifier = Modifier.fillMaxWidth())

        Text("5. 프로젝트 및 경험", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = projects, onValueChange = { projects = it }, label = { Text("수행한 프로젝트") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = roles, onValueChange = { roles = it }, label = { Text("역할 및 기여한 부분") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val db = Firebase.firestore
                val sessionId = "session_${System.currentTimeMillis()}" // 고유 세션 ID 생성
                val data = hashMapOf(
                    "category" to selectedCategory,
                    "job" to selectedSubcategory,
                    "contact" to contact,
                    "major" to major,
                    "career" to career,
                    "achievements" to achievements,
                    "certificates" to certificates,
                    "projects" to projects,
                    "roles" to roles,
                    "sessionId" to sessionId // 세션 ID 추가
                )

                db.collection("select_role")
                    .document(sessionId)
                    .set(data)
                    .addOnSuccessListener {
                        onNext(selectedCategory, selectedSubcategory, sessionId) // 세션 ID 전달
                    }
                    .addOnFailureListener { e -> Log.w("Firebase", "Error adding document", e) }
            },
            enabled = selectedCategory.isNotEmpty() && selectedSubcategory.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다음")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectRoleScreenPreview() {
    SelectRoleScreen(onNext = { _, _, _ -> }) // 세 개의 매개변수로 구현
}