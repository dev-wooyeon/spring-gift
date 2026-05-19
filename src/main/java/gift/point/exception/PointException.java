package gift.point.exception;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;

public class PointException extends DomainException {
    private PointException(ErrorType errorType, String message) {
        super(errorType, message);
    }

    public static PointException invalid(String message) {
        return new PointException(ErrorType.INVALID_REQUEST, message);
    }
}
