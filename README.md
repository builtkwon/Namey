# Namey
> AI 기반 함수명 추천 IntelliJ 플러그인

함수 바디를 작성하면 AI가 적절한 함수명 3개를 추천해주는 IntelliJ 플러그인입니다.  
`Alt+N` 한 번으로 네이밍 고민을 줄이고 로직에 집중할 수 있습니다.

---

## 왜 만들었나

함수 로직을 다 짜놓고 이름 앞에서 멈추는 경험, 다들 있을 거라 생각합니다.  
`temp()`, `doSomething()`, `process()`로 일단 넘기고 나중에 고치려다 그냥 두는 것도요.

네이밍에 쏟는 인지 에너지를 줄이고 로직에 집중하고 싶어서 만들었습니다.

---

## 어떻게 동작하나

```
함수 위에서 Alt+N
    ↓
PSI로 함수 컨텍스트 추출 (파라미터 타입·반환 타입·바디·클래스명)
    ↓
바디 없으면 → 설명 직접 입력 다이얼로그
    ↓
Gemini Flash API 호출 (비동기)
    ↓
추천 3개 팝업 (별점 + 한국어 이유)
    ↓
선택 → PSI RenameProcessor로 호출부까지 자동 리네임
```

```java
// 이런 상태에서 Alt+N 눌러도
public List<Order> temp(Long userId, LocalDate from, LocalDate to) {
    return orderRepository.findByUserIdAndCreatedAtBetween(userId, from, to)
        .stream()
        .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
        .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
        .collect(Collectors.toList());
}

// 이런 추천이 나옵니다
// ★★★  findActiveOrdersByUserInPeriod    — 취소되지 않은 주문을 기간 필터로 조회하는 의미가 명확
// ★★☆  getOrderHistoryByUserBetween      — 범위 쿼리임을 표현하나 상태 필터 의미 누락
// ★★☆  fetchNonCancelledOrdersForUser    — 상태 필터는 명확하지만 기간 정보 누락
```

---

## 기술 스택

| 항목 | 선택 | 이유 |
|------|------|------|
| 언어 | Kotlin | data class·sealed class·coroutine 활용 |
| 플랫폼 | IntelliJ Plugin SDK 2023.2+ | |
| AI | Gemini 1.5 Flash | 무료 티어, 충분한 응답 속도 |
| HTTP | `java.net.http.HttpClient` (JDK 17) | 외부 의존성 최소화 |
| JSON | `kotlinx.serialization` | Kotlin 네이티브 |
| 비동기 | `kotlinx.coroutines` | UI 블로킹 방지 |
| API Key 저장 | IntelliJ `PasswordSafe` | 운영체제 키체인 연동, 보안 |
| 빌드 | Gradle Kotlin DSL | |
| CI/CD | GitHub Actions | 태그 푸시 시 `.zip` 자동 빌드 및 릴리스 |

---

## 프로젝트 구조

```
com.namey/
├── domain/
│   ├── model/
│   │   ├── FunctionContext.kt      # 함수명·파라미터·반환타입·바디·클래스명·언어
│   │   ├── NameSuggestion.kt       # 추천명(camelCase) + 별점(1~3) + 한국어 이유
│   │   └── SuggestionResult.kt     # sealed: Success / Failure(FailureType)
│   └── port/
│       └── NamingPort.kt           # suspend fun suggest(ctx, count): SuggestionResult
├── usecase/
│   └── SuggestNameUseCase.kt       # NamingPort 호출, 바디 없는 경우 description 주입
├── infra/
│   ├── gemini/
│   │   ├── GeminiNamingAdapter.kt  # Gemini Flash API 호출·응답 파싱·에러 분류
│   │   └── GeminiResponseParser.kt # JSON 추출 (마크다운 코드블록 제거 포함)
│   └── intellij/
│       └── PsiCodeContextAdapter.kt # PSI → FunctionContext 변환 (Java·Kotlin 지원)
└── presentation/
    └── intellij/
        ├── SuggestNameAction.kt    # Alt+N 진입점, 예외 케이스 조기 반환
        ├── SuggestionPopup.kt      # JBPopupFactory 팝업, override 경고 배너
        ├── NameySettings.kt        # PasswordSafe 기반 API Key 관리
        └── NameyConfigurable.kt    # Settings > Tools > Namey UI
```

