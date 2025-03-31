# hire
2025-1 하이어 캡스톤 디자인 (면접자 촬영 프론트 파트)

## 주요 구현 기능
- SelectRoleScreen: 대분류/세부직군 선택, 이력서(텍스트 필드로 임시 구성)
- WarningScreen: 주의사항 확인 및 체크박스
- CameraSetupScreen: 전면 카메라 미리보기, 카메라/마이크 권한 요청
- QuestionScreen:
    - 총 9개의 질문 (기본 3 + AI 생성 6)
    - 각 질문마다 준비시간 30초 → 답변시간 60초
    - 답변 시간에만 영상 녹화
    - 질문 종료 후 저장 여부 팝업

## 영상 저장 방식
- 내부 저장소 `cacheDir`에 `.mp4` 파일로 저장  
  → 예시: `session_17234234_q1.mp4`
- "아니오" 선택 시 해당 영상들 삭제
- (※ 현재는 앱 내부 저장, 아직 Firebase 연동 X)

## 촬영 플로우
SelectRoleScreen → WarningScreen → CameraSetupScreen → QuestionScreen