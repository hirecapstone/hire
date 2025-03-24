package com.example.hireapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("com/example/hireapp/ui/screens/login")
    object SignUp : Screen("com/example/hireapp/ui/screens/signup")
    object HomeAppl : Screen("com/example/hireapp/ui/screens/home_appl")
    object HomeIntr : Screen("com/example/hireapp/ui/screens/home_intr")
    object MyPageAppl : Screen("com/example/hireapp/ui/screens/mypage_appl")
    object MyPageIntr : Screen("com/example/hireapp/ui/screens/mypage_intr")
    object Capture : Screen("com/example/hireapp/ui/screens/capture")
    object SignUpCommon : Screen("com/example/hireapp/ui/screens/signup_common")
    object SignUpInterviewer : Screen("com/example/hireapp/ui/screens/signup_interviewer")
}