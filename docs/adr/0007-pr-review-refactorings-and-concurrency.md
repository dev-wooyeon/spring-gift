# ADR-0007: PR 리뷰 피드백에 따른 도메인 격리 강화 및 동시성 제어 정책을 정의한다

## 상태

Accepted

## 날짜

2026-06-02

## 맥락

메인 브랜치 기준 PR 리뷰 피드백에서 아키텍처적 개선 요구 및 의문이 제기되었다. 핵심 이슈는 다음과 같다:
1. **위시리스트 성능 및 도메인 결합**: 위시리스트 조회 시의 상품 정보 단건 루프 조회 문제(N+1 쿼리) 및 `WishProductPort`와 구현체(Adapter)의 위치 설계 의도.
2. **이벤트 보안 및 결합**: 주문 생성 이벤트(`OrderCreatedEvent`)에 민감한 정보(`kakaoAccessToken`)를 그대로 포함시켜 전달하는 문제.
3. **인증 필터의 예외 삼킴**: `AuthenticationResolver`에서 모든 `Exception`을 잡아 `null`로 처리함으로써 DB 통신 등 내부 시스템 장애까지 전부 401(Unauthorized)로 숨겨져 모니터링이 불가능했던 문제.
4. **동시성 환경 데이터 정합성**: 다중 스레드 하의 주문 상황에서 옵션 재고 차감 및 회원 포인트 충전/차감 시 데이터 덮어쓰기(Lost Update) 및 음수 재고/포인트 현상 발생 우려.
5. **포인트 패키지 위치**: `point` 패키지가 `member` 도메인 내부에 있지 않고 최상위 도메인 레벨로 독립 배치된 설계 의도.

## 결정

### 1. 위시리스트 N+1 쿼리 해소 및 의존성 격리 유지
- **N+1 문제 해결**: 위시리스트를 조회할 때 단건마다 데이터베이스를 찌르는 구조를 탈피한다. `WishProductPort`에 `findProducts(List<Long> productIds)` 배칭 조회 API를 추가하고, `WishService` 단에서 페이지 내 모든 `productId`를 수집하여 단 한 번의 쿼리(IN 절)로 상품 정보를 일괄 로드하여 1+1 구조로 해소한다.
- **Port/Adapter의 위치 설계 의도**: core 도메인인 `catalog`는 서브 도메인인 `wish`를 절대 알면 안 된다. 의존성을 단방향(`wish` -> `catalog`)으로 제약하기 위해 인터페이스인 `WishProductPort`를 `wish` 애플리케이션 내에 두고, 이를 구현하여 `catalog` 상품 서비스와 통신해주는 `CatalogWishProductAdapter`는 `wish.infrastructure` 패키지에 위치시킨다. 이를 통해 `catalog` 도메인을 `wish` 도메인의 부가적인 요건으로부터 완벽히 보호한다.

### 2. 이벤트 페이로드 내 민감 정보 제거 및 알림 레이어 간접 조회
- **이벤트 경량화**: `OrderCreatedEvent` 페이로드에 민감 정보인 `kakaoAccessToken`을 포함하지 않도록 변경하고, 식별자 명세인 `memberId`만 담아 발행한다.
- **Lazy Loading**: `notification` 패키지 내에 인프라성 포트인 `NotificationMemberPort`를 정의하고, 알림 인프라 어댑터(`MemberNotificationAdapter`)가 알림 발송 시점에 `MemberService`를 통해 토큰을 안전하게 지연 조회(Lazy Load)하도록 수정하여 보안성과 도메인 결합도를 동시에 해결한다.

### 3. 예외 처리 범위 축소를 통한 에러 모니터링 복원
- **예외 범위 Narrowing**: `AuthenticationResolver`에서 catch하는 대상을 모든 `Exception`에서 JWT 만료/포맷 오류인 `JwtException` 및 `IllegalArgumentException`으로 좁힌다.
- **시스템 예외 전파**: DB 연결 문제 등 인프라 장애 발생 시 예외가 상위 컨트롤러와 필터로 정상 전파되게 함으로써, 401로 숨겨지지 않고 의도한 500 내부 서버 에러 응답 및 스택레이스 로그가 정상 수집되도록 조치한다.

### 4. 비관적 락(Pessimistic Lock)을 통한 동시성 데이터 무결성 보장
- **재고 및 포인트 보호**: 다중 사용자 결제가 겹쳐 발생하는 레이스 컨디션 상황 하에서 갱신 분실(Lost Update) 현상을 방지하기 위해, 데이터베이스의 행(Row) 수준 락을 활용한다.
- **PESSIMISTIC_WRITE 적용**: `OptionRepository`와 `MemberRepository`에 `findByIdWithLock(Long id)` 전용 메서드를 선언하고 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 설정한다. 재고를 차감하거나 포인트를 수정하는 비즈니스 진입 전에 락을 걸고 조회해 순차 처리를 보장함으로써 데이터 유실을 방지한다.

### 5. 독립 도메인 확장을 위한 point 패키지 격리
- **패키지 독립**: 포인트는 구조상 `Member` 도메인의 속성(Embedded)에 담겨 있어 물리적 결합도가 높지만, 장기적 비즈니스 관점에서는 독립적인 서브 도메인(Subdomain)으로 인식한다.
- **확장성 확보**: 충전/차감 거래 이력 추적, 만료일자 관리, 포인트 소급 정책 등 독자적이고 복잡한 비즈니스 규칙이 추가될 예정이므로, `member` 패키지 하위로 배치하지 않고 독립된 최상위 도메인 패키지인 `gift.point`로 격리하여 결합도 증가를 방지한다.

## 결과

- **도메인 격리성 극대화**: `catalog`와 `wish`, `member`와 `notification` 간의 불필요한 직·간접 결합을 완벽하게 완화하고 단방향 흐름을 정형화했다.
- **시스템 보안 강화**: 결제 이벤트 상에 카카오 토큰 등의 민감 데이터가 돌아다니지 않도록 격리하여 로깅 등으로 인한 토큰 유출을 방지했다.
- **동시성 무결성 확보**: 비관적 락 적용으로 인해 동일 상품 옵션 및 동일 회원 포인트에 대한 동시 다발적 연산 요청 시 데이터 덮어쓰기 현상 및 정합성 불일치를 기술적으로 제거했다.
- **모니터링 신뢰성 보장**: 401 오류가 더 이상 데이터베이스 연결 실패를 숨기지 않으며, 알림 전송 실패의 경우 스택레이스 에러 로그를 명확하게 기록하여 추적성을 갖췄다.
