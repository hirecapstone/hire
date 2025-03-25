package com.example.hireapp.screens.applicant

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen

@Composable
fun BottomNavigationAppl(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = false,
            onClick = { navController.navigate(Screen.HomeAppl.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "촬영") },
            label = { Text("촬영") },
            selected = false,
            onClick = { navController.navigate(Screen.Capture.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "마이페이지") },
            label = { Text("마이페이지") },
            selected = false,
            onClick = { navController.navigate(Screen.MyPageAppl.route) }
        )
    }
}
