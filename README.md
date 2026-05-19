# spring-gift

Spring Boot 기반 선물하기 서비스입니다. 상품, 카테고리, 옵션, 회원/인증, 위시, 주문, 관리자 화면을 제공하며, MySQL Testcontainers 기반 통합 테스트와 아키텍처 규칙으로 리팩토링 회귀를 검증합니다.

## 현재 상태

- 단일 Gradle 모듈을 유지한 모듈러 모놀리스 구조
- 도메인별 `presentation`, `application`, `domain`, `infrastructure` 패키지 분리
- Controller의 Repository 직접 접근 제거
- HTTP DTO와 application command/read model 분리
- Order의 Catalog/Member 협력 지점 port 분리
- 주문 생성의 재고 차감, 포인트 차감, 주문 저장을 단일 트랜잭션으로 묶음
- Notification은 주문 커밋 이후 이벤트 listener로 후행 처리
- Wish/Order의 Catalog Entity 직접 참조 제거
- REST API 전역 예외 처리와 도메인별 예외 타입 적용
- `./gradlew check`에 통합 테스트, JaCoCo, ArchUnit 규칙 포함

## 과제 분석

기존 코드는 기능은 동작하지만 Controller, Repository, Entity, 외부 API 호출이 한 흐름에 섞여 있어 변경 영향 범위를 추론하기 어려웠습니다. 특히 주문 생성은 재고 차감, 포인트 차감, 주문 저장, Kakao 메시지 발송이 한 흐름에 모여 있어 트랜잭션 정책과 외부 연동 실패 정책이 불명확했습니다.

따라서 리팩토링 목표를 새 기능 추가가 아니라 다음 네 가지로 잡았습니다.

1. 현재 동작을 characterization test로 고정한다.
2. 도메인 단위로 읽히는 패키지 구조를 만든다.
3. 서비스 계층과 application input/output을 분리해 HTTP 모델의 영향을 줄인다.
4. 품질 게이트로 구조 규칙을 자동 검증한다.

## 도메인별 작업 목록

| 도메인 | 분석한 책임 | 진행한 작업 | 검증 |
| --- | --- | --- | --- |
| Catalog | 상품, 카테고리, 옵션 관리 | Controller에서 Repository 접근 제거, `ProductService`, `CategoryService`, `OptionService` 분리, 요청 DTO를 command로 변환 | 상품/카테고리/옵션 characterization test |
| Member/Auth | 회원 가입, 로그인, JWT, Kakao OAuth | 회원 서비스와 Kakao 인증 서비스 분리, Lombok 생성자 주입 적용, token response 경계 분리 | 회원/인증 characterization test |
| Point | 회원 보유 포인트 규칙 | `member.point` DB 컬럼은 유지하되, 충전/차감 규칙은 `Point` 값 객체로 분리 | `PointTest` |
| Wish | 인증 회원의 위시 목록 | `WishService` 분리, `WishProductPort`와 `WishView`로 Catalog Entity 직접 참조 제거 | 위시 characterization test |
| Order | 주문 생성, 재고 차감, 포인트 차감 | `OrderOptionPort`, `OrderMemberPort` 도입, 재고/포인트/주문 저장 단일 트랜잭션 적용, `Order`는 `optionId`만 보관 | 주문 characterization test |
| Notification | Kakao 메시지 발송 | 주문 생성 본 흐름에서 분리, `OrderCreatedEvent`를 커밋 이후 listener가 처리 | 주문 메시지 mock 검증 |
| Admin | 관리자 상품/회원 HTML 화면 | 기존 관리자 흐름을 characterization test로 고정하고 서비스 계층을 경유하도록 유지 | 관리자 화면 characterization test |
| Architecture | 구조 회귀 방지 | ArchUnit으로 presentation/application/infrastructure 의존 규칙 검증 | `DomainArchitectureTest` |

## 코드 아키텍처 기준

패키지 구조는 도메인 우선으로 잡고, 각 도메인 안에서 계층을 나눴습니다.

```text
gift
├── auth
│   ├── application
│   ├── infrastructure
│   ├── presentation
│   └── support
├── catalog
│   ├── application
│   ├── domain
│   ├── infrastructure
│   └── presentation
├── member
│   ├── application
│   ├── domain
│   ├── infrastructure
│   └── presentation
├── notification
│   ├── application
│   └── infrastructure
├── order
│   ├── application
│   ├── domain
│   ├── infrastructure
│   └── presentation
├── point
│   └── domain
├── support
│   ├── exception
│   └── presentation
└── wish
    ├── application
    ├── domain
    ├── infrastructure
    └── presentation
```

계층별 기준은 다음과 같습니다.

