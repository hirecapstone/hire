package com.example.hireapp.screens.applicant.interviewcapture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.camera.core.Preview as CameraPreview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson

@Composable
fun CameraSetupScreen(navController: NavController,
                      fromInsert: Boolean = false,
                      questionsJson: String = "[]",
                      onNext: () -> Unit = {}
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // 2) 프리뷰 높이 비율 (예: 60%)
    val previewHeight = screenHeight * 0.6f
    val uiHeight = screenHeight - previewHeight
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    // 카메라, 녹음 권한 요청
    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        } else {
            hasPermission = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 카메라 미리보기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.6f),
            contentAlignment = Alignment.Center
        ) {
            if (hasPermission) {
                CameraPreviewView(Modifier.fillMaxSize())
            } else {
                Text("카메라 및 마이크 권한이 필요합니다")
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth().height(uiHeight)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(com.example.hireapp.R.drawable.setting),
                        contentDescription = null,
                        Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("카메라 세팅", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Text("세팅이 완료되었으면 촬영 시작 버튼을 누르세요")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (fromInsert) {
                            val gson = Gson()
                            val questions =
                                gson.fromJson(questionsJson, Array<String>::class.java).toList()
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "questions",
                                ArrayList(questions)
                            )
                            navController.navigate("check_question")
                        } else {
                            onNext()
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = hasPermission
                ) {
                    Text("촬영 시작")
                }
            }
        }
    }
}


@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = CameraPreview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // 전면 카메라 사용
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))
    }
}

@Preview(showBackground = true)
@Composable
fun CameraSetupScreenPreview() {
    CameraSetupScreen(navController = rememberNavController(),
        fromInsert = false,
        questionsJson = "[]"
    )
}