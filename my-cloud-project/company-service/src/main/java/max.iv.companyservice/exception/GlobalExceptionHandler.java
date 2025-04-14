package max.iv.companyservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    public ErrorResponse handleNotFoundException(ChangeSetPersister.NotFoundException e, WebRequest request) {
        log.info("Entity not found: {}, Request details: {}", e, request);
        String userFriendlyMessage = "Resource not found";
        return new ErrorResponse( System.currentTimeMillis(),userFriendlyMessage);
    }
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException e, WebRequest request) {
        String validationErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}, Errors: {}, Request details: {}", e, validationErrors, request);
        return new ErrorResponse(System.currentTimeMillis(),"Validation error: " + validationErrors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e, WebRequest request) {
        e.getMostSpecificCause();
        String detailedError = e.getMostSpecificCause().getMessage();

        log.error("Failed to read HTTP message. Detailed error: {}, Request details: {}, Message: {}", detailedError, request, e.getMessage());
        String userFriendlyMessage = "Invalid input. Please check your request format or parameters.";
        return new ErrorResponse( System.currentTimeMillis(),userFriendlyMessage);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGlobalException(Exception e, WebRequest request) {
        log.error("Unexpected error: {},{},{}", e.getMessage(), e, request);
        return new ErrorResponse(System.currentTimeMillis(),"An unexpected error occurred") {

        };
    }
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ErrorResponse handlerHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, WebRequest request){
        log.error("HttpRequestMethodNotSupportedException: {},{}",e.getMessage(),request);
        return new ErrorResponse(System.currentTimeMillis(),"Invalid request method");

    }
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e , WebRequest request){
        log.error("Incorrect data is entered{}",request);
        return new ErrorResponse(System.currentTimeMillis(),"Incorrect data is entered");

    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ErrorResponse handleNoHandlerFoundException(NoHandlerFoundException e, WebRequest request) {
        String path = e.getRequestURL(); // Получаем запрошенный путь
        log.warn("No handler found for HTTP request {} {}", e.getHttpMethod(), path);
        return new ErrorResponse(System.currentTimeMillis(), "Endpoint not found: " + e.getHttpMethod() + " " + path);
    }

}