- `presentation`: HTTP 요청/응답, validation, DTO와 command 변환
- `application`: 유스케이스, 트랜잭션, port, read model
- `domain`: Entity와 도메인 규칙
- `infrastructure`: Spring Data Repository, 외부 API client, 다른 도메인 adapter
- `support`: 인증처럼 여러 도메인에서 사용하는 기술 지원 코드와 전역 API 예외 처리

ArchUnit으로 아래 규칙을 `check`에 포함했습니다.

- `presentation`은 `infrastructure`에 의존하지 않는다.
- `application`은 `presentation`에 의존하지 않는다.
- 다른 도메인의 `infrastructure`를 직접 참조하지 않는다.
- Order/Wish의 `application`, `domain`은 다른 도메인 Entity에 직접 의존하지 않는다.
- cross-domain adapter는 `infrastructure` 패키지에 둔다.

## 구현 전략

이번 작업은 큰 리팩토링을 한 번에 진행하지 않고, 동작 고정 후 작은 단위로 나눠 진행했습니다.

1. Testcontainers 기반 통합 테스트 환경을 먼저 구축했습니다.
2. Flyway migration과 초기 데이터 적용을 검증했습니다.
3. 주요 API와 관리자 화면의 현재 동작을 characterization test로 고정했습니다.
4. 상품/카테고리, 회원/인증, 위시, 주문, 옵션 순서로 서비스 계층을 분리했습니다.
5. Lombok `@RequiredArgsConstructor`로 생성자 boilerplate를 줄였습니다.
6. 도메인 단위 패키지 구조로 재배치했습니다.
7. HTTP DTO가 application service로 들어가지 않도록 command를 도입했습니다.
8. Order/Wish에서 외부 도메인 Entity 직접 참조를 port/read model로 줄였습니다.
9. 주문 트랜잭션 정책을 ADR로 기록하고 테스트 기대값을 정책에 맞췄습니다.
10. ArchUnit 규칙을 추가해 구조 위반을 자동 검증했습니다.
11. 테스트 패키지도 도메인별로 정렬해 테스트 위치만 봐도 검증 범위가 보이게 했습니다.

## 주문 정책

주문 생성은 다음 정책을 기준으로 구현했습니다.

- 옵션이 없으면 주문을 만들지 않고 `404`로 매핑합니다.
- 재고 차감, 포인트 차감, 주문 저장은 하나의 트랜잭션입니다.
- 재고 부족 또는 포인트 부족 예외가 발생하면 재고 차감도 롤백됩니다.
- 주문 저장이 커밋된 뒤 Kakao 메시지를 발송합니다.
- Kakao 메시지 발송 실패는 주문 성공 여부에 영향을 주지 않습니다.

이 결정은 `docs/adr/0006-order-transaction-and-port-boundary.md`에 기록했습니다.

## 예외 처리 정책

REST API 예외 처리는 `GlobalApiExceptionHandler`에서 공통으로 처리합니다.

- 도메인 규칙 위반은 `CatalogException`, `MemberException`, `PointException`, `WishException`처럼 도메인별 예외 타입으로 표현합니다.
- 도메인 예외는 공통 기반 타입인 `DomainException`을 상속하고, 전역 handler가 HTTP status와 메시지로 변환합니다.
- validation 오류와 필수 요청 헤더 누락도 전역 handler에서 `400 Bad Request`로 변환합니다.
- 기존 API 테스트가 기대하던 한글 예외 메시지는 유지했습니다.

## 트랜잭션 작업

주문 도메인은 리팩토링 중 가장 명시적으로 트랜잭션 정책을 바꾼 부분입니다.

기존에는 주문 생성 흐름에서 재고 차감 이후 포인트 부족 예외가 발생하면, 재고 차감 결과가 남을 수 있는 동작을 characterization test로 확인했습니다. 이 동작은 주문 성공의 원자성 관점에서 유지하기 어렵다고 판단해 정책을 변경했습니다.

변경 후 기준은 다음과 같습니다.

```text
주문 생성 트랜잭션
1. 옵션 조회 및 재고 예약
2. 회원 포인트 차감
3. 주문 저장
4. 커밋 이후 주문 생성 이벤트 발행 결과로 Kakao 메시지 발송
```

트랜잭션 안에 포함되는 작업과 제외되는 작업을 분리했습니다.

| 구분 | 포함 여부 | 이유 |
| --- | --- | --- |
| 옵션 재고 차감 | 포함 | 주문 성공의 핵심 상태 변경 |
| 회원 포인트 차감 | 포함 | 결제 가능 여부와 직접 연결 |
| 주문 저장 | 포함 | 재고/포인트 변경과 함께 원자적으로 처리 |
| Kakao 메시지 발송 | 제외 | 외부 API 실패가 주문 성공을 깨면 안 됨 |