> `CodeContextPort`를 domain/port에 두지 않은 이유:  
> PSI는 IntelliJ SDK에 종속되어 플랫폼 중립적인 인터페이스를 정의할 수 없습니다.  
> AI 엔진 교체 추상화(`NamingPort`)가 핵심 목적이므로 PSI 추출은 infra 구체 클래스로 위치시켰습니다.

---

## 도메인 모델 상세

### FunctionContext
```kotlin
data class FunctionContext(
    val currentName: String,                  // 현재 함수명 (temp, doSomething 등)
    val parameters: List<Parameter>,          // 파라미터 목록
    val returnType: String,                   // 반환 타입 (void, List<Order> 등)
    val body: String?,                        // 함수 바디 코드 (null = 바디 없음)
    val description: String?,                 // 사용자 직접 입력 설명 (body null일 때)
    val containingClassName: String?,         // 포함 클래스명
    val parentInterface: String?,             // override 시 부모 인터페이스명
    val receiverType: String?,                // Kotlin extension function 수신 타입
    val language: Language,                   // JAVA, KOTLIN
) {
    data class Parameter(val name: String, val type: String)
    enum class Language { JAVA, KOTLIN }

    // body와 description 둘 다 null·blank이면 컨텍스트 불충분
    fun hasContext(): Boolean = !body.isNullOrBlank() || !description.isNullOrBlank()
}
```

### SuggestionResult
```kotlin
sealed class SuggestionResult {
    data class Success(val suggestions: List<NameSuggestion>) : SuggestionResult()
    data class Failure(val type: FailureType, val message: String) : SuggestionResult()

    enum class FailureType {
        API_KEY_MISSING,   // API Key 미설정
        NETWORK_ERROR,     // HTTP 4xx/5xx, 타임아웃
        RATE_LIMITED,      // HTTP 429
        PARSE_ERROR,       // 응답 JSON 파싱 실패
        UNKNOWN
    }
}
```

---

## PSI 예외 케이스 처리 방침

| 케이스 | 처리 | 토스트 메시지 |
|--------|------|--------------|
| 함수 바깥에 커서 | 조기 반환 | `"함수 안에 커서를 놓아주세요"` |
| 생성자 (`constructor`) | 조기 반환 | `"생성자는 지원하지 않습니다"` |
| 람다 / 익명 함수 | 조기 반환 | `"람다/익명 함수는 지원하지 않습니다"` |
| `override` 함수 | **지원** + 경고 배너 | 팝업 상단에 `"⚠️ 부모 메서드도 함께 변경됩니다"` 표시 후 진행. `parentInterface`를 FunctionContext에 포함해 AI 컨텍스트 보강. PSI RenameProcessor가 부모·구현체 일괄 처리 |
| Kotlin extension function | **지원** | `receiverType`을 FunctionContext에 포함해 AI에 전달 |
| API Key 미설정 | Settings 안내 | Settings > Tools > Namey 페이지로 자동 이동 |

---

## 에러 처리 전략

