package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionValidationTest {
    @Test
    @DisplayName("옵션 이름이 허용 문자와 길이 조건을 만족하면 성공적으로 객체가 생성된다")
    void createAcceptsValidName() {
        // given
        Product product = product();
        String name = "블랙 + 256GB";

        // when
        Option option = new Option(product, name, 10);

        // then
        assertThat(option.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("옵션 이름이 null이면 생성 시 예외가 발생한다")
    void createRejectsNullName() {
        // given
        Product product = product();
        String name = null;

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 이름이 공백이면 생성 시 예외가 발생한다")
    void createRejectsBlankName() {
        // given
        Product product = product();
        String name = "   ";

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 이름이 50자를 초과하면 생성 시 예외가 발생한다")
    void createRejectsNameLongerThanFiftyCharacters() {
        // given
        Product product = product();
        String name = "a".repeat(51);

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("옵션 이름에 허용되지 않는 특수 문자가 있으면 생성 시 예외가 발생한다")
    void createRejectsUnsupportedSpecialCharacters() {
        // given
        Product product = product();
        String name = "옵션!";

        // when & then
        assertThatThrownBy(() -> new Option(product, name, 10))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }

    @Test
    @DisplayName("오류 수집 메서드는 유효하지 않은 여러 검증 조건의 메시지를 모두 수집하여 반환한다")
    void checkErrorsReturnsAllErrorsExceptRequiredError() {
        // given
        String name = "a".repeat(51) + "!";

        // when
        List<String> errors = Option.checkErrors(name);

        // then
        assertThat(errors).containsExactly(
            "옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.",
            "옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _"
        );
    }

    private Product product() {
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        return new Product("상품", 1_000, "https://example.com/product.jpg", category);
    }
}