검증은 `OrderCharacterizationTest`에서 포인트 부족 시 재고가 롤백되는지 확인하는 방식으로 고정했습니다. 메시지 발송은 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 커밋 이후 처리되며, 실패해도 주문 결과에 영향을 주지 않도록 분리했습니다.

## 테스트 전략

테스트는 도메인별 패키지로 배치했습니다.

```text
src/test/java/gift
├── admin
├── architecture
├── catalog
├── member
├── order
├── support
└── wish
```

테스트 성격은 다음과 같습니다.

- Unit test: 도메인 객체, validator, application service의 비즈니스 규칙과 분기 검증
- 통합 테스트: 실제 Spring context, Flyway, MySQL Testcontainer 기반 API/HTML 흐름 검증
- Characterization test: 리팩토링 전후 HTTP 응답과 DB 상태 변화 고정
- Architecture test: ArchUnit으로 계층 및 도메인 의존 규칙 검증
- Coverage gate: JaCoCo line coverage 기준을 `check`에 포함

단위 테스트는 `@DisplayName`으로 검증 의도를 드러내고, 본문은 `given / when / then` 흐름으로 작성했습니다. 통합 테스트가 넓은 실행 경로를 보장한다면, 단위 테스트는 포인트 차감, 옵션 재고 차감, 이름 검증, 위시/주문 application 분기처럼 핵심 규칙을 작게 설명하는 역할을 합니다.

## 실행 방법

### 필요 조건

- Docker Desktop 또는 Docker Engine이 실행 중이어야 합니다.
- 로컬 MySQL 서버, 로컬 DB 포트, Kakao API 키는 테스트 실행에 필요하지 않습니다.
- 첫 실행은 Testcontainers가 MySQL 이미지를 내려받기 때문에 시간이 더 걸릴 수 있습니다.

### 전체 품질 게이트

```bash
./gradlew check
```

`check`는 `test`, JaCoCo 리포트/검증, ArchUnit 테스트를 포함합니다.

### 테스트만 실행

```bash
./gradlew test
```

JaCoCo HTML 리포트는 실행 후 `build/reports/jacoco/test/html/index.html`에서 확인할 수 있습니다.

## AI 활용 시 고려한 점

AI를 단순 코드 생성 도구로 쓰기보다, 변경 범위와 구조적 리스크를 계속 검토하는 보조 리뷰어처럼 사용했습니다.

진행 순서는 다음 기준을 따랐습니다.

1. 코드 변경 전 현재 구조와 테스트를 먼저 읽었습니다.
2. 리팩토링 전에 characterization test를 보강해 기존 동작을 고정했습니다.
3. 한 번에 한 도메인씩 작업하고, 작업 단위별로 커밋했습니다.
4. 사용자의 피드백으로 생성자 주입, Lombok, 도메인별 배포 가능성, 디렉토리 구조 문제를 재검토했습니다. 
5. 구조 변경은 ADR에 남기고, 자동 검증 가능한 내용은 ArchUnit으로 옮겼습니다. 
6. AI가 제안한 구조가 실제 유지보수성으로 이어지는지 `./gradlew check`로 매번 확인했습니다. 
7. 새 todo는 무조건 늘리지 않고, 합의된 36번까지만 완료 범위로 삼았습니다.

이 과정에서 특히 경계한 점은 다음과 같습니다.

- 테스트를 끄거나 실패를 무시하지 않는다.
- 문서상 의도와 코드 구조가 다르면 코드나 문서를 즉시 맞춘다.
- 도메인 분리를 이유로 멀티모듈/마이크로서비스를 성급히 도입하지 않는다.
- 후속 작업은 사용자와 협의된 범위 안에서만 todo에 반영한다.

## 참고 문서

- `docs/todo.md`: 진행한 작업 목록
- `docs/분석.md`: 관리자 화면 기준선과 비즈니스 흐름 분석
- `docs/adr/0001-testcontainers-based-test-system.md`: Testcontainers 기반 테스트 시스템 결정
- `docs/adr/0002-gradle-check-quality-gate.md`: Gradle `check` 품질 게이트 결정
- `docs/adr/0003-jacoco-before-archunit.md`: JaCoCo 우선 도입 결정
- `docs/adr/0004-modular-monolith-before-domain-deployments.md`: 모듈러 모놀리스 경계 결정
- `docs/adr/0005-domain-internal-package-structure.md`: 도메인 내부 계층 구조 결정
- `docs/adr/0006-order-transaction-and-port-boundary.md`: 주문 트랜잭션과 port/event 경계 결정
