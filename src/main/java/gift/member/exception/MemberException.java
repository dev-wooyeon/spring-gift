package gift.member.exception;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;

public class MemberException extends DomainException {
    private MemberException(ErrorType errorType, String message) {
        super(errorType, message);
    }

    public static MemberException invalid(String message) {
        return new MemberException(ErrorType.INVALID_REQUEST, message);
    }

    public static MemberException notFound(String message) {
        return new MemberException(ErrorType.NOT_FOUND, message);
    }
}
