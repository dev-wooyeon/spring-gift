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

- [x] Spring Boot 테스트가 MySQL 컨테이너를 띄울 수 있는 통합 테스트 클래스를 추가한다.
- 완료 기준: 테스트 실행 시 MySQL 컨테이너가 시작되고 Spring ApplicationContext가 로드된다.
- 검증 명령: `./gradlew test`

### 3. Flyway migration 적용 검증

- [x] 테스트 DB에 Flyway migration이 적용되었는지 검증하는 테스트를 추가한다.
- 완료 기준: 테스트에서 기본 테이블 또는 초기 데이터 존재를 확인한다.
- 검증 명령: `./gradlew test`

### 4. `/api/products` 통합 테스트 추가

- [x] MySQL Testcontainers 환경에서 `/api/products` 조회 API를 검증한다.
- 완료 기준: 응답 status가 `200 OK`이고 초기 상품 데이터가 응답에 포함된다.
- 검증 명령: `./gradlew test`

### 5. 테스트 실행 문서화

- [x] README에 Docker 필요 조건과 테스트 실행 방법을 추가한다.
- 완료 기준: 처음 보는 사람이 `./gradlew test` 실행 전 필요한 조건을 알 수 있다.
- 검증 명령: 문서 검토

### 6. 검증 기록 업데이트

- [x] `docs/분석.md` 또는 별도 검증 기록 문서에 Testcontainers 테스트 결과를 남긴다.
- 완료 기준: 실행 명령, 성공 여부, 확인한 동작이 기록되어 있다.
- 검증 명령: `./gradlew test`

### 7. 다음 품질 게이트 후보 결정

- [x] JaCoCo, ArchUnit, Gradle `check` 확장, Docker Compose 중 다음 확장 대상을 하나만 결정한다.
- 완료 기준: 선택지가 2개 이상이면 ADR을 작성한다.
- 검증 명령: 문서 검토

### 8. `check` 품질 게이트에 추가할 다음 검증 후보 결정

- [x] JaCoCo 커버리지 기준과 ArchUnit 아키텍처 규칙 중 다음에 추가할 검증을 하나만 결정한다.
- 완료 기준: 선택지가 2개 이상이면 ADR을 작성한다.
- 검증 명령: 문서 검토

### 9. JaCoCo 커버리지 게이트 추가

- [x] JaCoCo 플러그인을 추가하고 `./gradlew check`에서 커버리지 검증이 실행되도록 구성한다.
- 완료 기준: 커버리지 리포트와 검증 태스크가 생성되고, 기준을 만족하지 못하면 `check`가 실패한다.
- 검증 명령: `./gradlew check`

### 10. 리팩토링 전 characterization test 작업 분해

- [x] 리팩토링 대상의 현재 동작 고정 작업을 주요 도메인별 Todo로 나눈다.
- 완료 기준: 각 항목이 하나의 커밋 후보가 될 수 있는 크기로 나뉘어 있다.
- 검증 명령: 문서 검토

### 11. 상품/카테고리 characterization test 보강

- [x] 상품 조회, 단건 조회, 생성, 수정, 삭제와 카테고리 조회/생성/수정/삭제의 현재 HTTP 응답과 DB 상태를 테스트로 고정한다.
- 완료 기준: 상품명/카테고리 validation, 존재하지 않는 ID, 기본 데이터 조회 흐름이 테스트로 설명되고 JaCoCo 기준을 만족한다.
- 검증 명령: `./gradlew check`

### 12. 회원/인증 characterization test 보강

- [x] 회원 가입, 로그인, 중복 이메일, 잘못된 비밀번호, JWT 생성/해석, Kakao callback의 현재 동작을 테스트로 고정한다.
- 완료 기준: 성공/실패 응답과 토큰 기반 회원 식별 흐름이 테스트로 설명되고 JaCoCo 기준을 만족한다.
- 검증 명령: `./gradlew check`

### 13. 위시 characterization test 보강

- [x] 위시 목록 조회, 추가, 삭제, 인증 헤더 누락/오류의 현재 동작을 테스트로 고정한다.
- 완료 기준: JWT 인증이 필요한 흐름과 DB 반영 결과가 테스트로 설명되고 JaCoCo 기준을 만족한다.
- 검증 명령: `./gradlew check`

### 14. 주문 characterization test 보강

