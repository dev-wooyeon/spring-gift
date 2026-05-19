package gift.catalog.domain;

import gift.catalog.exception.CatalogException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {
    @Test
    @DisplayName("옵션 재고 수량이 1 이상이면 옵션을 생성한다")
    void constructorAcceptsPositiveQuantity() {
        // given
        int quantity = 1;

        // when
        Option option = new Option(product(), "기본 옵션", quantity);

        // then
        assertThat(option.getQuantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("옵션 재고 수량이 0이면 옵션을 생성할 수 없다")
    void constructorRejectsZeroQuantity() {
        // given
        int quantity = 0;

        // when & then
        assertThatThrownBy(() -> new Option(product(), "기본 옵션", quantity))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 재고 수량은 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("옵션 재고 수량이 음수이면 옵션을 생성할 수 없다")
    void constructorRejectsNegativeQuantity() {
        // given
        int quantity = -1;

        // when & then
        assertThatThrownBy(() -> new Option(product(), "기본 옵션", quantity))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 재고 수량은 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("옵션 재고에서 요청 수량만큼 차감한다")
    void subtractQuantityDecreasesQuantity() {
        // given
        Option option = new Option(product(), "기본 옵션", 10);

        // when
        option.subtractQuantity(4);

        // then
        assertThat(option.getQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("옵션 재고 전체 수량을 차감할 수 있다")
    void subtractQuantityAllowsExactStock() {
        // given
        Option option = new Option(product(), "기본 옵션", 10);

        // when
        option.subtractQuantity(10);

        // then
        assertThat(option.getQuantity()).isZero();
    }

    @Test
    @DisplayName("옵션 차감 수량이 0이면 예외가 발생하고 재고는 유지된다")
    void subtractQuantityRejectsZeroAmount() {
        // given
        Option option = new Option(product(), "기본 옵션", 10);

        // when & then
        assertThatThrownBy(() -> option.subtractQuantity(0))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 차감 수량은 1 이상이어야 합니다.");
        assertThat(option.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("옵션 차감 수량이 음수이면 예외가 발생하고 재고는 유지된다")
    void subtractQuantityRejectsNegativeAmount() {
        // given
        Option option = new Option(product(), "기본 옵션", 10);

        // when & then
        assertThatThrownBy(() -> option.subtractQuantity(-1))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 차감 수량은 1 이상이어야 합니다.");
        assertThat(option.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("현재 재고보다 큰 수량을 차감하면 예외가 발생하고 재고는 유지된다")
    void subtractQuantityRejectsAmountGreaterThanStock() {
        // given
        Option option = new Option(product(), "기본 옵션", 10);

        // when & then
        assertThatThrownBy(() -> option.subtractQuantity(11))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 재고가 부족합니다. 차감 수량이 현재 재고보다 많습니다.");
        assertThat(option.getQuantity()).isEqualTo(10);
    }

    private Product product() {
        Category category = new Category("전자기기", "#000000", "https://example.com/category.jpg", "설명");
        return new Product("상품", 1_000, "https://example.com/product.jpg", category);
    }
}
