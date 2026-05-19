package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int quantity;

    public Option(Product product, String name, int quantity) {
        if (quantity < 1) {
            throw CatalogException.invalid("옵션 재고 수량은 1 이상이어야 합니다.");
        }
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    public void subtractQuantity(int amount) {
        if (amount < 1) {
            throw CatalogException.invalid("옵션 차감 수량은 1 이상이어야 합니다.");
        }
        if (amount > this.quantity) {
            throw CatalogException.invalid("옵션 재고가 부족합니다. 차감 수량이 현재 재고보다 많습니다.");
        }
        this.quantity -= amount;
    }
}
