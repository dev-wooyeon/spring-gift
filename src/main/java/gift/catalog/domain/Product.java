package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    private static final int MAX_LENGTH = 15;
    private static final Pattern ALLOWED_PATTERN =
        Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int price;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    public Product(String name, int price, String imageUrl, Category category) {
        this(name, price, imageUrl, category, false);
    }

    public Product(String name, int price, String imageUrl, Category category, boolean allowKakao) {
        checkName(name, allowKakao);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void update(String name, int price, String imageUrl, Category category) {
        update(name, price, imageUrl, category, false);
    }

    public void update(String name, int price, String imageUrl, Category category, boolean allowKakao) {
        checkName(name, allowKakao);
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public static List<String> validateName(String name, boolean allowKakao) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add("상품 이름은 필수입니다.");
            return errors;
        }

        if (name.length() > MAX_LENGTH) {
            errors.add("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
        }

        if (!ALLOWED_PATTERN.matcher(name).matches()) {
            errors.add("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
        }

        if (!allowKakao && name.contains("카카오")) {
            errors.add("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }

        return errors;
    }

    private void checkName(String name, boolean allowKakao) {
        List<String> errors = validateName(name, allowKakao);
        if (!errors.isEmpty()) {
            throw CatalogException.invalid(String.join(", ", errors));
        }
    }
}
