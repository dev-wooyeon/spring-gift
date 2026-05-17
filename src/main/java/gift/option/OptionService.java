package gift.option;

import gift.product.Product;
import gift.product.ProductService;
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
    public Optional<Option> createOption(Long productId, OptionRequest request) {
        validateName(request.name());

        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option saved = optionRepository.save(new Option(product, request.name(), request.quantity()));
        return Optional.of(saved);
    }

    @Transactional
    public boolean deleteOption(Long productId, Long optionId) {
        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            return false;
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            return false;
        }

        optionRepository.delete(option);
        return true;
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

}
