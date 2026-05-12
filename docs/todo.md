# TodoList

목표: 개인 로컬 설정에 의존하지 않고 누구나 동일하게 실행할 수 있는 MySQL Testcontainers 기반 테스트 시스템을 구축한다.

## 작업 원칙

- 한 번에 한 항목만 진행한다.
- 각 항목은 하나의 커밋 후보가 될 수 있어야 한다.
- 구조 변경과 작동 변경을 섞지 않는다.
- 완료 기준과 검증 명령을 만족해야 다음 항목으로 넘어간다.
- 테스트를 비활성화하거나 실패를 무시하지 않는다.

## Todo

### 0. Kotlin 프로젝트 설정 제거

- [x] Java만 사용하는 프로젝트 기준으로 Kotlin 플러그인, Kotlin 의존성, Kotlin 전용 Gradle 설정을 제거한다.
- 완료 기준: Kotlin 소스가 없고 `./gradlew test`가 성공한다.
- 검증 명령: `./gradlew test`

### 1. 테스트 의존성 추가

- [x] `build.gradle.kts`에 Spring Boot Testcontainers, Testcontainers JUnit Jupiter, Testcontainers MySQL 의존성을 추가한다.
- 완료 기준: Gradle이 테스트 의존성을 해석할 수 있다.
- 검증 명령: `./gradlew test`

### 2. Testcontainers 기반 테스트 골격 추가

- [ ] Spring Boot 테스트가 MySQL 컨테이너를 띄울 수 있는 통합 테스트 클래스를 추가한다.
- 완료 기준: 테스트 실행 시 MySQL 컨테이너가 시작되고 Spring ApplicationContext가 로드된다.
- 검증 명령: `./gradlew test`

### 3. Flyway migration 적용 검증

- [ ] 테스트 DB에 Flyway migration이 적용되었는지 검증하는 테스트를 추가한다.
- 완료 기준: 테스트에서 기본 테이블 또는 초기 데이터 존재를 확인한다.
- 검증 명령: `./gradlew test`

### 4. `/api/products` 통합 테스트 추가

- [ ] MySQL Testcontainers 환경에서 `/api/products` 조회 API를 검증한다.
- 완료 기준: 응답 status가 `200 OK`이고 초기 상품 데이터가 응답에 포함된다.
- 검증 명령: `./gradlew test`

### 5. 테스트 실행 문서화

- [ ] README에 Docker 필요 조건과 테스트 실행 방법을 추가한다.
- 완료 기준: 처음 보는 사람이 `./gradlew test` 실행 전 필요한 조건을 알 수 있다.
- 검증 명령: 문서 검토

### 6. 검증 기록 업데이트

- [ ] `docs/분석.md` 또는 별도 검증 기록 문서에 Testcontainers 테스트 결과를 남긴다.
- 완료 기준: 실행 명령, 성공 여부, 확인한 동작이 기록되어 있다.
- 검증 명령: `./gradlew test`

### 7. 다음 품질 게이트 후보 결정

- [ ] JaCoCo, ArchUnit, Gradle `check` 확장, Docker Compose 중 다음 확장 대상을 하나만 결정한다.
- 완료 기준: 선택지가 2개 이상이면 ADR을 작성한다.
- 검증 명령: 문서 검토
