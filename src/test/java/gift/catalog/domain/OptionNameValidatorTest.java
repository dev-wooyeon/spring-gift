package gift.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptionNameValidatorTest {
    @Test
    @DisplayName("옵션 이름이 허용 문자와 길이 조건을 만족하면 검증 오류가 없다")
    void validateAcceptsValidName() {
        // given
        String name = "블랙 + 256GB";

        // when
        List<String> errors = OptionNameValidator.validate(name);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("옵션 이름이 null이면 필수 입력 오류만 반환한다")
    void validateRejectsNullName() {
        // given
        String name = null;

        // when
        List<String> errors = OptionNameValidator.validate(name);

        // then
        assertThat(errors).containsExactly("옵션 이름은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 이름이 공백이면 필수 입력 오류만 반환한다")
    void validateRejectsBlankName() {
        // given
        String name = "   ";

        // when
        List<String> errors = OptionNameValidator.validate(name);

        // then
        assertThat(errors).containsExactly("옵션 이름은 필수입니다.");
    }

    @Test
    @DisplayName("옵션 이름이 50자를 초과하면 길이 오류를 반환한다")
    void validateRejectsNameLongerThanFiftyCharacters() {
        // given
        String name = "a".repeat(51);

        // when
        List<String> errors = OptionNameValidator.validate(name);

        // then
        assertThat(errors).contains("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
    }

    @Test
    @DisplayName("옵션 이름에 허용되지 않는 특수 문자가 있으면 문자 오류를 반환한다")
    void validateRejectsUnsupportedSpecialCharacters() {
        // given
        String name = "옵션!";

        // when
        List<String> errors = OptionNameValidator.validate(name);

        // then
        assertThat(errors).contains("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
    }

    @Test
    @DisplayName("여러 검증 조건을 위반하면 모든 오류를 반환한다")
    void validateReturnsAllErrorsExceptRequiredError() {
        // given
        String name = "a".repeat(51) + "!";

        // when
        List<String> errors = OptionNameValidator.validate(name);

        // then
        assertThat(errors).containsExactly(
            "옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.",
            "옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _"
        );
    }
}
