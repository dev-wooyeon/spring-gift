package gift.order.infrastructure;

import gift.catalog.application.OptionService;
import gift.catalog.domain.Option;
import gift.order.application.OrderOptionPort;
import gift.order.application.ReservedOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CatalogOrderOptionAdapter implements OrderOptionPort {
    private final OptionService optionService;

    @Override
    public Optional<ReservedOption> reserveOption(Long optionId, int quantity) {
        return optionService.reserveOption(optionId, quantity)
            .map(this::toReservedOption);
    }

    private ReservedOption toReservedOption(Option option) {
        var product = option.getProduct();
        return new ReservedOption(
            option.getId(),
            option.getName(),
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }
}
