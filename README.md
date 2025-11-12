# Tagmoa

Firebase Realtime Database를 기반으로 대(태그) → 중(메인 테스크) → 소(서브 테스크) 구조의 할 일 관리를 돕는 안드로이드 앱입니다. 태그를 중심으로 업무를 분류하고, 각 메인 테스크/서브 테스크의 일자·색상·중요도를 세분화해서 기록하도록 설계했습니다.

## 구성 개요
- **플랫폼**: Android (minSdk 24, targetSdk 36)
- **언어 / 프레임워크**: Kotlin, AndroidX, Material Components
- **백엔드**: Firebase Realtime Database (+ Analytics, Google Services)
- **주요 모듈**
  - `MainActivity`: 메인 테스크 목록과 태그/테스크 생성 플로우 진입점
  - `TagManagementActivity`: 태그 CRUD 및 메인 테스크 일괄 태깅
  - `AddEditMainTaskActivity`: 메인 테스크 생성/수정 폼
  - `MainTaskDetailActivity`: 메인 테스크 상세 + 서브 테스크 관리
  - `AddEditSubTaskActivity`: 서브 테스크 생성/수정 폼
  - `Models.kt`: Tag/MainTask/SubTask 데이터 클래스

## 구현된 주요 기능
### 태그(Tag)
- 새 태그 생성 시 이름 중복을 제외하고 Firebase `tags` 노드에 저장
- 생성 직후 다이얼로그로 모든 메인 테스크에 선택적 연결 (체크박스로 복수 선택)
- 태그 삭제 시 Firebase에서 제거하고, 모든 메인 테스크의 `tagIds` 배열에서 해당 태그 ID를 정리

### 메인 테스크(Main Task)
- 입력 항목: 제목(필수), 설명, 시작/종료일(선택), 대표 색상 스피너, 태그 멀티 선택
- 컬러/태그/날짜 상태는 XML 리소스(배열)와 다이얼로그를 통해 선택하도록 구성
- 목록 화면에서 RecyclerView + 카드 UI로 제목/태그/설명/색상 스트립을 시각화
- 상세 화면에서 수정·삭제·서브 테스크 추가 진입 버튼 제공, 삭제 시 연결된 서브 테스크까지 일괄 삭제

### 서브 테스크(Sub Task)
- 메인 테스크 스피너 선택 또는 상세 화면에서 해당 테스크 고정 진입
- 입력 항목: 내용(필수), 중요도(낮음/보통/높음 스피너), 시작/종료일(선택)
- RecyclerView 항목에서 내용/일자/중요도 노출 및 수정·삭제 버튼 제공
- 삭제는 서브 테스크만 제거하며, 메인 테스크 본문은 영향 없음

### 공통
- Firebase Realtime Database 경로
  - `tags/{tagId}`
  - `mainTasks/{taskId}`
  - `subTasks/{mainTaskId}/{subTaskId}`
- `google-services.json`을 통해 Firebase 프로젝트와 연동 (이미 `app/google-services.json` 위치에 포함됨)
- `./gradlew lint`로 기본 정적 분석 수행

## 데이터 스키마 예시
```json
{
  "tags": {
    "tagA": { "id": "tagA", "name": "디자인" }
  },
  "mainTasks": {
    "task1": {
      "id": "task1",
      "title": "신규 온보딩",
      "description": "신규 사용자 체험 개선",
      "startDate": 1735689600000,
      "endDate": 1736294400000,
      "dueDate": 1736294400000,
      "mainColor": "#FF6200EE",
      "tagIds": ["tagA", "tagB"]
    }
  },
  "subTasks": {
    "task1": {
      "sub1": {
        "id": "sub1",
        "mainTaskId": "task1",
        "content": "인터뷰 가이드 작성",
        "priority": 2,
        "startDate": 1735257600000,
        "endDate": 1735344000000,
        "dueDate": 1735344000000
      }
    }
  }
}
```

