package gift.auth.exception;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;

/**
 * Exception thrown when a user is not authenticated.
 * Maps to HTTP Status 401 via GlobalApiExceptionHandler.
 *
 * @author brian.kim
 * @since 1.0
 */
public class UnauthenticatedException extends DomainException {
    public UnauthenticatedException(String message) {
        super(ErrorType.UNAUTHORIZED, message);
    }
}
