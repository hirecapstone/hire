package com.example.hireapp.screens.applicant.interviewcapture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WarningScreen(fromInsert: Boolean = false, onNext: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.example.hireapp.R.drawable.warn),
                contentDescription = "경고 이미지",
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "주의사항",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // 여러 문장의 주의사항을 출력
        Spacer(modifier = Modifier.height(24.dp))
        Text("1. 조명이 잘 맞춰져 있는지 확인하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("2. 배경이 깔끔한지 확인하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("3. 카메라와 얼굴의 거리가 적절한지 확인하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("4. 주변 소음을 최소화하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("5. 면접 질문지를 준비하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("6. 카메라의 배터리 상태를 확인하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("7. 인터넷 연결 상태를 확인하세요.",
            fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("8. 면접 중 방해받지 않도록 주변에 알리세요.",
            fontSize = 18.sp)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it }
            )
            Text("주의사항을 모두 확인했습니다.")
        }

        Button(
            onClick = onNext,
            enabled = checked,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        ) {
            Text("다음")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WarningScreenPreview() {
    WarningScreen(onNext = {})
    WarningScreen(fromInsert = false, onNext = {})
}