package gift.order.application;

import java.util.Optional;

public interface OrderOptionPort {
    Optional<ReservedOption> reserveOption(Long optionId, int quantity);
}
