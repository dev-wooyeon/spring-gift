package gift.point.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointTest {
    @Test
    @DisplayName("포인트 충전 금액이 1 이상이면 보유 포인트가 증가한다")
    void chargeIncreasesAmount() {
        // given
        Point point = Point.zero();

        // when
        point.charge(10_000);
        point.charge(5_000);

        // then
        assertThat(point.getAmount()).isEqualTo(15_000);
    }

    @Test
    @DisplayName("포인트 충전 금액이 0이면 예외가 발생하고 포인트는 유지된다")
    void chargeRejectsZeroAmount() {
        // given
        Point point = Point.zero();

        // when & then
        assertThatThrownBy(() -> point.charge(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 충전 금액은 1 이상이어야 합니다.");
        assertThat(point.getAmount()).isZero();
    }

    @Test
    @DisplayName("포인트 충전 금액이 음수이면 예외가 발생하고 포인트는 유지된다")
    void chargeRejectsNegativeAmount() {
        // given
        Point point = Point.zero();

        // when & then
        assertThatThrownBy(() -> point.charge(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 충전 금액은 1 이상이어야 합니다.");
        assertThat(point.getAmount()).isZero();
    }

    @Test
    @DisplayName("포인트 차감 금액이 보유 포인트 이하이면 포인트가 감소한다")
    void deductDecreasesAmount() {
        // given
        Point point = new Point(10_000);

        // when
        point.deduct(4_000);

        // then
        assertThat(point.getAmount()).isEqualTo(6_000);
    }

    @Test
    @DisplayName("보유 포인트 전체를 차감할 수 있다")
    void deductAllowsExactBalance() {
        // given
        Point point = new Point(10_000);

        // when
        point.deduct(10_000);

        // then
        assertThat(point.getAmount()).isZero();
    }

    @Test
    @DisplayName("포인트 차감 금액이 0이면 예외가 발생하고 포인트는 유지된다")
    void deductRejectsZeroAmount() {
        // given
        Point point = new Point(10_000);

        // when & then
        assertThatThrownBy(() -> point.deduct(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 차감 금액은 1 이상이어야 합니다.");
        assertThat(point.getAmount()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("포인트 차감 금액이 음수이면 예외가 발생하고 포인트는 유지된다")
    void deductRejectsNegativeAmount() {
        // given
        Point point = new Point(10_000);

        // when & then
        assertThatThrownBy(() -> point.deduct(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트 차감 금액은 1 이상이어야 합니다.");
        assertThat(point.getAmount()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("보유 포인트보다 큰 금액을 차감하면 예외가 발생하고 포인트는 유지된다")
    void deductRejectsAmountGreaterThanBalance() {
        // given
        Point point = new Point(10_000);

        // when & then
        assertThatThrownBy(() -> point.deduct(10_001))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트가 부족합니다.");
        assertThat(point.getAmount()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("포인트는 음수로 생성할 수 없다")
    void constructorRejectsNegativeAmount() {
        // given
        int amount = -1;

        // when & then
        assertThatThrownBy(() -> new Point(amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("회원 포인트는 음수일 수 없습니다.");
    }
}
