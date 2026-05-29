# ADR-0012: 커스텀 @LoginMember 애노테이션과 HandlerMethodArgumentResolver 도입

## 맥락 (Context)
현재 `spring-gift` 프로젝트는 컨트롤러마다 사용자 인증 처리를 위해 다음과 같은 공통 로직을 반복적으로 수행하고 있습니다.
- `@RequestHeader("Authorization") String authorization` 형태로 직접 HTTP 헤더를 전달받음
- `AuthenticationResolver`를 직접 의존하여 `extractMember(authorization)` 호출
- 결과가 `null`일 경우 `ResponseEntity.status(401).build()`를 반환하는 예외/분기 논리 직접 구현

이러한 수동 처리 방식은 다음과 같은 문제점을 낳습니다.
1. **코드 중복**: 회원 인증 정보가 필요한 모든 컨트롤러 API마다 동일한 헤더 파싱 및 `null` 체크 코드가 복사-붙여넣기 형태로 존재합니다.
2. **단일 책임 원칙(SRP) 위반**: 컨트롤러가 핵심 HTTP 요청/응답 변환과 비즈니스 조율에 집중하지 못하고, 인증 토큰 추출 및 예외 처리라는 인프라스트럭처적 책임까지 함께 짊어집니다.
3. **일관성과 직관성 저해**: Spring Security가 없는 아키텍처 환경이지만, Spring MVC가 지원하는 최적의 우아한 구조를 활용하지 않아 유지보수하는 제3자의 시선에서 다소 원시적으로 보일 수 있습니다.

## 결정 (Decision)
설계의 최우선 가치인 **일관성(Consistency)**과 **직관성(Intuitiveness)**을 완벽히 수호하기 위해, Spring MVC의 표준 바인딩 확장 스펙인 **`HandlerMethodArgumentResolver`**를 도입하고 커스텀 애노테이션인 **`@LoginMember`**를 정의합니다.

- **`@LoginMember` 애노테이션**: 컨트롤러 파라미터 수준에서 사용되어 "해당 객체가 인증된 회원 정보"임을 선언합니다.
- **`LoginMemberArgumentResolver` 구현**:
  - 요청의 `Authorization` 헤더에서 JWT를 추출하고 `AuthenticationResolver`를 통해 `Member` 도메인을 조회합니다.
  - 로그인 정보가 누락되거나 유효하지 않은 경우, 컨트롤러 내부에서 401 처리를 하는 대신 `UnauthenticatedException` 표준 예외를 발생시키고 이를 `GlobalExceptionHandler`가 일관되게 `401 Unauthorized` HTTP Status로 가로채어 응답하도록 처리 책임을 완전히 격리합니다.
- **`WebConfig` 등록**: `WebMvcConfigurer` 구현체를 통해 생성한 `LoginMemberArgumentResolver`를 빈으로 등록하여 Spring Web MVC 생명주기에 완벽하게 통합시킵니다.

## 결과 (Consequences)
- **장점**:
  1. **극적인 직관성 획득**: 컨트롤러 파라미터에 `@LoginMember Member member`를 선언하는 것만으로 헤더 파싱, DB 회원 확인, 인증 예외 처리가 모두 자동화되어 코드가 놀랍도록 간결해집니다.
  2. **완벽한 책임 분리**: 인증 검증 및 예외 변환 로직이 프레임워크 확장 포인터(`ArgumentResolver`)와 전역 에러 핸들러(`ControllerAdvice`)로 완벽히 이전되어 컨트롤러의 SRP가 완벽히 수호됩니다.
  3. **코드 중복 100% 제거**: 수많은 인증 필요 API의 boilerplate 코드가 영구 소멸됩니다.
- **단점**:
  - `WebMvcConfigurer` 관련 커스텀 설정 클래스(`WebConfig`) 및 신규 예외 클래스가 추가되어 파일 개수가 약간 증가합니다.
