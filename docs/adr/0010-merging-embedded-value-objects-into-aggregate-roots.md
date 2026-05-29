# ADR-0010: 임베디드 값 객체(Value Object)인 Point를 Member 도메인 내부로 통합한다

## 상태

Accepted

## 날짜

2026-05-29

## 맥락

기존 구조에서는 포인트(`point`) 도메인이 `catalog`, `member`, `order`와 동등한 위상을 가진 독립적인 탑레벨 패키지로 분리되어 있었다.
```text
gift
├── member
└── point
```

그러나 DDD(도메인 주도 설계) 및 실제 DB 스키마 관점에서 분석해 보면 다음과 같은 모순과 불일치가 발견된다.
1. **독립 애그리거트의 부재**: `Point`는 자신만의 JPA Repository, Controller, Service를 가지지 않으며 데이터베이스에서도 단독 테이블로 존재하지 않는다.
2. **Member 애그리거트의 일부**: `Point`는 `Member` 엔티티 내부의 `@Embedded` 필드로 매핑되어 `member` 테이블의 한 컬럼으로 관리되는 전형적인 **임베디드 값 객체(Value Object)**이다.

포인트를 독립 도메인 패키지로 둔 구조는 제3자가 코드를 읽을 때 "포인트가 독립적인 애그리거트 루트인가?"라는 착각을 불러일으켜 **직관성**을 떨어뜨리고, Bounded Context 설계 원칙의 **일관성**을 위배한다.

## 결정

1. 탑레벨 `gift.point` 패키지를 완전히 삭제하고, 해당 코드를 `gift.member` 패키지 내부로 흡수시킨다.
   - `gift.point.domain.Point` -> `gift.member.domain.Point` (패키지 선언 변경)
   - `gift.point.exception.PointException` -> `gift.member.exception.PointException` (패키지 선언 변경)
2. 테스트 패키지 구조 역시 동일하게 정렬한다.
   - `gift.point.domain.PointTest` -> `gift.member.domain.PointTest` (패키지 선언 변경)
3. 코드 전반의 임포트 경로를 수정하고, `Member` 엔티티와 `Point` 값 객체가 동일 패키지(`gift.member.domain`)에 거주하게 만듦으로써 임포트 구문 없이 투명하게 협력하도록 정렬한다.

## 트레이드오프

### 장점 (Pros)
- **도메인 경계의 극대화된 직관성**: 이제 탑레벨 디렉토리만 보더라도 Bounded Context와 애그리거트 루트들이 무엇인지 100% 명확히 일치한다. 불필요하게 쪼개진 탑레벨 폴더가 없어져 인지 부하가 줄어든다.
- **DDD 원칙 준수**: 임베디드 값 객체가 상위 애그리거트 루트 패키지 안에 위치하여, 응집력 있는 도메인 경계를 형성한다.
- **메인과 테스트 구조의 영구적 일치**: 메인 패키지와 테스트 패키지 양쪽에서 동일하게 병합이 수행되어 일관성이 지켜진다.

### 단점 (Cons)
- **임포트 경로 수정 비용**: 물리적 파일 이동과 임포트 교정 작업이 발생한다. (단, 참조 범위가 매우 적어 안전하게 수행 가능하다.)

## 결과

- `spring-gift` 시스템의 디렉토리 구조는 완벽한 DDD 설계 원칙에 부합하게 정렬되었다.
- 훗날 유지보수자가 시스템을 탐색할 때, 회원 테이블에 임베디드된 포인트 모델을 찾기 위해 다른 폴더를 방황하지 않고 `member` 도메인 안에서 원스톱으로 직관적으로 파악할 수 있다.
