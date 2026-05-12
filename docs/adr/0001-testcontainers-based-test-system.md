# ADR-0001: Testcontainers 기반 테스트 시스템을 우선 구축한다

## 상태

Accepted

## 날짜

2026-05-11

## 맥락

현재 목표는 단순 기능 추가나 리팩토링보다, 누구나 개인 로컬 설정에 의존하지 않고 동일하게 실행 가능한 테스트 시스템을 구축하는 것이다. 또한 프로덕션과 유사한 환경에서 반복 실행할 수 있어야 한다.

현재 프로젝트는 Spring Boot, JPA, Flyway, H2, MySQL driver를 함께 사용한다. 기본 실행은 로컬 설정이나 H2에 기대기 쉽고, 운영에 가까운 MySQL 동작과 Flyway migration 결과를 자동으로 검증하는 장치가 부족하다.

이번 세션에서 다음 선택지를 검토했다.

- SonarQube 같은 품질 분석 시스템
- Spring Boot Starter 형태의 SDK 라이브러리
- Gradle Convention Plugin
- Docker Compose 기반 수동 통합 환경
- Testcontainers 기반 자동 통합 테스트

## 결정

첫 번째 테스트 시스템 구축 방향은 Testcontainers 기반 자동 통합 테스트로 한다.

구체적으로는 `./gradlew test` 실행 시 테스트 코드가 MySQL 컨테이너를 자동으로 띄우고, Spring Boot 애플리케이션 컨텍스트가 해당 DB에 연결되며, Flyway migration을 적용한 뒤 HTTP/API 또는 Repository 수준의 검증을 수행하도록 한다.

Spring Boot의 `spring-boot-testcontainers`와 Testcontainers MySQL 모듈을 우선 사용한다. Docker Compose, SonarQube, Gradle Convention Plugin, Spring Boot Starter는 이후 필요가 분명해졌을 때 별도 단계로 검토한다.

## 트레이드오프

### Testcontainers

장점:

- 로컬 MySQL 설치와 수동 DB 설정에 의존하지 않는다.
- 로컬과 CI에서 같은 방식으로 테스트할 수 있다.
- H2와 MySQL 차이로 인한 누락을 줄인다.
- Flyway migration 결과를 실제 DB에 가깝게 검증할 수 있다.
- 테스트 코드 안에 인프라 구성이 포함되어 재현성이 높다.

단점:

- Docker Desktop 또는 Docker Engine이 필요하다.
- 순수 단위 테스트보다 실행 시간이 길다.
- 컨테이너 이미지 다운로드가 최초 실행 시간을 늘릴 수 있다.
- 외부 API까지 실제로 재현하는 도구는 아니므로 Kakao API 등은 별도 fake, mock, stub이 필요하다.

### Docker Compose

장점:

- 애플리케이션과 DB를 사람이 직접 함께 띄워 운영 유사 실행을 확인하기 좋다.
- jar 실행, 환경변수, DB 연결을 수동 smoke test로 검증하기 좋다.

단점:

- 테스트 코드와 생명주기가 분리되어 자동화된 `./gradlew test` 흐름과는 거리가 있다.
- 사람이 `docker compose up/down`을 관리해야 해서 반복 자동화 기준으로는 Testcontainers보다 약하다.

### Spring Boot Starter 형태의 SDK

장점:

- 여러 Spring Boot 프로젝트에 공통 테스트 설정을 배포하기 좋다.
- 자동 설정과 공통 테스트 유틸을 라이브러리처럼 제공할 수 있다.

단점:

- 반복 적용할 규칙이 아직 충분히 안정화되지 않았다.
- 현재 단계에서는 테스트 시스템 구축보다 라이브러리 설계 비중이 커질 수 있다.
- 현재 저장소의 작동 증거를 먼저 만드는 흐름과 맞지 않는다.

### Gradle Convention Plugin

장점:

- 테스트, 커버리지, 품질 게이트 같은 빌드 규칙을 표준화하기 좋다.
- 여러 프로젝트로 확장할 때 Spring Boot Starter보다 테스트 시스템 표준화에 더 직접적이다.

단점:

- 첫 단계부터 도입하면 빌드 로직 구조화가 주 작업이 된다.
- 아직 공통화할 테스트 규칙이 충분히 검증되지 않았다.

## 결과

- 첫 구현 단위는 MySQL Testcontainers 기반 통합 테스트 1개로 제한한다.
- 테스트 대상은 외부 API 의존이 없는 `/api/products` 또는 Repository/Flyway 기준선으로 시작한다.
- 테스트는 로컬 MySQL, 로컬 포트, Kakao API 키에 의존하지 않아야 한다.
- Docker가 없을 때의 실패는 환경 미비로 명확히 드러나야 하며, 테스트를 임의로 비활성화하지 않는다.
- 이후 필요하면 `./gradlew check` 또는 `qualityGate` 태스크로 확장한다.
- 같은 규칙이 반복되고 다른 프로젝트에 옮길 필요가 확인되면 Gradle Convention Plugin을 검토한다.
- Spring Boot Starter는 공통 런타임 자동 설정 또는 테스트 유틸 API가 명확해진 뒤 검토한다.

## 다음 작업

1. `README.md` 또는 계획 문서에 "MySQL Testcontainers 기반 통합 테스트 추가"를 다음 작업으로 기록한다.
2. `build.gradle.kts`에 필요한 test dependency를 추가한다.
3. Spring Boot 테스트에서 MySQL 컨테이너를 띄우고 Flyway migration이 적용되는지 확인한다.
4. `/api/products`가 초기 데이터 기준으로 조회되는 통합 테스트를 추가한다.
5. `./gradlew test` 결과를 검증 기록으로 남긴다.
