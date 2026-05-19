package gift.support.exception;

public abstract class DomainException extends RuntimeException {
    private final ErrorType errorType;

    protected DomainException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