## 개발 환경 설정
1. **Firebase 프로젝트**
   - 콘솔에서 Android 앱을 등록하고 `com.example.tagmoa` 패키지명을 사용합니다.
   - `app/google-services.json`을 다운로드해 동일 경로에 위치시킵니다 (현재 파일이 있다면 자신의 프로젝트 설정으로 교체).
2. **로컬 빌드**
   - JDK 11 이상, Android Studio Koala 이상 버전 권장
   - 의존성 설치: `./gradlew tasks` (필요 시)
   - 정적 분석: `./gradlew lint`
   - 에뮬레이터/디바이스에서 실행: Android Studio “Run” 사용
3. **실행 전 체크리스트**
   - Firebase Database > Realtime Database 활성화 및 읽기/쓰기 규칙 설정
   - 네트워크 권한은 기본 Manifest로 충분 (추가 퍼미션 불필요)

## 화면 흐름
1. **홈(MainActivity)** — 메인 테스크 목록, “메인 테스크 추가” & “태그 관리” 버튼
2. **태그 관리(TagManagementActivity)** — 태그 추가, 전체 리스트, 각 태그별 삭제
3. **메인 테스크 작성(AddEditMainTaskActivity)** — 폼 입력 후 저장
4. **메인 테스크 상세(MainTaskDetailActivity)** — 테스크 정보 + 서브 테스크 리스트, 편집/삭제/추가
5. **서브 테스크 작성(AddEditSubTaskActivity)** — 메인 테스크 지정 후 내용/중요도/일자 입력

## 현재 구현 상태 vs 향후 계획
### ✅ 구현 완료
- Firebase Realtime Database CRUD (태그/메인/서브)
- 태그 생성 시 다중 메인 테스크 연결/삭제 시 일괄 해제 로직
- 메인 테스크 컬러·태그·일자 입력 및 목록/상세 뷰 표현
- 서브 테스크 중요도/일자/내용 관리, 메인 테스크별 중첩 저장 구조
- RecyclerView 어댑터 3종(Main/Tag/Sub) + Material Card UI
- 공통 유틸 (`DateUtils.asDateLabel`) 및 스피너 리스너 래퍼
- Lint 기반 기초 품질 검사 스크립트

### 📌 향후 구현하면 좋은 항목
1. **태그 편집 기능**: 이름 변경 및 다이얼로그 재사용, 연동된 테스크에 실시간 반영
2. **정렬/필터**: 메인 테스크 리스트를 마감일/태그/색상별로 정렬하거나 필터링하는 UX
3. **검색 & 빠른 이동**: 상단 검색창에서 태그/테스크 제목 검색
4. **서브 테스크 완료 상태**: 체크박스로 완료 여부 토글 및 정렬(완료/미완료 분리)
5. **입력 검증 고도화**: 태그 중복 이름 방지, 날짜 범위 포맷 안내, 날짜 유효성 (시작/종료) 추가
6. **오프라인 캐시/로컬 DB**: Room 또는 DataStore를 이용한 임시 저장 및 오프라인 편집
7. **알림 연동**: 마감일 기반 로컬 알림 또는 FCM Push 알림
8. **다크 모드 & 테마 커스터마이징**: 색상 팔레트 확장, 사용자 지정 색상 입력
9. **테스트 자동화**: UI 테스트(Espresso), ViewModel/Repository 분리 및 단위 테스트 도입
10. **다중 사용자 지원**: Firebase Auth 연동으로 사용자별 데이터 분리

## 빌드 & 테스트
```bash
./gradlew lint        # 정적 분석
./gradlew assembleDebug  # 디버그 APK 빌드
```
필요 시 Android Studio에서 `Run > Run 'app'`으로 에뮬레이터/실단말 실행이 가능합니다.

## 참고
- Firebase 규칙을 공개 프로젝트에 맞게 조정해야 합니다 (예: 인증 사용자만 쓰기 가능).
- `google-services.json`에는 민감 정보가 포함되므로 외부 저장소에 업로드 시 주의하세요.
