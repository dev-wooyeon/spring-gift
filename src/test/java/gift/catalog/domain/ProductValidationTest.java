package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductValidationTest {
    private final Category category = new Category("가전", "#000000", "http://image", "가전제품");

    @Test
    @DisplayName("상품 이름이 허용 문자와 길이 조건을 만족하면 에외 없이 정상적으로 생성된다")
    void acceptsValidName() {
        // given
        String name = "맥북 프로 16";

        // when & then
        assertThatCode(() -> new Product(name, 1000, "http://image", category))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("상품 이름이 null이면 필수 입력 예외가 발생한다")
    void rejectsNullName() {
        // given
        String name = null;

        // when & then
        assertThatThrownBy(() -> new Product(name, 1000, "http://image", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 이름이 공백이면 필수 입력 예외가 발생한다")
    void rejectsBlankName() {
        // given
        String name = "   ";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1000, "http://image", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 이름이 15자를 초과하면 길이 초과 예외가 발생한다")
    void rejectsNameLongerThanFifteenCharacters() {
        // given
        String name = "1234567890123456";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1000, "http://image", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("상품 이름에 허용되지 않는 특수 문자가 있으면 특수 문자 정책 위반 예외가 발생한다")
    void rejectsUnsupportedSpecialCharacters() {
        // given
        String name = "상품!";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1000, "http://image", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }

    @Test
    @DisplayName("카카오가 포함된 상품 이름은 기본(API) 정책에서 예외가 발생한다")
    void rejectsKakaoNameByDefault() {
        // given
        String name = "카카오 선물";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1000, "http://image", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("관리자(Admin) 정책에서는 카카오가 포함된 상품 이름을 허용한다")
    void allowsKakaoNameForAdminPolicy() {
        // given
        String name = "카카오 선물";

        // when & then
        assertThatCode(() -> new Product(name, 1000, "http://image", category, true))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 검증 조건을 동시에 위반하면 해당 오류 메시지들이 취합된 예외가 발생한다")
    void returnsAllErrorsExceptRequiredError() {
        // given
        String name = "카카오!!!!!!!!!!!!!";

        // when & then
        assertThatThrownBy(() -> new Product(name, 1000, "http://image", category))
            .isInstanceOf(CatalogException.class)
            .hasMessageContaining("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.")
            .hasMessageContaining("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _")
            .hasMessageContaining("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
    }
}
