# Parkinson Killer Native

Parkinson Killer의 Android Native 버전입니다.

이 레포는 기존 Expo MVP를 보존한 뒤, Kotlin + Jetpack Compose 기반으로 새로 개발하기 위한 공간입니다.

## 기술 방향

- Kotlin
- Jetpack Compose
- Material 3
- Android NotificationManager
- Local-first session state
- Foreground Service / AlarmManager 확장 준비

## Expo 박물관 백업

기존 Expo MVP는 아래 브랜치에 보존되어 있습니다.

https://github.com/jengjunseo/parkinson-killer/tree/museum/expo-mvp

## 현재 목표

첫 네이티브 프로토타입은 전체 기능 복제가 아니라 다음 흐름을 검증합니다.

1. 목표 입력
2. 시간 입력
3. 실패 메시지 입력
4. 집중 세션 시작
5. 전체 화면 타이머
6. 지속 알림
7. 완료/실패 화면

## 개발 메모

처음에는 작게 갑니다. 복잡한 레이어, 과한 아키텍처, 불필요한 추상화는 넣지 않습니다.
