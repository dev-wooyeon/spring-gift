package gift.wish.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "product_id", nullable = false)
    private Long productId;

    public Wish(Long memberId, Long productId) {
        this.memberId = memberId;
        this.productId = productId;
    }
}
