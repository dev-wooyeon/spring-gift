# ADR-0005: 단일 모듈 안에서 도메인 내부 계층을 먼저 분리한다

## 상태

Accepted

## 날짜

2026-05-17

## 맥락

도메인 단위 배포를 목표로 삼더라도 지금 바로 Gradle 멀티모듈이나 독립 서비스로 나누면 데이터 소유권, 트랜잭션, API contract가 정리되지 않은 상태에서 운영 복잡도만 커진다.

따라서 현재 단계의 목표는 배포 단위를 늘리는 것이 아니라, 단일 Spring Boot 애플리케이션 안에서 도메인 경계를 코드 구조로 먼저 드러내는 것이다.

## 결정

현재 구조는 단일 Gradle 모듈을 유지하고, 도메인별로 다음 내부 계층을 둔다.

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
└── wish
    ├── application
    ├── domain
    ├── infrastructure
    └── presentation
```

계층별 책임은 다음과 같다.

- `presentation`: HTTP 요청/응답, validation, DTO와 application command 변환
- `application`: 유스케이스, 트랜잭션 후보, 도메인 간 협력 지점
- `domain`: Entity, 도메인 규칙, 값 검증
- `infrastructure`: Spring Data Repository, 외부 API client
- `support`: 인증처럼 여러 계층에서 쓰이는 기술 지원 코드

서비스 계층이 `presentation` DTO를 직접 알지 않도록 request DTO는 application command로 변환한다. 이로써 HTTP 모델을 바꿔도 유스케이스 입력 모델이 바로 흔들리지 않게 한다.

## 문제 해결 방법

### 1. 계층 의존 방향

문제: Controller가 Repository를 직접 알거나, application service가 presentation DTO를 직접 알면 경계가 코드 리뷰에만 의존한다.

방법:

- ArchUnit으로 `presentation -> application -> domain/infrastructure` 방향을 검증한다.
- `application` 패키지에서 `presentation` 패키지 import를 금지한다.
- 다른 도메인의 `infrastructure` import를 금지하고, 필요한 경우 application service 또는 port를 통해 연결한다.

### 2. 도메인 간 결합

문제: Order가 Catalog의 Option, Member의 Point, Notification의 Kakao client를 직접 엮으면 독립 배포 후보가 되기 어렵다.

방법:

- 단기: 도메인 간 호출은 application service를 통해서만 허용한다.
- 중기: `Order`가 필요한 Catalog/Member 기능을 port 인터페이스로 정의하고 adapter가 구현한다.
- 장기: 재고 차감, 포인트 차감, 메시지 발송처럼 실패 정책이 다른 흐름은 domain event와 비동기 처리 후보로 분리한다.

### 3. 트랜잭션 정책

문제: 주문 생성은 재고 차감, 포인트 차감, 주문 저장, 알림 발송이 섞여 있다. 어떤 실패가 전체 롤백이어야 하는지 결정하지 않으면 서비스 분리 시 장애가 커진다.

방법:

- 주문 성공의 원자성 범위를 먼저 결정한다.
- 재고와 포인트를 같은 트랜잭션으로 묶을지, 보상 트랜잭션으로 풀지 ADR로 남긴다.
- 알림 발송은 주문 성공 후 후행 작업으로 보고 실패해도 주문을 실패시키지 않는 정책을 기본 후보로 둔다.

### 4. 데이터 소유권

문제: `Wish`와 `Order`가 Catalog Entity를 직접 참조하고, member는 primitive FK로 들고 있어 배포 경계가 불명확하다.

방법:

- 각 도메인이 소유하는 테이블과 외부 도메인 참조 방식을 문서화한다.
- 독립 배포 후보가 되면 외부 도메인 Entity 참조를 ID 또는 read model로 바꾼다.
- DB schema 분리 전에는 migration과 characterization test로 현재 동작을 고정한다.

### 5. API contract

문제: 내부 Entity와 외부 API 응답이 가까우면 도메인 모델 변경이 API 변경으로 번지기 쉽다.

방법:

- API request/response DTO는 `presentation`에 유지한다.
- 유스케이스 입력은 application command로 분리한다.
- 도메인 간 API가 필요해지면 public contract DTO를 별도로 두고, 내부 Entity를 직접 노출하지 않는다.

## 결과

- 지금 단계는 Gradle 멀티모듈 전환이 아니다.
- 단일 모듈의 모듈러 모놀리스 구조로 경계를 먼저 검증한다.
- ArchUnit, port, event, transaction policy, data ownership 정리가 끝난 도메인부터 멀티모듈 또는 독립 배포 후보로 승격한다.