| 상황 | 처리 |
|------|------|
| HTTP 429 | `RATE_LIMITED` — `"Gemini 요청 한도 초과. 잠시 후 다시 시도해주세요"` |
| HTTP 4xx | `NETWORK_ERROR` — `"API 요청 실패 (상태 코드: {code})"` |
| HTTP 5xx / 타임아웃 | `NETWORK_ERROR` — `"네트워크 오류. 연결을 확인해주세요"` |
| JSON 파싱 실패 | 마크다운 코드블록(` ```json `) 제거 후 재파싱 → 그래도 실패 시 `PARSE_ERROR` |
| 빈 배열 응답 | `PARSE_ERROR` — `"AI가 추천을 생성하지 못했습니다. 함수 바디를 보완해주세요"` |

---

## Gemini 프롬프트

```
당신은 Java/Kotlin 함수 네이밍 전문가입니다.
아래 함수에 적합한 이름 {count}개를 추천해주세요.

규칙:
- 영문 camelCase, 반드시 동사로 시작
- 각 추천에 별점(1~3점)과 한국어 이유 한 줄 포함
- 반드시 아래 JSON 배열 형식으로만 응답 (다른 텍스트 절대 금지)

[{"name":"...", "stars":3, "reason":"..."}]

[함수 정보]
- 언어: {JAVA|KOTLIN}
- 클래스: {containingClassName}
- 파라미터: {param1: Type1, param2: Type2}
- 반환 타입: {returnType}
{- 수신 타입: {receiverType}}     ← extension function일 때만 포함
{- 부모 인터페이스: {parentInterface}}  ← override일 때만 포함
- 바디/설명:
{body or description}
```

---

## 테스트 전략

IntelliJ SDK 없이 실행 가능한 순수 단위 테스트만 작성합니다.

```
src/test/kotlin/com/namey/
├── domain/model/
│   └── FunctionContextTest.kt
│       ├── hasContext() — body·description 조합별 반환값 검증
│       └── Parameter — name·type 빈 문자열 허용 확인
├── usecase/
│   └── SuggestNameUseCaseTest.kt
│       ├── NamingPort 정상 응답 → Success 그대로 반환
│       ├── NamingPort Failure 반환 → Failure 그대로 반환
│       └── description만 있는 FunctionContext → NamingPort에 정상 전달
└── infra/gemini/
    └── GeminiResponseParserTest.kt
        ├── 순수 JSON 배열 파싱
        ├── ```json 마크다운 블록 포함 파싱
        ├── stars 범위 외 값(0, 4) → 1~3으로 클램핑
        ├── stars 필드 누락 → 기본값 1
        ├── name 빈 문자열 항목 → 결과에서 필터링
        └── 빈 배열 [] → PARSE_ERROR
```

---

## 설치

1. [Releases](../../releases) 에서 최신 `.zip` 다운로드  
   (GitHub Actions가 태그 푸시 시 자동 빌드)
2. IntelliJ → Settings → Plugins → ⚙️ → **Install Plugin from Disk**
3. Settings → Tools → **Namey** → Gemini API Key 입력

> Gemini API Key 무료 발급: [aistudio.google.com](https://aistudio.google.com)

---

## 사용법

| 상황 | 방법 |
|------|------|
| 함수 바디 있음 | 함수 안에 커서 → `Alt+N` → 바로 추천 |
| 함수 바디 없음 | `Alt+N` → 설명 직접 입력 → 추천 |
| override 함수 | `Alt+N` → 경고 확인 후 선택 → 부모 포함 일괄 리네임 |
| 단축키 변경 | Settings → Keymap → "Namey" 검색 |

---

## CI/CD

`v*` 태그 푸시 시 GitHub Actions가 자동으로 플러그인 `.zip`을 빌드하고 GitHub Releases에 업로드합니다.

```
git tag v1.0.0 && git push origin v1.0.0
```

---

## 확장 계획

- [ ] VS Code Extension (NamingPort 구현체 교체만으로 대응 가능)
- [ ] Kotlin / Python 언어 지원
- [ ] AI 엔진 선택 (Claude / Groq / Ollama — NamingPort 구현체 추가)
- [ ] 팀 네이밍 패턴 학습 (RAG)

---

## 개발 배경

이 프로젝트는 두 가지 목적으로 시작했습니다.

하나는 실제로 쓸 수 있는 도구를 만드는 것이고,  
다른 하나는 Java 백그라운드에서 Kotlin을 제대로 익히는 것입니다.

Kotlin의 `data class`, `sealed class`, `coroutine`, `extension function` 등  
언어적 강점을 의도적으로 활용하는 방향으로 설계했습니다.

---

## 기여

이슈, PR 모두 환영합니다.

---

## 라이선스

MIT