- [x] 주문 생성, 옵션 수량 차감, 회원 포인트 차감, 메시지 처리, 재고/포인트 부족의 현재 동작을 테스트로 고정한다.
- 완료 기준: 주문 성공/실패의 HTTP 응답과 DB 상태 변화가 테스트로 설명되고 JaCoCo 기준을 만족한다.
- 검증 명령: `./gradlew check`

### 15. 관리자 화면 characterization test 보강

- [x] 관리자 상품/회원 HTML 화면의 목록, 신규, 수정, 삭제, 포인트 충전 흐름의 현재 동작을 테스트로 고정한다.
- 완료 기준: 현재 무인증 접근 기준선, form submit 결과, validation 오류 표시 흐름이 테스트로 설명되고 JaCoCo 기준을 만족한다.
- 검증 명령: `./gradlew check`

### 16. 커버리지 기준 상향

- [x] 도메인별 characterization test 보강 후 JaCoCo 라인 커버리지 기준을 현재 33%보다 높인다.
- 완료 기준: 새 기준을 만족하지 못하면 `./gradlew check`가 실패하고, 기준 상향 근거가 검증 기록에 남아 있다.
- 검증 명령: `./gradlew check`

### 17. 상품/카테고리 서비스 계층 분리

- [x] 상품/카테고리 API와 관리자 상품 화면에서 Repository 직접 접근을 서비스 계층으로 이동한다.
- 완료 기준: HTTP 응답, 관리자 화면 흐름, DB 상태 변화가 기존 characterization test와 동일하게 유지된다.
- 검증 명령: `./gradlew check`

### 18. 회원/인증 서비스 계층 분리

- [x] 회원 가입, 로그인, Kakao 인증, 관리자 회원 화면에서 Repository 직접 접근을 서비스 계층으로 이동한다.
- 완료 기준: 회원/인증 API 응답, JWT 발급/해석, Kakao callback mock 흐름, 관리자 회원 화면의 현재 동작이 기존 characterization test와 동일하게 유지된다.
- 검증 명령: `./gradlew check`

### 19. 위시 서비스 계층 분리

- [x] 위시 목록 조회, 추가, 삭제 흐름에서 Repository 직접 접근과 인증 회원 확인 흐름을 서비스 계층으로 이동한다.
- 완료 기준: 인증 헤더 누락/오류, 상품 없음, 중복 위시, 타 회원 위시 삭제 거부, DB 상태 변화가 기존 characterization test와 동일하게 유지된다.
- 검증 명령: `./gradlew check`

### 20. 주문 서비스 계층 분리

- [x] 주문 목록 조회와 주문 생성 흐름에서 옵션 조회, 재고 차감, 포인트 차감, 주문 저장, Kakao 메시지 발송을 서비스 계층으로 이동한다.
- 완료 기준: 주문 성공/실패 응답, 현재 예외 전파 방식, 재고/포인트 DB 상태 변화, Kakao 메시지 mock 호출이 기존 characterization test와 동일하게 유지된다.
- 검증 명령: `./gradlew check`

### 21. 옵션 characterization test 보강

- [x] 옵션 목록 조회, 생성, 삭제, 상품 없음, 중복 옵션명, 마지막 옵션 삭제 거부, 다른 상품 옵션 삭제를 테스트로 고정한다.
- 완료 기준: 옵션 컨트롤러의 현재 HTTP 응답과 DB 상태 변화가 리팩토링 전 테스트로 설명된다.
- 검증 명령: `./gradlew check`

### 22. 옵션 서비스 계층 분리

- [ ] 옵션 목록 조회, 생성, 삭제 흐름에서 ProductRepository와 OptionRepository 직접 접근을 서비스 계층으로 이동한다.
- 완료 기준: 옵션 characterization test와 전체 `check`가 통과하고, 컨트롤러에는 HTTP 응답 조립 책임만 남는다.
- 검증 명령: `./gradlew check`

### 23. 리팩토링 완료 검증 및 문서 정리

- [ ] 전체 도메인 서비스 계층 분리 결과를 검증 기록에 남기고 다음 품질 강화 후보를 정리한다.
- 완료 기준: `./gradlew check`가 통과하고, README와 분석 문서가 현재 구조와 다음 작업을 설명한다.
- 검증 명령: `./gradlew check`
