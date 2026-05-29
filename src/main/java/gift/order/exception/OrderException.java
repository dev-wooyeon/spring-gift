package gift.order.exception;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;

public class OrderException extends DomainException {
    private OrderException(ErrorType errorType, String message) {
        super(errorType, message);
    }

    public static OrderException invalid(String message) {
        return new OrderException(ErrorType.INVALID_REQUEST, message);
    }

    public static OrderException notFound(String message) {
        return new OrderException(ErrorType.NOT_FOUND, message);
    }
}
