package gift.catalog.application;

import gift.catalog.domain.Category;
import gift.catalog.domain.Option;
import gift.catalog.domain.Product;
import gift.catalog.exception.CatalogException;
import gift.catalog.infrastructure.OptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {
    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OptionService optionService;

    @Test
    @DisplayName("옵션 목록 조회는 상품이 있으면 상품의 옵션 목록을 반환한다")
    void getOptionsReturnsProductOptions() {
        // given
        Product product = product(1L);
        Option option = option(10L, product, "기본 옵션", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(option));

        // when
        Optional<List<Option>> result = optionService.getOptions(1L);

        // then
        assertThat(result).hasValue(List.of(option));
    }

    @Test
    @DisplayName("옵션 목록 조회는 상품이 없으면 빈 결과를 반환한다")
    void getOptionsReturnsEmptyWhenProductDoesNotExist() {
        // given
        when(productService.findProduct(1L)).thenReturn(Optional.empty());

        // when
        Optional<List<Option>> result = optionService.getOptions(1L);

        // then
        assertThat(result).isEmpty();
        verify(optionRepository, never()).findByProductId(1L);
    }

    @Test
    @DisplayName("옵션 생성은 상품이 있고 중복 이름이 아니면 옵션을 저장한다")
    void createOptionSavesOption() {
        // given
        Product product = product(1L);
        OptionCommand command = new OptionCommand("블랙", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.existsByProductIdAndName(1L, "블랙")).thenReturn(false);
        when(optionRepository.save(any(Option.class))).thenAnswer(invocation -> {
            Option saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        // when
        Optional<Option> result = optionService.createOption(1L, command);

        // then
        ArgumentCaptor<Option> optionCaptor = ArgumentCaptor.forClass(Option.class);
        verify(optionRepository).save(optionCaptor.capture());
        assertThat(optionCaptor.getValue().getProduct()).isEqualTo(product);
        assertThat(optionCaptor.getValue().getName()).isEqualTo("블랙");
        assertThat(optionCaptor.getValue().getQuantity()).isEqualTo(10);
        assertThat(result).hasValueSatisfying(option -> assertThat(option.getId()).isEqualTo(10L));
    }

    @Test
    @DisplayName("옵션 생성은 상품이 없으면 옵션을 저장하지 않는다")
    void createOptionReturnsEmptyWhenProductDoesNotExist() {
        // given
        OptionCommand command = new OptionCommand("블랙", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.empty());

        // when
        Optional<Option> result = optionService.createOption(1L, command);

        // then
        assertThat(result).isEmpty();
        verify(optionRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션 생성은 같은 상품에 같은 이름이 있으면 옵션을 저장하지 않는다")
    void createOptionRejectsDuplicateNameForSameProduct() {
        // given
        Product product = product(1L);
        OptionCommand command = new OptionCommand("블랙", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.existsByProductIdAndName(1L, "블랙")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> optionService.createOption(1L, command))
            .isInstanceOf(CatalogException.class)
            .hasMessage("해당 상품에 이미 존재하는 옵션 이름입니다.");
        verify(optionRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션 생성은 이름 검증 오류가 있으면 옵션을 저장하지 않는다")
    void createOptionRejectsInvalidName() {
        // given
        OptionCommand command = new OptionCommand("", 10);

        // when & then
        assertThatThrownBy(() -> optionService.createOption(1L, command))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 이름은 필수입니다.");
        verify(optionRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션 예약은 옵션이 없으면 빈 결과를 반환한다")
    void reserveOptionReturnsEmptyWhenOptionDoesNotExist() {
        // given
        when(optionRepository.findByIdWithLock(10L)).thenReturn(Optional.empty());

        // when
        Optional<Option> result = optionService.reserveOption(10L, 2);

        // then
        assertThat(result).isEmpty();
        verify(optionRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션 예약은 옵션 재고를 차감하고 저장한다")
    void reserveOptionSubtractsQuantityAndSavesOption() {
        // given
        Option option = option(10L, product(1L), "블랙", 10);
        when(optionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(option));
        when(optionRepository.save(option)).thenReturn(option);

        // when
        Optional<Option> result = optionService.reserveOption(10L, 3);

        // then
        assertThat(result).hasValue(option);
        assertThat(option.getQuantity()).isEqualTo(7);
        verify(optionRepository).save(option);
    }

    @Test
    @DisplayName("옵션 예약은 차감 수량이 유효하지 않으면 옵션을 저장하지 않는다")
    void reserveOptionRejectsInvalidQuantity() {
        // given
        Option option = option(10L, product(1L), "블랙", 10);
        when(optionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(option));

        // when & then
        assertThatThrownBy(() -> optionService.reserveOption(10L, 0))
            .isInstanceOf(CatalogException.class)
            .hasMessage("옵션 차감 수량은 1 이상이어야 합니다.");
        assertThat(option.getQuantity()).isEqualTo(10);
        verify(optionRepository, never()).save(any());
    }

    @Test
    @DisplayName("옵션 삭제는 상품이 없으면 false를 반환한다")
    void deleteOptionReturnsFalseWhenProductDoesNotExist() {
        // given
        when(productService.findProduct(1L)).thenReturn(Optional.empty());

        // when
        boolean result = optionService.deleteOption(1L, 10L);

        // then
        assertThat(result).isFalse();
        verify(optionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("옵션 삭제는 상품의 옵션이 하나뿐이면 예외가 발생한다")
    void deleteOptionRejectsSingleOptionProduct() {
        // given
        Product product = product(1L);
        Option option = option(10L, product, "블랙", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(option));

        // when & then
        assertThatThrownBy(() -> optionService.deleteOption(1L, 10L))
            .isInstanceOf(CatalogException.class)
            .hasMessage("상품에는 최소 1개의 옵션이 필요합니다.");
        verify(optionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("옵션 삭제는 삭제할 옵션이 없으면 false를 반환한다")
    void deleteOptionReturnsFalseWhenOptionDoesNotExist() {
        // given
        Product product = product(1L);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(
            option(10L, product, "블랙", 10),
            option(11L, product, "화이트", 10)
        ));
        when(optionRepository.findById(99L)).thenReturn(Optional.empty());

        // when
        boolean result = optionService.deleteOption(1L, 99L);

        // then
        assertThat(result).isFalse();
        verify(optionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("옵션 삭제는 다른 상품의 옵션이면 false를 반환한다")
    void deleteOptionReturnsFalseWhenOptionBelongsToAnotherProduct() {
        // given
        Product product = product(1L);
        Product otherProduct = product(2L);
        Option otherProductOption = option(20L, otherProduct, "블랙", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(
            option(10L, product, "블랙", 10),
            option(11L, product, "화이트", 10)
        ));
        when(optionRepository.findById(20L)).thenReturn(Optional.of(otherProductOption));

        // when
        boolean result = optionService.deleteOption(1L, 20L);

        // then
        assertThat(result).isFalse();
        verify(optionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("옵션 삭제는 상품에 여러 옵션이 있으면 대상 옵션을 삭제한다")
    void deleteOptionDeletesOption() {
        // given
        Product product = product(1L);
        Option deleteTarget = option(10L, product, "블랙", 10);
        when(productService.findProduct(1L)).thenReturn(Optional.of(product));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(
            deleteTarget,
            option(11L, product, "화이트", 10)
        ));
        when(optionRepository.findById(10L)).thenReturn(Optional.of(deleteTarget));

        // when
        boolean result = optionService.deleteOption(1L, 10L);

        // then
        assertThat(result).isTrue();
        verify(optionRepository).delete(deleteTarget);
    }

    private Option option(Long id, Product product, String name, int quantity) {
        Option option = new Option(product, name, quantity);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private Product product(Long id) {
        Product product = new Product("상품" + id, 10_000, "https://example.com/product-" + id + ".jpg", category(1L));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Category category(Long id) {
        Category category = new Category("카테고리" + id, "#000000", "https://example.com/category-" + id + ".jpg", "설명");
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
