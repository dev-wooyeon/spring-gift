# ADR-0011: 도메인 내부 계층 분류를 표준화하고 로컬 support 패키지를 제거한다

## 상태

Accepted

## 날짜

2026-05-29

## 맥락

우리는 단일 모듈 내에서 각 도메인(Bounded Context) 하위에 표준 4대 레이어(`presentation`, `application`, `domain`, `infrastructure`)를 두어 모듈러 모놀리스를 구현했다.
이 방식은 **의존성 방향의 단방향 흐름**을 자동 검증(`DomainArchitectureTest`)하고 코드의 **응집도를 극대화**하는 매우 일관된 아키텍처이다.

그러나 `auth` 도메인은 여전히 예외적으로 `support`라는 임시 로컬 패키지를 두어 다음과 같은 클래스들을 포함하고 있다.
- `AuthenticationResolver`: HTTP Authorization 헤더를 추출하여 인증된 회원을 식별하는 컴포넌트
- `JwtProvider`: JWT 토큰을 암호화/복호화하여 서명하는 기술적 유틸리티
- `KakaoLoginProperties`: 외부 카카오 OAuth 설정을 관리하는 프로퍼티 클래스

로컬 `support` 폴더의 존재는 표준 레이어 분류 원칙의 **일관성**을 위배하며, 제3자가 특정 기능의 소속 위치를 예측할 때 혼선을 준다. 따라서 이들을 표준 레이어로 재배치하여 패키지 구조의 직관성을 완성해야 한다.

## 결정

1. `gift.auth.support` 패키지를 **완전히 제거**한다.
2. 소속 클래스들을 본연의 아키텍처 역할에 맞추어 표준 레이어로 다음과 같이 재배치한다:
   - **`AuthenticationResolver` ➔ `gift.auth.presentation`**: HTTP 헤더 파싱 및 컨트롤러와 직접 연계되는 작업은 표현 계층(presentation)의 고유 책임이다.
   - **`JwtProvider` ➔ `gift.auth.infrastructure`**: 토큰 서명 및 암호화 처리는 전형적인 외부 라이브러리 연동 및 기술 지원을 수행하는 인프라 계층(infrastructure)의 책임이다.
   - **`KakaoLoginProperties` ➔ `gift.auth.infrastructure`**: 외부 API 설정 정보 바인딩은 기술 구성 요소이므로 인프라 계층(infrastructure)에 속하는 것이 직관적이다.
3. 이에 맞추어 전체 임포트 경로를 수정하고 테스트 코드 또한 정비한다.

## 트레이드오프

### 장점 (Pros)
- **완벽한 레이어 일관성**: 도메인 내부의 레이어 구분이 `presentation / application / domain / infrastructure`로 완전히 통일된다. 애매모호한 로컬 `support` 폴더가 사라져 인지 부하가 소멸한다.
- **아키텍처 회귀 검증의 명료성**: ArchUnit 규칙이 기술 컴포넌트들(`JwtProvider`, `Properties`)의 위치를 명확히 인지하게 되어 의존 규칙 검증이 훨씬 더 직관적이고 쉬워진다.

### 단점 (Cons)
- **클래스 경로 변경에 따른 수정**: 일부 컨트롤러 및 설정 클래스의 임포트 구문을 교정해야 한다.

## 결과

- `spring-gift` 모듈러 모놀리스 아키텍처는 이제 모든 Bounded Context에 대해 **단 하나의 오차도 없이 일관된 패키지 분류 체계**를 가지게 되었다.
- 훗날 제3자가 유지보수할 때, 인증 헤더 파싱 기능은 당연히 `presentation` 폴더에 있을 것이고, 암호화 유틸은 `infrastructure` 폴더에 있을 것이라고 100% 직관적으로 예측하고 즉시 찾아낼 수 있다.
