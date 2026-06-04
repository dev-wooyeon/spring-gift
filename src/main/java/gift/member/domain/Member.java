package gift.member.domain;

import gift.point.domain.Point;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Represents a registered member.
 *
 * @author brian.kim
 * @since 1.0
 */
@Getter
@Entity
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String password;

    private String kakaoAccessToken;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "point", nullable = false))
    private Point point = Point.zero();

    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Member(String email) {
        this.email = email;
    }

    public void update(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void updateKakaoAccessToken(String kakaoAccessToken) {
        this.kakaoAccessToken = kakaoAccessToken;
    }

    public int getPoint() {
        return point.getAmount();
    }

    public void chargePoint(int amount) {
        point.charge(amount);
    }

    public void deductPoint(int amount) {
        point.deduct(amount);
    }
}
