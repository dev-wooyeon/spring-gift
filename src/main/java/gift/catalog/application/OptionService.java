package gift.catalog.application;

import gift.catalog.domain.Option;
import gift.catalog.domain.Product;
import gift.catalog.exception.CatalogException;
import gift.catalog.infrastructure.OptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public Optional<List<Option>> getOptions(Long productId) {
        if (productService.findProduct(productId).isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(optionRepository.findByProductId(productId));
    }

    @Transactional
    public Optional<Option> createOption(Long productId, OptionCommand command) {
        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        if (optionRepository.existsByProductIdAndName(productId, command.name())) {
            throw CatalogException.invalid("해당 상품에 이미 존재하는 옵션 이름입니다.");
        }

        Option saved = optionRepository.save(new Option(product, command.name(), command.quantity()));
        return Optional.of(saved);
    }

    @Transactional
    public Optional<Option> reserveOption(Long optionId, int quantity) {
        return optionRepository.findByIdWithLock(optionId)
            .map(option -> {
                option.subtractQuantity(quantity);
                return optionRepository.save(option);
            });
    }

    @Transactional
    public boolean deleteOption(Long productId, Long optionId) {
        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            return false;
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw CatalogException.invalid("상품에는 최소 1개의 옵션이 필요합니다.");
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            return false;
        }

        optionRepository.delete(option);
        return true;
    }



}
