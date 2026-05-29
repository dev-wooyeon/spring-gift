# ADR-0013: Spring Boot 3.x Declarative HTTP Interfaces 도입

## 맥락 (Context)
현재 `spring-gift` 프로젝트는 외부 카카오 API(액세스 토큰 발급 및 사용자 정보 조회)와의 연동을 위해 인프라스트럭처 레이어의 `KakaoLoginClient`에서 스프링의 `RestClient`를 명시적으로 조작하고 있습니다.
이 방식은 동작에는 문제가 없으나 다음과 같은 한계를 가집니다.
1. **절차지향적 HTTP 명세 제어**: HTTP 요청의 세부 설정(URL 빌딩, 헤더 셋팅, 파라미터 매핑)이 Java 코드 형태로 절차지향적으로 나열되어 비즈니스 흐름을 파악하기 어렵게 만듭니다.
2. **낮은 재사용성**: 외부 API가 늘어날 때마다 매번 `RestClient` 통신 보일러플레이트 코드를 작성해야 합니다.

선언적(Declarative) HTTP 클라이언트를 사용하면, 외부 API의 명세를 인터페이스 정의와 애노테이션 선언만으로 자동 캡슐화할 수 있어 설계 제1원칙인 **일관성**과 **직관성**을 극대화할 수 있습니다.

## 결정 (Decision)
의존성이 무거운 Spring Cloud OpenFeign 라이브러리를 추가하는 대신, **Spring Boot 3.5.9의 순수 코어 사양인 `Http Interfaces` 스펙(Spring Framework 6.0+)을 전격 도입**하기로 결정합니다.

- **`KakaoApi` 인터페이스 정의**: 카카오 외부 통신 스펙을 `@HttpExchange`, `@PostExchange`, `@GetExchange` 애노테이션으로 완벽히 선언하여 직관적으로 명세화합니다.
- **`HttpClientConfig` 설정 구현**: 스프링 기본 제공 `RestClientAdapter`와 `HttpServiceProxyFactory`를 사용하여 인터페이스 프록시 빈을 자동으로 빌드하고 스프링 IoC 컨테이너에 완벽히 통합합니다.
- **`KakaoLoginClient` 리팩토링**: 절차지향적으로 `RestClient`를 조작하던 코드를 제거하고, 주입받은 선언형 `KakaoApi`를 그대로 주입하여 1줄의 선언형 코드로 외부 통신을 수행합니다.

## 결과 (Consequences)
- **장점**:
  1. **의존성 경량화 수호**: 추가적인 라이브러리(Spring Cloud 관련 패키지 일체)를 빌드 스크립트에 추가하지 않으므로 프로젝트의 빌드 가벼움과 버전 호환성 제약이 일절 발생하지 않습니다.
  2. **극도의 직관성과 직교성**: 외부 API 규격이 정합적인 인터페이스 구조로 정돈되어 있어, 훗날 유지보수하는 사람이 외부 통신 스펙을 단 3초 만에 파악하고 확장할 수 있습니다.
  3. **RestClient 재활용**: 내부 백엔드 엔진으로 이미 정비된 `RestClient` 인프라를 그대로 어댑터 패턴으로 연동하여 사용하므로 통신의 완성도와 데이터 처리가 일관되게 보증됩니다.
- **단점**:
  - `HttpClientConfig` 설정 클래스 작성이 소폭 필요합니다.
