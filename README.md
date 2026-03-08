# Namey
> AI-powered function naming suggestion plugin for IntelliJ IDEA

함수 바디를 작성하면 AI가 적절한 함수명을 추천해주는 IntelliJ 플러그인입니다.

---

## 왜 만들었나

함수 로직을 다 짜놓고 이름 앞에서 멈추는 경험, 다들 있을 거라 생각합니다.  
`temp()`, `doSomething()`, `process()` 로 일단 넘기고 나중에 고치려다 그냥 두는 것도요.

네이밍에 쏟는 인지 에너지를 줄이고 로직에 집중하고 싶어서 만들었습니다.

(사실 직장 개발자분들의 사소한 고민 대화에서 아이디어 가져다 해봤습니다)

---

## 어떻게 동작하나

```
함수 위에서 Alt+N
    ↓
현재 함수 상태 기준으로 AI가 이름 추천
    ↓
선택하면 호출부까지 자동 리네임
```

함수 바디가 없어도 됩니다. 직접 설명을 입력하면 그걸 기반으로 추천합니다.

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
// ★★★ findActiveOrdersByUserInPeriod
// ★★☆ getOrderHistoryByUserBetweenDates
// ★★☆ fetchNonCancelledOrdersForUser
```

---

## 기술 스택

- **언어**: Kotlin
- **플랫폼**: IntelliJ Plugin SDK
- **AI**: Gemini Flash (무료 티어)
- **빌드**: Gradle Kotlin DSL

---

## 프로젝트 구조

```
com.namey/
├── domain/          # 순수 비즈니스 로직, 플랫폼 의존 없음
│   ├── model/       # FunctionContext, NameSuggestion, SuggestionResult
│   └── port/        # NamingPort, CodeContextPort (interface)
├── usecase/         # SuggestNameUseCase
├── infra/
│   ├── gemini/      # Gemini API 구현체
│   └── intellij/    # PSI 컨텍스트 추출
└── presentation/
    └── intellij/    # Action, Popup, Settings
```

AI 엔진과 IDE 플랫폼을 인터페이스로 추상화했습니다.  
Gemini → Claude / GPT 교체, IntelliJ → VS Code 확장 모두 구현체만 바꾸면 됩니다.
추상화 연습하며 짜 본 것이니,, 좀 더 나은 방향이 보인다면 언제든 코멘트 주세요!

---

## 설치

1. [Releases](../../releases) 에서 최신 `.zip` 다운로드
2. IntelliJ → Settings → Plugins → ⚙️ → Install Plugin from Disk
3. Settings → Tools → **Namey** → Gemini API Key 입력

> Gemini API Key 무료 발급: [aistudio.google.com](https://aistudio.google.com)

---

## 사용법

| 상황 | 방법 |
|------|------|
| 함수 바디 있음 | 함수 위에서 `Alt+N` → 바로 추천 |
| 함수 바디 없음 | `Alt+N` → 설명 직접 입력 → 추천 |
| 단축키 변경 | Settings → Keymap → "Namey" 검색 |

---

## 개발 배경

이 프로젝트는 두 가지 목적으로 시작했습니다.

하나는 실제로 쓸 수 있는 도구를 만드는 것이고,  
다른 하나는 Java 백그라운드에서 Kotlin을 제대로 익히는 것입니다.

Kotlin의 `data class`, `sealed class`, `coroutine`, `extension function` 등  
언어적 강점을 의도적으로 활용하는 방향으로 설계했습니다. 코틀린 공부할 겸.. 이라고 했다가 본격적이 된..

---

## 확장 계획

- [ ] VS Code Extension
- [ ] Kotlin / Python 언어 지원
- [ ] 팀 네이밍 패턴 학습 (RAG)
- [ ] AI 엔진 선택 (Gemini / Claude / Groq / Ollama)

---

## 기여

이슈, PR 모두 환영합니다.

---

## 라이선스

MIT
