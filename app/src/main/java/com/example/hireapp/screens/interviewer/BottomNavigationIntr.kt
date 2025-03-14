package com.example.hireapp.screens.interviewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.hireapp.navigation.Screen

@Composable
fun BottomNavigationIntr(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "홈") },
            label = { Text("홈") },
            selected = false,
            onClick = { navController.navigate(Screen.HomeIntr.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "마이페이지") },
            label = { Text("마이페이지") },
            selected = false,
            onClick = { navController.navigate(Screen.MyPageIntr.route) }
        )
    }
}
