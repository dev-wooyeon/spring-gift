package gift.point.domain;

import gift.point.exception.PointException;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {
    private int amount;

    public Point(int amount) {
        if (amount < 0) {
            throw PointException.invalid("회원 포인트는 음수일 수 없습니다.");
        }
        this.amount = amount;
    }

    public static Point zero() {
        return new Point(0);
    }

    public void charge(int amount) {
        if (amount <= 0) {
            throw PointException.invalid("회원 포인트 충전 금액은 1 이상이어야 합니다.");
        }
        this.amount += amount;
    }

    public void deduct(int amount) {
        if (amount <= 0) {
            throw PointException.invalid("회원 포인트 차감 금액은 1 이상이어야 합니다.");
        }
        if (amount > this.amount) {
            throw PointException.invalid("회원 포인트가 부족합니다.");
        }
        this.amount -= amount;
    }
}
