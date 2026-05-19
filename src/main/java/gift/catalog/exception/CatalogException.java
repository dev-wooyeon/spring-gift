package gift.catalog.exception;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;

public class CatalogException extends DomainException {
    private CatalogException(ErrorType errorType, String message) {
        super(errorType, message);
    }

    public static CatalogException invalid(String message) {
        return new CatalogException(ErrorType.INVALID_REQUEST, message);
    }

    public static CatalogException notFound(String message) {
        return new CatalogException(ErrorType.NOT_FOUND, message);
    }
}
