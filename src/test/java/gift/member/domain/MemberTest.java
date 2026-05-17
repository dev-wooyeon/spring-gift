package gift.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {
    @Test
    @DisplayName("포인트 충전 금액이 1 이상이면 보유 포인트가 증가한다")
    void chargePointIncreasesPoint() {
        // given
        Member member = new Member("member@example.com", "password");

        // when
        member.chargePoint(10_000);
        member.chargePoint(5_000);

        // then
        assertThat(member.getPoint()).isEqualTo(15_000);
    }

    @Test
    @DisplayName("포인트 충전 금액이 0이면 예외가 발생하고 포인트는 유지된다")
    void chargePointRejectsZeroAmount() {
        // given
        Member member = new Member("member@example.com", "password");

        // when & then
        assertThatThrownBy(() -> member.chargePoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 충전 금액은 1 이상이어야 합니다.");
        assertThat(member.getPoint()).isZero();
    }

    @Test
    @DisplayName("포인트 충전 금액이 음수이면 예외가 발생하고 포인트는 유지된다")
    void chargePointRejectsNegativeAmount() {
        // given
        Member member = new Member("member@example.com", "password");

        // when & then
        assertThatThrownBy(() -> member.chargePoint(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 충전 금액은 1 이상이어야 합니다.");
        assertThat(member.getPoint()).isZero();
    }

    @Test
    @DisplayName("포인트 차감 금액이 보유 포인트 이하이면 포인트가 감소한다")
    void deductPointDecreasesPoint() {
        // given
        Member member = new Member("member@example.com", "password");
        member.chargePoint(10_000);

        // when
        member.deductPoint(4_000);

        // then
        assertThat(member.getPoint()).isEqualTo(6_000);
    }

    @Test
    @DisplayName("보유 포인트 전체를 차감할 수 있다")
    void deductPointAllowsExactBalance() {
        // given
        Member member = new Member("member@example.com", "password");
        member.chargePoint(10_000);

        // when
        member.deductPoint(10_000);

        // then
        assertThat(member.getPoint()).isZero();
    }

    @Test
    @DisplayName("포인트 차감 금액이 0이면 예외가 발생하고 포인트는 유지된다")
    void deductPointRejectsZeroAmount() {
        // given
        Member member = new Member("member@example.com", "password");
        member.chargePoint(10_000);

        // when & then
        assertThatThrownBy(() -> member.deductPoint(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 차감 금액은 1 이상이어야 합니다.");
        assertThat(member.getPoint()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("포인트 차감 금액이 음수이면 예외가 발생하고 포인트는 유지된다")
    void deductPointRejectsNegativeAmount() {
        // given
        Member member = new Member("member@example.com", "password");
        member.chargePoint(10_000);

        // when & then
        assertThatThrownBy(() -> member.deductPoint(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 차감 금액은 1 이상이어야 합니다.");
        assertThat(member.getPoint()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("보유 포인트보다 큰 금액을 차감하면 예외가 발생하고 포인트는 유지된다")
    void deductPointRejectsAmountGreaterThanBalance() {
        // given
        Member member = new Member("member@example.com", "password");
        member.chargePoint(10_000);

        // when & then
        assertThatThrownBy(() -> member.deductPoint(10_001))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트가 부족합니다.");
        assertThat(member.getPoint()).isEqualTo(10_000);
    }
}
