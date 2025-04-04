package com.example.hireapp.navigation

sealed class Screen(val route: String) {
    // 공통
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object SignUpCommon : Screen("signup_common")
    object SignUpInterviewer : Screen("signup_interviewer")
    object VideoDetail : Screen("video_detail")

    // 면접자 (Applicant)
    object HomeAppl : Screen("home_appl")
    object MyPageAppl : Screen("mypage_appl")
    object Capture : Screen("capture")

    // 면접관 (Interviewer)
    object HomeIntr : Screen("home_intr")
    object MyPageIntr : Screen("mypage_intr")
}