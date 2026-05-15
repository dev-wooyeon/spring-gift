# ADR-0002: Gradle check를 품질 게이트 진입점으로 사용한다

## 상태

Accepted

## 날짜

2026-05-13

## 맥락

MySQL Testcontainers 기반 통합 테스트가 추가되어 `./gradlew test`만으로 Docker 기반 MySQL, Spring Boot 애플리케이션 컨텍스트, Flyway migration, `/api/products` 조회 API를 검증할 수 있게 되었다.

다음 단계에서는 이 테스트를 포함해 앞으로 추가될 품질 검증을 어떤 명령으로 실행할지 정해야 한다. 후보는 다음과 같다.

- JaCoCo 커버리지 기준
- ArchUnit 아키텍처 규칙
- Gradle `check` 확장
- Docker Compose 기반 수동 통합 환경

## 결정

다음 품질 게이트 진입점은 Gradle 표준 태스크인 `./gradlew check`로 정한다.

현재는 `check`가 `test`를 포함하므로 Testcontainers 기반 통합 테스트까지 실행된다. 이후 JaCoCo, ArchUnit 같은 추가 품질 검증을 도입하더라도 별도 명령을 늘리지 않고 `check` 태스크에 연결한다.

## 트레이드오프

### Gradle check

장점:

- Java Gradle 프로젝트의 표준 검증 진입점이다.
- 현재 `test` 태스크를 포함하므로 새 도구 없이 바로 사용할 수 있다.
- 이후 JaCoCo, ArchUnit, 정적 분석 태스크를 하나의 검증 명령으로 묶기 좋다.
- 로컬과 CI에서 같은 명령을 사용할 수 있다.

단점:

- 지금 당장은 `test`와 실행 결과가 거의 같다.
- 품질 기준 자체를 강화하려면 JaCoCo, ArchUnit 같은 추가 태스크를 별도로 도입해야 한다.

### JaCoCo

장점:

- 테스트 커버리지 기준을 숫자로 관리할 수 있다.
- 테스트가 부족한 영역을 찾는 데 도움이 된다.

단점:

- 커버리지 숫자는 품질을 직접 보장하지 않는다.
- 현재처럼 통합 테스트 기반을 만드는 단계에서는 기준값 논쟁이 먼저 커질 수 있다.

### ArchUnit

장점:

- Controller, Repository, domain 간 의존 방향 같은 구조 규칙을 코드로 강제할 수 있다.
- 프로젝트가 커질수록 회귀 방지 효과가 커진다.

단점:

- 현재 패키지 구조와 아키텍처 규칙이 충분히 명확히 합의된 뒤 도입하는 편이 안전하다.
- 너무 이른 도입은 실제 설계보다 규칙 작성이 먼저 앞설 수 있다.

### Docker Compose

장점:

- 애플리케이션과 DB를 사람이 직접 함께 띄워 보는 수동 통합 환경에 유용하다.
- 운영 실행 형태를 smoke test로 확인하기 좋다.

단점:

- 자동 품질 게이트보다는 수동 확인 환경에 가깝다.
- 현재 목표인 단일 자동 검증 명령과는 직접성이 낮다.

## 결과

- 기본 검증 명령은 `./gradlew check`로 문서화한다.
- `./gradlew test`는 테스트만 직접 실행할 때 사용할 수 있는 하위 명령으로 둔다.
- CI를 추가하면 우선 `./gradlew check`를 실행하도록 구성한다.
- 다음 품질 강화 후보는 `check`에 붙일 수 있는 JaCoCo 또는 ArchUnit 중에서 별도 작업으로 결정한다.
