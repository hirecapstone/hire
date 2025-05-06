# hire
2025-1 하이어 캡스톤 디자인

#신규파일
1. CaptureOptionScreen.kt: 촬영옵션 선택화면(질문생성,질문입력 중 선택)
2. InsertQuestionScreen.kt: 질문입력화면
3. CheckQuestionScreen.kt: 입력한 질문 촬영화면

#수정파일
1. build.gradle.kts: mediapipe의존성 추가
2. QuestionScreen.kt: mediapipe분석추가
 분석한 값은 interview_mediapipe컬렉션에 저장됩니다.
 badposture: 구부정한 자세
 notfront: 정면아님
 smile: 미소
 count: 변화가 일어난 횟수
 timestamps: 변화가 일어난 시간대(초)
3. HomeApplScreen.kt: 촬영->CaptureOptionScreen으로 이동으로 수정
4. Navigation.kt: 신규스크린경로추가
5. Screen.kt: 신규스크린추가
