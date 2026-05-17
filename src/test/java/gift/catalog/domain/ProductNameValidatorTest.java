package gift.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNameValidatorTest {
    @Test
    @DisplayName("상품 이름이 허용 문자와 길이 조건을 만족하면 검증 오류가 없다")
    void validateAcceptsValidName() {
        // given
        String name = "맥북 프로 16";

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("상품 이름이 null이면 필수 입력 오류만 반환한다")
    void validateRejectsNullName() {
        // given
        String name = null;

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).containsExactly("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 이름이 공백이면 필수 입력 오류만 반환한다")
    void validateRejectsBlankName() {
        // given
        String name = "   ";

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).containsExactly("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 이름이 15자를 초과하면 길이 오류를 반환한다")
    void validateRejectsNameLongerThanFifteenCharacters() {
        // given
        String name = "1234567890123456";

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).contains("상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("상품 이름에 허용되지 않는 특수 문자가 있으면 문자 오류를 반환한다")
    void validateRejectsUnsupportedSpecialCharacters() {
        // given
        String name = "상품!";

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).contains("상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }

    @Test
    @DisplayName("카카오가 포함된 상품 이름은 기본 정책에서 오류를 반환한다")
    void validateRejectsKakaoNameByDefault() {
        // given
        String name = "카카오 선물";

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).contains("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("관리자 정책에서는 카카오가 포함된 상품 이름을 허용한다")
    void validateAllowsKakaoNameForAdminPolicy() {
        // given
        String name = "카카오 선물";

        // when
        List<String> errors = ProductNameValidator.validate(name, true);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("여러 검증 조건을 위반하면 모든 오류를 반환한다")
    void validateReturnsAllErrorsExceptRequiredError() {
        // given
        String name = "카카오!!!!!!!!!!!!!";

        // when
        List<String> errors = ProductNameValidator.validate(name);

        // then
        assertThat(errors).containsExactly(
            "상품 이름은 공백을 포함하여 최대 15자까지 입력할 수 있습니다.",
            "상품 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _",
            "\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다."
        );
    }
}
