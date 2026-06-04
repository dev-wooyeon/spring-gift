# Architecture Decision Records (ADR) Summary

이 문서는 프로젝트 내에서 내린 모든 주요 아키텍처 의사결정(ADR)을 빠르게 파악할 수 있도록 제공하는 요약 페이지입니다. 각 결정의 상세한 맥락, 대안 검토, 트레이드오프는 개별 ADR 문서 링크를 통해 확인할 수 있습니다.

---

## 📌 의사결정 요약 표 (Quick Reference)

| 번호 | 아키텍처 결정 사항 | 상태 | 핵심 결론 (무엇을 왜 선택했는가) |
| :--- | :--- | :--- | :--- |
| **0001** | [Testcontainers 기반 테스트 시스템 우선 구축](./0001-testcontainers-based-test-system.md) | `Accepted` | 로컬 DB 설정 없이 CI/로컬에서 동일하게 작동하는 MySQL Docker 기반 통합 테스트 환경 구축. |
| **0002** | [Gradle check를 품질 게이트 진입점으로 사용](./0002-gradle-check-quality-gate.md) | `Accepted` | 모든 품질 검증(Test, JaCoCo, ArchUnit 등)을 단일 명령 `./gradlew check`로 통일. |
| **0003** | [대규모 리팩토링 전 JaCoCo 커버리지 게이트 우선 도입](./0003-jacoco-before-archunit.md) | `Accepted` | 대규모 리팩토링 중 기존 기능 회귀를 방지하기 위해 아키텍처 검증보다 테스트 커버리지 기준을 우선 적용. |
| **0004** | [모듈러 모놀리스 경계 우선 수립](./0004-modular-monolith-before-domain-deployments.md) | `Accepted` | 독립 배포 분리에 앞서 분산 트랜잭션/네트워크 비용을 방지하기 위해 단일 앱 내 모듈러 모놀리스 경계를 먼저 정립. |
| **0005** | [단일 모듈 안에서 도메인 내부 계층 분리](./0005-domain-internal-package-structure.md) | `Accepted` | `presentation-application-domain-infrastructure` 계층 구조를 정립하고 ArchUnit으로 도메인 격리성 강제. |
| **0006** | [주문 협력 경계와 트랜잭션 정책 명시](./0006-order-transaction-and-port-boundary.md) | `Accepted` | 주문 저장/재고/포인트만 트랜잭션으로 묶고, 알림은 이벤트 후행 처리로 분리. 타 도메인은 Entity 직접 참조 대신 ID 참조. |
| **0007** | [PR 리뷰 피드백 반영 (격리 강화 및 동시성 제어)](./0007-pr-review-refactorings-and-concurrency.md) | `Accepted` | N+1 조회 배칭 해소, 이벤트 민감 정보 제거, 비관적 락(`PESSIMISTIC_WRITE`) 적용, 포인트 도메인 독립 격리. |
| **0008** | [값 객체 제거 및 도메인 엔티티 내부 직접 검증](./0008-entity-internal-validation.md) | `Accepted` | 오버엔지니어링(단순 포맷 검증용 VO들)을 걷어내고, 원시 타입 복구 후 엔티티 자가 검증 메서드로 응집. |

---

## 🔍 핵심 의사결정 상세 요약 (What & Why)

### 📂 1. 도메인 경계 및 패키지 구조

#### [ADR-0004: 도메인 단위 배포를 고려해 모듈러 모놀리스 경계를 먼저 만든다](./0004-modular-monolith-before-domain-deployments.md)
* **최종 선택 (What)**: 물리적인 마이크로서비스(MS)나 멀티모듈로 바로 쪼개지 않고, 단일 Spring Boot 애플리케이션 내에서 느슨한 결합도를 가지는 **모듈러 모놀리스**를 구축.
* **선택 이유 (Why)**: 데이터 소유권과 도메인 간 협력 관계가 불분명한 상태에서 네트워크 경계를 나누면 분산 트랜잭션, 배포 복잡도 등 비용만 증가하므로, 논리적 경계(Presentation/Application/Domain/Infrastructure)를 먼저 검증하기 위함.

#### [ADR-0005: 단일 모듈 안에서 도메인 내부 계층을 먼저 분리한다](./0005-domain-internal-package-structure.md)
* **최종 선택 (What)**: 도메인 패키지 내부를 `presentation`, `application`, `domain`, `infrastructure`로 분리하고, ArchUnit을 통해 계층 간 단방향 의존성 규칙을 강제.
* **선택 이유 (Why)**: 서비스 계층이 HTTP 응답 DTO나 외부 인프라 기술에 직접 결합되는 것을 막고, 도메인 핵심 로직을 순수하게 격리하여 모듈러 모놀리스의 완성도를 높이기 위함.

