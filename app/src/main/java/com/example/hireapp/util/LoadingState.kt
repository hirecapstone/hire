package com.example.hireapp.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로딩 상태를 관리하는 전역 object
 */
object LoadingState {
    private val _isLoading = MutableStateFlow(false)
    private val _loadingText = MutableStateFlow("로딩 중입니다...")
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val loadingText: StateFlow<String> = _loadingText.asStateFlow()

    fun show(text: String = "로딩 중입니다...") {
        _loadingText.value = text
        _isLoading.value = true
    }

    fun hide() {
        _isLoading.value = false
    }
}

@Composable
fun GlobalLoadingScreen() {
    val isLoading = LoadingState.isLoading.collectAsState().value
    val loadingText = LoadingState.loadingText.collectAsState().value

    if (isLoading) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(loadingText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
