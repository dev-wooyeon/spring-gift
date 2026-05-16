package gift.option;

import gift.product.Product;
import gift.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public OptionService(OptionRepository optionRepository, ProductService productService) {
        this.optionRepository = optionRepository;
        this.productService = productService;
    }

    public Optional<List<OptionResponse>> getOptions(Long productId) {
        if (productService.findProduct(productId).isEmpty()) {
            return Optional.empty();
        }

        List<OptionResponse> options = optionRepository.findByProductId(productId).stream()
            .map(OptionResponse::from)
            .toList();
        return Optional.of(options);
    }

    @Transactional
    public Optional<OptionResponse> createOption(Long productId, OptionRequest request) {
        validateName(request.name());

        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            return Optional.empty();
        }

        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new IllegalArgumentException("이미 존재하는 옵션명입니다.");
        }

        Option saved = optionRepository.save(new Option(product, request.name(), request.quantity()));
        return Optional.of(OptionResponse.from(saved));
    }

    @Transactional
    public DeleteResult deleteOption(Long productId, Long optionId) {
        Product product = productService.findProduct(productId).orElse(null);
        if (product == null) {
            return DeleteResult.notFound();
        }

        List<Option> options = optionRepository.findByProductId(productId);
        if (options.size() <= 1) {
            throw new IllegalArgumentException("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }

        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            return DeleteResult.notFound();
        }

        optionRepository.delete(option);
        return DeleteResult.noContent();
    }

    private void validateName(String name) {
        List<String> errors = OptionNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public enum DeleteStatus {
        NO_CONTENT,
        NOT_FOUND
    }

    public record DeleteResult(DeleteStatus status) {
        static DeleteResult noContent() {
            return new DeleteResult(DeleteStatus.NO_CONTENT);
        }

        static DeleteResult notFound() {
            return new DeleteResult(DeleteStatus.NOT_FOUND);
        }
    }
}
