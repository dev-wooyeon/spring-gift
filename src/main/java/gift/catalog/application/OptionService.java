package gift.catalog.application;

import gift.catalog.domain.Option;
import gift.catalog.domain.Product;
import gift.catalog.exception.CatalogException;
import gift.catalog.infrastructure.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public Optional<List<Option>> getOptions(Long productId) {
        log.info("[OptionService] 상품의 옵션 목록 조회 - 상품 ID: {}", productId);
        if (productService.findProduct(productId).isEmpty()) {
            log.error("[OptionService] 옵션 목록 조회 실패 - 상품 존재하지 않음. 상품 ID: {}", productId);
            return Optional.empty();
        }

        return Optional.of(optionRepository.findByProductId(productId));
    }

    @Transactional
    public Optional<Option> createOption(Long productId, OptionCommand command) {
        log.info("[OptionService] 옵션 생성 요청 - 상품 ID: {}, 옵션명: {}, 수량: {}",
            productId, command.name(), command.quantity());
        validateName(command.name());

        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            log.error("[OptionService] 옵션 생성 실패 - 존재하지 않는 상품 ID: {}", productId);
            return Optional.empty();
        }

        if (optionRepository.existsByProductIdAndName(productId, command.name())) {
            log.error("[OptionService] 옵션 생성 실패 - 이미 존재하는 옵션명. 상품 ID: {}, 옵션명: {}",
                productId, command.name());
            throw CatalogException.invalid("해당 상품에 이미 존재하는 옵션 이름입니다.");
        }

        Option saved = optionRepository.save(new Option(product, command.name(), command.quantity()));
        log.info("[OptionService] 옵션 생성 성공 - 옵션 ID: {}", saved.getId());
        return Optional.of(saved);
    }

    @Transactional
    public Optional<Option> reserveOption(Long optionId, int quantity) {
        log.info("[OptionService] 옵션 재고 예약 차감 요청 - 옵션 ID: {}, 수량: {}", optionId, quantity);
        return optionRepository.findByIdWithLock(optionId)
            .map(option -> {
                option.subtractQuantity(quantity);
                Option saved = optionRepository.save(option);
                log.info("[OptionService] 옵션 재고 예약 차감 성공 - 옵션 ID: {}, 잔여 수량: {}", optionId, saved.getQuantity());
                return saved;
            });
    }

    @Transactional
    public boolean deleteOption(Long productId, Long optionId) {
        log.info("[OptionService] 옵션 삭제 요청 - 상품 ID: {}, 옵션 ID: {}", productId, optionId);
        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            log.error("[OptionService] 옵션 삭제 실패 - 존재하지 않는 상품 ID: {}", productId);
            return false;
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            log.error("[OptionService] 옵션 삭제 거부 - 상품에는 최소 1개의 옵션 필요. 상품 ID: {}", productId);
            throw CatalogException.invalid("상품에는 최소 1개의 옵션이 필요합니다.");
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            log.error("[OptionService] 옵션 삭제 실패 - 옵션이 존재하지 않거나 타 상품의 옵션임. 옵션 ID: {}", optionId);
            return false;
        }

        optionRepository.delete(option);
        log.info("[OptionService] 옵션 삭제 성공 - 옵션 ID: {}", optionId);
        return true;
    }

    private void validateName(String name) {
        List<String> errors = Option.validateName(name);
        if (!errors.isEmpty()) {
            log.error("[OptionService] 옵션 이름 검증 실패 - 옵션명: {}, 에러: {}", name, errors);
            throw CatalogException.invalid(String.join(", ", errors));
        }
    }
}
