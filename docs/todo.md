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

- [x] 옵션 목록 조회, 생성, 삭제 흐름에서 ProductRepository와 OptionRepository 직접 접근을 서비스 계층으로 이동한다.
- 완료 기준: 옵션 characterization test와 전체 `check`가 통과하고, 컨트롤러에는 HTTP 응답 조립 책임만 남는다.
- 검증 명령: `./gradlew check`

### 23. 리팩토링 완료 검증 및 문서 정리

- [x] 전체 도메인 서비스 계층 분리 결과를 검증 기록에 남기고 다음 품질 강화 후보를 정리한다.
- 완료 기준: `./gradlew check`가 통과하고, README와 분석 문서가 현재 구조와 다음 작업을 설명한다.
- 검증 명령: `./gradlew check`

### 24. ArchUnit 아키텍처 규칙 도입

- [x] Controller가 Repository에 직접 의존하지 않고 Service를 통해 도메인 흐름을 실행하는 규칙을 `check` 품질 게이트에 추가한다.
- 완료 기준: Controller -> Service -> Repository 의존 방향이 자동 검증되고, 위반 시 `./gradlew check`가 실패한다.
- 검증 명령: `./gradlew check`

### 25. 서비스 계층의 HTTP/Auth 결합 제거

- [x] `WishService`, `OrderService`, `OptionService`에서 HTTP 상태를 표현하는 `Result` 타입과 Authorization 헤더 의존을 제거한다.
- 완료 기준: 인증 헤더 해석과 HTTP 응답 매핑은 Controller가 담당하고, Service는 도메인 입력과 결과만 다룬다.
- 검증 명령: `./gradlew check`

### 26. 서비스/DTO 책임 분리 후보 정리

- [x] Service가 Response DTO를 직접 반환하는 흐름을 제거하고, API DTO 매핑은 Controller에서 수행한다.
- 완료 기준: Service는 Entity, primitive, 도메인 결과만 반환하고 HTTP Response DTO를 직접 생성하지 않는다.
- 검증 명령: `./gradlew check`

### 27. 남은 리팩토링 후보 분해

- [x] Admin/API use case 분리, 전역 예외 처리, 도메인별 예외 타입, 주문 트랜잭션 정책, primitive FK 제거 후보를 작업 단위로 분해한다.
- 완료 기준: 각 후보가 동작 변경 여부와 검증 기준을 포함한 커밋 가능한 크기로 정리된다.
- 검증 명령: 문서 검토

### 28. 모듈러 모놀리스 경계 정리

- [x] 도메인별 `presentation`, `application`, `domain`, `infrastructure` 책임을 나누고 외부 도메인 직접 참조를 줄인다.
- 완료 기준: 각 도메인의 Controller는 application service만 호출하고, Repository/JPA Entity 직접 참조는 도메인 내부로 제한된다.
- 검증 명령: `./gradlew check`

### 29. 도메인 독립 배포 후보 평가

- [x] Catalog, Member/Auth, Wish, Order, Notification의 데이터 소유권, API contract, 이벤트 경계, 트랜잭션 정책을 정리한다.
- 완료 기준: 독립 배포 가능한 도메인과 아직 모놀리스 내부에 남겨야 할 도메인이 구분된다.
- 검증 명령: ADR 검토

### 30. ArchUnit 도메인 계층 규칙 도입

- [x] `presentation`의 Repository 접근, `application`의 `presentation` import, 다른 도메인의 `infrastructure` import를 금지하는 규칙을 추가한다.
- 완료 기준: 패키지 계층 위반 시 `./gradlew check`가 실패한다.
- 검증 명령: `./gradlew check`

### 31. 도메인 간 port 후보 분리

- [x] Order가 Catalog/Member 기능을 직접 서비스 호출로 묶는 지점을 port 인터페이스 후보로 분리한다.
- 완료 기준: Order application은 필요한 협력 기능을 자기 언어의 port로 표현하고 adapter가 실제 도메인 서비스를 호출한다.
- 검증 명령: `./gradlew check`

### 32. 주문 트랜잭션 정책 결정

- [x] 재고 차감, 포인트 차감, 주문 저장의 원자성 범위를 ADR로 결정하고 테스트를 정책에 맞게 조정한다.
- 완료 기준: 포인트 부족, 재고 부족, 주문 저장 실패의 DB 상태 변화가 명확한 테스트로 고정된다.
- 검증 명령: `./gradlew check`

### 33. Notification 이벤트 분리

- [x] Kakao 메시지 발송을 주문 생성 본 흐름에서 domain event 또는 application event 후행 작업으로 분리한다.
- 완료 기준: 알림 실패가 주문 성공 여부에 영향을 주지 않는 정책이 테스트로 검증된다.
- 검증 명령: `./gradlew check`

### 34. 외부 도메인 Entity 참조 축소

- [x] Wish/Order의 Catalog Entity 직접 참조를 ID 또는 read model 후보로 축소할 수 있는 migration 경로를 설계한다.
- 완료 기준: 데이터 소유권, 조회 성능, API 응답 영향이 문서화되고 단계별 변경 커밋이 분해된다.
- 검증 명령: 문서 검토

### 35. 테스트 패키지 도메인별 정렬

- [ ] `src/test/java/gift` 아래의 characterization test를 도메인별 패키지로 옮긴다.
- 완료 기준: 테스트 위치만 봐도 Catalog, Member/Auth, Wish, Order, Admin 흐름을 구분할 수 있다.
- 검증 명령: `./gradlew check`

### 36. Application port adapter 규칙 강화

- [ ] 다른 도메인 Entity 참조가 adapter 패키지에만 머무르는지 ArchUnit 규칙으로 검증한다.
- 완료 기준: Order/Wish application/domain에서 Catalog Entity를 직접 import하면 `./gradlew check`가 실패한다.
- 검증 명령: `./gradlew check`
