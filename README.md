# spring-gift

## 과제5 작업 계획

### 목표

개인 로컬 설정에 의존하지 않고 누구나 동일하게 실행할 수 있는 테스트 시스템을 구축한다. 첫 단계는 MySQL Testcontainers 기반 통합 테스트를 추가하여 `./gradlew test`만으로 운영에 가까운 DB 환경과 Flyway migration 결과를 검증하는 것이다.

### 다음 작업

- [ ] MySQL Testcontainers 기반 통합 테스트 추가

### 구현 전략

1. `build.gradle.kts`에 Spring Boot Testcontainers와 MySQL Testcontainers 테스트 의존성을 추가한다.
2. 테스트 실행 시 MySQL 컨테이너가 자동으로 시작되도록 구성한다.
3. Spring Boot 테스트 컨텍스트가 컨테이너 DB에 연결되도록 설정한다.
4. Flyway migration과 초기 데이터가 테스트 DB에 적용되는지 확인한다.
5. `/api/products` 조회 통합 테스트를 추가해 기본 상품 목록을 검증한다.
6. `./gradlew test` 결과를 검증 기록으로 남긴다.

### 작업 규칙

- 한 번에 한 조각만 변경한다.
- 구조 변경과 작동 변경을 섞지 않는다.
- 테스트를 비활성화하거나 회피하지 않는다.
- 로컬 MySQL, 로컬 포트, Kakao API 키에 의존하지 않는다.
- 자세한 결정 배경은 `docs/adr/0001-testcontainers-based-test-system.md`를 따른다.
