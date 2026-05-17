# spring-gift

Spring Boot 기반 선물하기 서비스입니다. 상품, 카테고리, 회원, 위시, 주문 기능을 제공하며 관리자용 HTML 화면과 고객용 REST API를 함께 다룹니다.

## 현재 목표

개인 로컬 설정에 의존하지 않고 누구나 동일하게 실행할 수 있는 테스트 시스템을 유지한다. MySQL Testcontainers 기반 characterization test와 JaCoCo 품질 게이트를 기준선으로 두고, 주요 도메인의 서비스 계층과 도메인 내부 패키지 경계를 분리했다. 기본 품질 게이트 진입점은 `./gradlew check`다.

## 다음 작업

- [ ] ArchUnit 아키텍처 규칙 도입
- [ ] 도메인 간 port 후보 분리
- [ ] 주문 트랜잭션 정책 결정

## 구현 전략

1. `build.gradle.kts`에 Spring Boot Testcontainers와 MySQL Testcontainers 테스트 의존성을 추가한다.
2. 테스트 실행 시 MySQL 컨테이너가 자동으로 시작되도록 구성한다.
3. Spring Boot 테스트 컨텍스트가 컨테이너 DB에 연결되도록 설정한다.
4. Flyway migration과 초기 데이터가 테스트 DB에 적용되는지 확인한다.
5. `/api/products` 조회 통합 테스트를 추가해 기본 상품 목록을 검증한다.
6. `./gradlew test` 결과를 검증 기록으로 남긴다.
7. 리팩토링 전 characterization test로 현재 동작을 고정한다.
8. 도메인 단위로 Controller의 Repository 직접 접근을 Service 계층으로 이동한다.
9. 단일 Gradle 모듈 안에서 `presentation`, `application`, `domain`, `infrastructure` 계층을 도메인별로 분리한다.
10. 리팩토링 후 ArchUnit으로 Controller, Service, Repository 의존 방향을 품질 게이트에 추가한다.

## 실행

### 필요 조건

- Docker Desktop 또는 Docker Engine이 설치되어 있고 실행 중이어야 한다.
- 로컬 MySQL 서버, 로컬 DB 포트, Kakao API 키는 필요하지 않다.
- 첫 실행 때는 Testcontainers가 MySQL 이미지를 내려받기 때문에 시간이 더 걸릴 수 있다.

### 품질 게이트

```bash
./gradlew check
```

`./gradlew check`는 기본 검증 진입점이다. 현재는 `test`와 JaCoCo 커버리지 검증을 포함한다. 테스트는 MySQL Testcontainer를 자동으로 시작하고, Spring Boot 애플리케이션 컨텍스트가 컨테이너 DB에 연결되는지 확인한다. 또한 Flyway migration, 초기 데이터, 주요 API와 관리자 화면의 현재 HTTP 응답과 DB 상태를 검증한다.

JaCoCo HTML 리포트는 실행 후 `build/reports/jacoco/test/html/index.html`에서 확인할 수 있다.

### 테스트만 실행

```bash
./gradlew test
```

테스트만 직접 확인할 때는 `./gradlew test`를 사용할 수 있다.

## 작업 규칙

- 한 번에 한 조각만 변경한다.
- 구조 변경과 작동 변경을 섞지 않는다.
- 테스트를 비활성화하거나 회피하지 않는다.
- 로컬 MySQL, 로컬 포트, Kakao API 키에 의존하지 않는다.
- 자세한 결정 배경은 `docs/adr/0001-testcontainers-based-test-system.md`를 따른다.

## 참고 문서

- `Agent.md`: 작업 규칙과 검증 기준
- `docs/todo.md`: 실행 가능한 작업 목록
- `docs/adr/0001-testcontainers-based-test-system.md`: Testcontainers 기반 테스트 시스템 결정 기록
- `docs/adr/0002-gradle-check-quality-gate.md`: Gradle `check` 품질 게이트 결정 기록
- `docs/adr/0003-jacoco-before-archunit.md`: 대규모 리팩토링 전 JaCoCo 우선 도입 결정 기록
- `docs/adr/0004-modular-monolith-before-domain-deployments.md`: 도메인 단위 배포 전 모듈러 모놀리스 경계 결정 기록
- `docs/adr/0005-domain-internal-package-structure.md`: 단일 모듈 안 도메인 내부 계층 구조 결정 기록
- `docs/분석.md`: 관리자 화면 기준선과 비즈니스 흐름 분석
