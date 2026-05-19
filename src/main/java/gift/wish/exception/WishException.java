package gift.wish.exception;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;

public class WishException extends DomainException {
    private WishException(ErrorType errorType, String message) {
        super(errorType, message);
    }

    public static WishException internal(String message) {
        return new WishException(ErrorType.INTERNAL_ERROR, message);
    }
}
