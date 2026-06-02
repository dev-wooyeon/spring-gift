package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductValidationTest {
    @Test
    @DisplayName("상품 이름이 허용 문자와 길이 조건을 만족하면 성공적으로 객체가 생성된다")
    void createAcceptsValidName() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = "맥북 프로 16";

        // when
        Product product = new Product(name, 1_000_000, "https://example.com/image.jpg", category);

        // then
        assertThat(product.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("상품 이름이 null이면 생성 시 예외가 발생한다")
    void createRejectsNullName() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = null;

        // when & then
        assertThatThrownBy(() -> new Product(name, 1_000_000, "https://example.com/image.jpg", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 이름이 공백이면 생성 시 예외가 발생한다")
    void createRejectsBlankName() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = "   ";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1_000_000, "https://example.com/image.jpg", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 이름이 15자를 초과하면 생성 시 예외가 발생한다")
    void createRejectsNameLongerThanFifteenCharacters() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = "1234567890123456";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1_000_000, "https://example.com/image.jpg", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("상품 이름에 허용되지 않는 특수 문자가 있으면 생성 시 예외가 발생한다")
    void createRejectsUnsupportedSpecialCharacters() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = "상품!";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1_000_000, "https://example.com/image.jpg", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }

    @Test
    @DisplayName("카카오가 포함된 상품 이름은 기본 정책에서 생성 시 예외가 발생한다")
    void createRejectsKakaoNameByDefault() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = "카카오 선물";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1_000_000, "https://example.com/image.jpg", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("관리자 정책에서는 카카오가 포함된 상품 이름을 허용한다")
    void createAllowsKakaoNameForAdminPolicy() {
        // given
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        String name = "카카오 선물";

        // when
        Product product = new Product(name, 1_000_000, "https://example.com/image.jpg", category, true);

        // then
        assertThat(product.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("오류 수집 메서드는 유효하지 않은 여러 검증 조건의 메시지를 모두 수집하여 반환한다")
    void checkErrorsReturnsAllErrorsExceptRequiredError() {
        // given
        String name = "카카오!!!!!!!!!!!!!";

        // when
        List<String> errors = Product.checkErrors(name, false);

        // then
        assertThat(errors).containsExactly(
            "상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.",
            "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _",
            "\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."
        );
    }
}
