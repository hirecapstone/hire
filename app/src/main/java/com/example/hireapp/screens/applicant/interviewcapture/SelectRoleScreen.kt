package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SelectRoleScreen(onNext: () -> Unit) {
    val categories = listOf("기업", "공무원", "교육", "대학")
    val jobMap = mapOf(
        "기업" to listOf("IT", "디자인", "경영/사무", "생산/기술"),
        "공무원" to listOf("7,9급", "경찰", "소방", "군무원"),
        "교육" to listOf("초·중등교사", "유치원교사", "강사"),
        "대학" to listOf("학부 입시", "편입", "대학원")
    )

    var selectedCategory by remember { mutableStateOf("") }
    var selectedSubcategory by remember { mutableStateOf("") }
    var resumeText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 대분류
        DropdownMenuBox(
            label = "대분류",
            options = categories,
            selectedOption = selectedCategory,
            onOptionSelected = {
                selectedCategory = it
                selectedSubcategory = ""
            }
        )

        // 세부 직군
        DropdownMenuBox(
            label = "세부 직군",
            options = jobMap[selectedCategory] ?: emptyList(),
            selectedOption = selectedSubcategory,
            onOptionSelected = { selectedSubcategory = it }
        )

        // 정보입력칸, 텍스트 필드로 구성
        OutlinedTextField(
            value = resumeText,
            onValueChange = { resumeText = it },
            label = { Text("이력 정보") },
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp) // 입력 필드 크기 확장
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
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
    SelectRoleScreen(onNext = {})
}