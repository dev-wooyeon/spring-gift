package gift.support.presentation;

import gift.support.exception.DomainException;
import gift.support.exception.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalApiExceptionHandler {
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<String> handleDomainException(DomainException exception) {
        return ResponseEntity.status(toHttpStatus(exception.getErrorType()))
            .body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(validationMessage(exception));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingRequestHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.badRequest()
            .body("필수 요청 헤더가 없습니다. name=" + exception.getHeaderName());
    }

    private HttpStatus toHttpStatus(ErrorType errorType) {
        return switch (errorType) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String validationMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return "요청 값이 올바르지 않습니다.";
        }
        return fieldError.getDefaultMessage();
    }
}
