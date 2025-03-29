package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun WarningScreen(onNext: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "주의사항", // 주의사항 내용 작성
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it }
            )
            Text("주의사항을 모두 확인했습니다.")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = checked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다음")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WarningScreenPreview() {
    WarningScreen(onNext = {})
}
