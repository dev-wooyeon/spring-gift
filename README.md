# spring-gift

Spring Boot 기반 선물하기 서비스입니다. 상품, 카테고리, 회원, 위시, 주문 기능을 제공하며 관리자용 HTML 화면과 고객용 REST API를 함께 다룹니다.

## 현재 목표

개인 로컬 설정에 의존하지 않고 누구나 동일하게 실행할 수 있는 테스트 시스템을 구축한다. 첫 단계는 MySQL Testcontainers 기반 통합 테스트를 추가하여 `./gradlew test`만으로 운영에 가까운 DB 환경과 Flyway migration 결과를 검증하는 것이다.

## 다음 작업

- [ ] MySQL Testcontainers 기반 통합 테스트 추가

## 구현 전략

1. `build.gradle.kts`에 Spring Boot Testcontainers와 MySQL Testcontainers 테스트 의존성을 추가한다.
2. 테스트 실행 시 MySQL 컨테이너가 자동으로 시작되도록 구성한다.
3. Spring Boot 테스트 컨텍스트가 컨테이너 DB에 연결되도록 설정한다.
4. Flyway migration과 초기 데이터가 테스트 DB에 적용되는지 확인한다.
5. `/api/products` 조회 통합 테스트를 추가해 기본 상품 목록을 검증한다.
6. `./gradlew test` 결과를 검증 기록으로 남긴다.

## 실행

```bash
./gradlew test
```

현재 Testcontainers 기반 통합 테스트를 준비 중이다. Docker Desktop 또는 Docker Engine이 준비되면 MySQL 컨테이너 기반 테스트까지 같은 명령으로 실행되도록 확장한다.

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
- `docs/분석.md`: 관리자 화면 기준선과 비즈니스 흐름 분석