#### [ADR-0008: 값 객체(Value Object)를 제거하고 도메인 엔티티 내부에서 직접 검증을 처리한다](./0008-entity-internal-validation.md)
* **최종 선택 (What)**: 단순히 단독 검증 조건(길이, Null/Blank 등)만 존재하는 `ProductName`, `OptionName` 값 객체를 제거하고 원시 타입 `String`으로 복구하되, 엔티티(`Product`, `Option`) 내부에서 자가 검증(`checkErrors`)을 직접 수행.
* **선택 이유 (Why)**: 재사용이나 내부 연산 비즈니스가 없는 단순 필드를 값 객체로 쪼개어 파일 수와 DTO 매핑 복잡도를 과도하게 높이던 오버엔지니어링을 해소하고, 엔티티 내에 검증 규칙을 깔끔하게 응집시키기 위함.

---

### 🧪 2. 테스트 및 인프라 구축

#### [ADR-0001: Testcontainers 기반 테스트 시스템을 우선 구축한다](./0001-testcontainers-based-test-system.md)
* **최종 선택 (What)**: Testcontainers MySQL 모듈을 활용하여 `./gradlew test` 시점에 Docker 컨테이너를 자동 구동하여 통합 테스트 수행.
* **선택 이유 (Why)**: H2와의 방언 차이 및 로컬 MySQL 설치 상태에 따른 테스트 파편화를 방지하고, Flyway 마이그레이션이 정상 적용된 실제 DB 환경과 동일한 조건에서 API 및 레포지토리를 자동 검증하기 위함.

#### [ADR-0002: Gradle check를 품질 게이트 진입점으로 사용한다](./0002-gradle-check-quality-gate.md)
* **최종 선택 (What)**: 프로젝트의 표준 검증 태스크로 `./gradlew check`를 사용.
* **선택 이유 (Why)**: 로컬 개발 환경과 CI 파이프라인에서 실행할 검증 명령어를 하나로 통일하고, 향후 정적 분석이나 아키텍처 검증 도구가 추가되더라도 빌드 파이프라인 수정을 최소화하기 위함.

#### [ADR-0003: 대규모 리팩토링 전 JaCoCo 커버리지 게이트를 우선 도입한다](./0003-jacoco-before-archunit.md)
* **최종 선택 (What)**: 패키지 구조 제한(ArchUnit) 이전에 JaCoCo 테스트 커버리지 기준 검증을 먼저 도입하여 품질 게이트에 연결.
* **선택 이유 (Why)**: 구조를 전면 리팩토링하기 전에 기존 레거시의 세부 비즈니스 동작이 깨지지 않도록 충분한 Characterization Test(동작 고정 테스트) 안전망을 구축하고 실행 경로를 측정하기 위함.

---

### 💻 3. 핵심 비즈니스 및 코드 레벨 설계

#### [ADR-0006: 주문 협력 경계와 트랜잭션 정책을 명시한다](./0006-order-transaction-and-port-boundary.md)
* **최종 선택 (What)**: 
  * 주문 생성의 원자성(트랜잭션) 범위를 '주문 저장 + 재고 차감 + 포인트 차감'으로 제한.
  * 카카오 알림톡 발송은 트랜잭션 커밋 이후 후행 작업으로 처리하며, 외부 Port와 이벤트를 통해 결합 해소.
  * 주문/위시 엔티티에서 타 도메인 엔티티(Catalog 등)를 직접 참조하던 방식을 ID 기반 간접 참조로 변경.
* **선택 이유 (Why)**: 인프라/알림 실패가 비즈니스 트랜잭션 전체를 롤백시키는 장애 전파를 막고, DB 테이블 간 물리적 FK 수준 외에는 엔티티 결합을 끊어 향후 독립 배포를 용이하게 만들기 위함.

#### [ADR-0007: PR 리뷰 피드백에 따른 도메인 격리 강화 및 동시성 제어 정책을 정의한다](./0007-pr-review-refactorings-and-concurrency.md)
* **최종 선택 (What)**:
  * 위시리스트 조회 N+1 문제를 `WishProductPort`를 통한 일괄 조회(1+1)로 변경하여 의존 단방향성을 유지한 채 해소.
  * 주문 생성 이벤트에서 민감 정보(`kakaoAccessToken`)를 제거하고 필요시 알림 레이어에서 지연 조회(Lazy Loading).
  * `AuthenticationResolver`의 무분별한 예외 캐치를 줄여 DB 인프라 예외가 정상적으로 500 에러 및 스택레이스에 남도록 조정.
  * 재고 및 포인트 차감 로직에 비관적 락(`@Lock(LockModeType.PESSIMISTIC_WRITE)`)을 적용하여 동시성 정합성 보장.
  * `point` 패키지를 `member` 하위가 아닌 최상위 독립 도메인 패키지로 분리.
* **선택 이유 (Why)**: 메인 브랜치 PR 리뷰에서 제기된 결합도, 보안 유출 위협, 레이스 컨디션 데이터 손실, 불명확한 에러 전파 문제를 구조적/기술적으로 해결하기 위함.
