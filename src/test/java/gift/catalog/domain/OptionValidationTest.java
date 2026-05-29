package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionValidationTest {
    private final Category category = new Category("가전", "#000000", "http://image", "가전제품");
    private final Product product = new Product("맥북", 1000, "http://image", category);

    @Test
    @DisplayName("옵션 이름이 허용 문자와 길이 조건을 만족하고 수량이 1 이상이면 예외 없이 정상적으로 생성된다")
    void acceptsValidNameAndQuantity() {
        // given
        String name = "블랙 + 256GB";
        int quantity = 10;

        // when & then
        assertThatCode(() -> new Option(product, name, quantity))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("옵션 이름이 null이면 필수 입력 예외가 발생한다")
    void rejectsNullName() {
        // given
        String name = null;

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 이름이 공백이면 필수 입력 예외가 발생한다")
    void rejectsBlankName() {
        // given
        String name = "   ";

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 이름이 50자를 초과하면 길이 초과 예외가 발생한다")
    void rejectsNameLongerThanFiftyCharacters() {
        // given
        String name = "a".repeat(51);

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("옵션 이름에 허용되지 않는 특수 문자가 있으면 특수 문자 정책 위반 예외가 발생한다")
    void rejectsUnsupportedSpecialCharacters() {
        // given
        String name = "옵션!";

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }

    @Test
    @DisplayName("옵션 수량이 1 미만이면 수량 범위 위반 예외가 발생한다")
    void rejectsInvalidQuantity() {
        // given
        String name = "블랙";
        int quantity = 0;

        // when & then
        assertThatThrownBy(() -> new Option(product, name, quantity))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 재고 수량은 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("여러 검증 조건을 위반하면 모든 오류를 취합한 예외가 발생한다")
    void returnsAllErrorsExceptRequiredError() {
        // given
        String name = "a".repeat(51) + "!";

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.")
            .hasMessageContaining("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }
}
