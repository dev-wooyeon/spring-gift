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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@Entity
@Table(name = "options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Option {
    private static final int MAX_LENGTH = 50;
    private static final Pattern ALLOWED_PATTERN =
        Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

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
        List<String> errors = validateName(name);
        if (!errors.isEmpty()) {
            throw CatalogException.invalid(String.join(", ", errors));
        }
        if (quantity < 1) {
            throw CatalogException.invalid("옵션 재고 수량은 1 이상이어야 합니다.");
        }
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    public static List<String> validateName(String name) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add("옵션 이름은 필수입니다.");
            return errors;
        }

        if (name.length() > MAX_LENGTH) {
            errors.add("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
        }

        if (!ALLOWED_PATTERN.matcher(name).matches()) {
            errors.add("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }

        return errors;
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
