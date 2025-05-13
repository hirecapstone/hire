package com.example.hireapp.navigation

sealed class Screen(val route: String) {
    // 공통
    object Login : Screen("login")
    object SignUp : Screen("sign_up")
    object SignUpCommon : Screen("sign_up_common")
    object SignUpInterviewer : Screen("sign_up_interviewer")
    object VideoDetail : Screen("video_detail")

    // 면접자 (Applicant)
    object HomeAppl : Screen("home_appl")
    object MyPageAppl : Screen("mypage_appl")
    object Capture : Screen("capture")
    object Capture2 : Screen("capture2")
    object CaptureOption : Screen("capture_option")
    object InsertQuestion : Screen("insert_question")
    object CheckQuestion : Screen("check_question")
    object Warning : Screen("warning")
    object CameraSetup : Screen("camera_setup")

    // 면접관 (Interviewer)
    object HomeIntr : Screen("home_intr")
    object MyPageIntr : Screen("mypage_intr")

    // 추가된 QuestionScreen 경로
    object QuestionScreen : Screen("question_screen")
}