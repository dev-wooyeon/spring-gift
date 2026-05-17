package gift.wish.application;

import gift.wish.domain.Wish;
import gift.wish.infrastructure.WishRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class WishServiceTest {
    @Mock
    private WishRepository wishRepository;

    @Mock
    private WishProductPort productPort;

    @InjectMocks
    private WishService wishService;

    @Test
    @DisplayName("회원의 위시 목록을 상품 read model과 결합해 조회한다")
    void getWishesMapsProductReadModels() {
        // given
        Wish wish = wish(1L, 10L, 100L);
        WishProduct product = product(100L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(wishRepository.findByMemberId(10L, pageable)).thenReturn(new PageImpl<>(List.of(wish)));
        when(productPort.findProduct(100L)).thenReturn(Optional.of(product));

        // when
        Page<WishView> result = wishService.getWishes(10L, pageable);

        // then
        assertThat(result.getContent()).containsExactly(new WishView(
            1L,
            100L,
            "상품100",
            10_000,
            "https://example.com/product-100.jpg"
        ));
    }

    @Test
    @DisplayName("위시에 연결된 상품을 찾을 수 없으면 목록 조회에서 예외가 발생한다")
    void getWishesThrowsWhenProductReadModelIsMissing() {
        // given
        Wish wish = wish(1L, 10L, 100L);
        PageRequest pageable = PageRequest.of(0, 10);
        when(wishRepository.findByMemberId(10L, pageable)).thenReturn(new PageImpl<>(List.of(wish)));
        when(productPort.findProduct(100L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishService.getWishes(10L, pageable).getContent())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("위시에 연결된 상품을 찾을 수 없습니다. productId=100");
    }

    @Test
    @DisplayName("상품이 없으면 위시를 생성하지 않고 PRODUCT_MISSING을 반환한다")
    void addWishReturnsProductMissingWhenProductDoesNotExist() {
        // given
        WishCommand command = new WishCommand(100L);
        when(productPort.findProduct(100L)).thenReturn(Optional.empty());

        // when
        WishService.CreateResult result = wishService.addWish(10L, command);

        // then
        assertThat(result.status()).isEqualTo(WishService.CreateStatus.PRODUCT_MISSING);
        assertThat(result.wish()).isNull();
        verify(wishRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 위시면 새로 저장하지 않고 EXISTING을 반환한다")
    void addWishReturnsExistingWhenWishAlreadyExists() {
        // given
        WishProduct product = product(100L);
        Wish existing = wish(1L, 10L, 100L);
        WishCommand command = new WishCommand(100L);
        when(productPort.findProduct(100L)).thenReturn(Optional.of(product));
        when(wishRepository.findByMemberIdAndProductId(10L, 100L)).thenReturn(Optional.of(existing));

        // when
        WishService.CreateResult result = wishService.addWish(10L, command);

        // then
        assertThat(result.status()).isEqualTo(WishService.CreateStatus.EXISTING);
        assertThat(result.wish()).isEqualTo(WishView.of(1L, product));
        verify(wishRepository, never()).save(any());
    }

    @Test
    @DisplayName("상품이 있고 기존 위시가 없으면 위시를 저장하고 CREATED를 반환한다")
    void addWishCreatesWish() {
        // given
        WishProduct product = product(100L);
        WishCommand command = new WishCommand(100L);
        when(productPort.findProduct(100L)).thenReturn(Optional.of(product));
        when(wishRepository.findByMemberIdAndProductId(10L, 100L)).thenReturn(Optional.empty());
        when(wishRepository.save(any(Wish.class))).thenAnswer(invocation -> {
            Wish saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        // when
        WishService.CreateResult result = wishService.addWish(10L, command);

        // then
        ArgumentCaptor<Wish> wishCaptor = ArgumentCaptor.forClass(Wish.class);
        verify(wishRepository).save(wishCaptor.capture());
        assertThat(wishCaptor.getValue().getMemberId()).isEqualTo(10L);
        assertThat(wishCaptor.getValue().getProductId()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo(WishService.CreateStatus.CREATED);
        assertThat(result.wish()).isEqualTo(WishView.of(2L, product));
    }

    @Test
    @DisplayName("삭제할 위시가 없으면 WISH_MISSING을 반환한다")
    void removeWishReturnsNotFoundWhenWishDoesNotExist() {
        // given
        when(wishRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        WishService.DeleteResult result = wishService.removeWish(10L, 1L);

        // then
        assertThat(result.status()).isEqualTo(WishService.DeleteStatus.WISH_MISSING);
        verify(wishRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 회원의 위시는 삭제하지 않고 NOT_OWNER를 반환한다")
    void removeWishReturnsNotOwnerWhenMemberDoesNotOwnWish() {
        // given
        Wish wish = wish(1L, 20L, 100L);
        when(wishRepository.findById(1L)).thenReturn(Optional.of(wish));

        // when
        WishService.DeleteResult result = wishService.removeWish(10L, 1L);

        // then
        assertThat(result.status()).isEqualTo(WishService.DeleteStatus.NOT_OWNER);
        verify(wishRepository, never()).delete(any());
    }

    @Test
    @DisplayName("자신의 위시는 삭제하고 DELETED를 반환한다")
    void removeWishDeletesOwnWish() {
        // given
        Wish wish = wish(1L, 10L, 100L);
        when(wishRepository.findById(1L)).thenReturn(Optional.of(wish));

        // when
        WishService.DeleteResult result = wishService.removeWish(10L, 1L);

        // then
        assertThat(result.status()).isEqualTo(WishService.DeleteStatus.DELETED);
        verify(wishRepository).delete(wish);
    }

    private Wish wish(Long id, Long memberId, Long productId) {
        Wish wish = new Wish(memberId, productId);
        ReflectionTestUtils.setField(wish, "id", id);
        return wish;
    }

    private WishProduct product(Long productId) {
        return new WishProduct(
            productId,
            "상품" + productId,
            10_000,
            "https://example.com/product-" + productId + ".jpg"
        );
    }
}
