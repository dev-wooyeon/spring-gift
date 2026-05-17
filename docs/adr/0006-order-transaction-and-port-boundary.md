# ADR-0006: 주문 협력 경계와 트랜잭션 정책을 명시한다

## 상태

Accepted

## 날짜

2026-05-17

## 맥락

주문 생성은 옵션 재고 차감, 회원 포인트 차감, 주문 저장, Kakao 메시지 발송을 함께 수행한다. 이 흐름을 하나의 서비스 메서드에서 직접 처리하면 Order가 Catalog, Member, Notification 구현에 강하게 묶인다.

또한 `Order`와 `Wish`가 Catalog JPA Entity를 직접 참조하면, 장기적으로 도메인별 데이터 소유권이나 독립 배포 후보를 평가하기 어렵다.

## 결정

1. 주문 생성의 핵심 원자성 범위는 재고 차감, 포인트 차감, 주문 저장까지로 둔다.
2. Kakao 메시지 발송은 주문 저장 커밋 이후 후행 작업으로 처리한다.
3. Order application은 Catalog/Member 서비스를 직접 호출하지 않고, `OrderOptionPort`, `OrderMemberPort`를 통해 협력한다.
4. Notification은 `OrderCreatedEvent`를 구독하고, Kakao 메시지 발송 실패가 주문 성공을 깨지 않도록 예외를 격리한다.
5. `Order`는 Catalog `Option` Entity 대신 `optionId`를 저장한다.
6. `Wish`는 Catalog `Product` Entity 대신 `productId`를 저장하고, 응답에 필요한 상품 정보는 `WishProductPort` read model로 조회한다.

## 현재 경계

- Order core: `OrderService`, `Order`, `OrderCommand`, `OrderCreatedEvent`
- Order outbound ports: `OrderOptionPort`, `OrderMemberPort`
- Order adapters: Catalog option adapter, Member point adapter
- Wish core: `WishService`, `Wish`, `WishCommand`, `WishView`
- Wish outbound port: `WishProductPort`
- Notification listener: 주문 커밋 이후 Kakao 메시지 전송

## 트랜잭션 정책

주문 생성은 다음 규칙을 따른다.

- 옵션이 없으면 주문을 만들지 않고 `404`로 매핑한다.
- 재고가 부족하면 예외를 전파하고 재고, 포인트, 주문 저장을 모두 롤백한다.
- 포인트가 부족하면 예외를 전파하고 재고, 포인트, 주문 저장을 모두 롤백한다.
- 주문 저장이 성공적으로 커밋된 뒤 Kakao 메시지를 발송한다.
- Kakao 메시지 발송 실패는 주문 결과에 영향을 주지 않는다.

## 데이터 소유권 전환 경로

현재 DB에는 `orders.option_id`, `wish.product_id` 외래키가 남아 있다. 이는 데이터 무결성을 유지하기 위한 과도기적 선택이다.

다음 단계는 아래 순서로 진행한다.

1. 코드에서 외부 JPA Entity 직접 참조를 제거한다.
2. 도메인별 read model과 port contract를 고정한다.
3. 도메인 간 FK를 유지할지, 애플리케이션 검증으로 옮길지 결정한다.
4. 독립 배포 후보가 되면 schema ownership과 contract test를 추가한다.
5. 비동기 메시징이 필요해지면 outbox 또는 application event 저장소를 검토한다.

## 결과

- Order/Wish 도메인 Entity는 Catalog JPA Entity를 직접 참조하지 않는다.
- Order application은 Catalog/Member 구현 대신 자기 언어의 port에 의존한다.
- Notification은 주문 생성 본 흐름에서 분리되었고, 커밋 이후 후행 처리된다.
- 아직 독립 배포 완료 상태는 아니다. DB FK, 동기 adapter, Spring transaction은 모놀리스 내부 경계로 남아 있다.
