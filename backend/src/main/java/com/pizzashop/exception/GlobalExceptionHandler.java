package com.pizzashop.exception;

import com.pizzashop.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return errorResponse(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    /** Malformed JSON or an unparseable value (e.g. an unknown enum constant) is a client error. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "The request body is malformed or contains an invalid value.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Invalid value for parameter '" + ex.getName() + "'.");
    }

    /** Someone else changed the same record first; the caller should reload and retry. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentModification(
            OptimisticLockingFailureException ex) {
        return errorResponse(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "This record was changed by someone else. Please reload and try again.");
    }

    /**
     * Only relevant when Spring itself serves the built Angular SPA (the Cloud Run image; the
     * Docker Compose / Oracle path never reaches this, nginx resolves such paths on its own).
     * Spring's static resource handler only serves paths that exist as an actual file, so a
     * direct load or refresh on a client-side route like "/admin/login" - not a real file, not
     * an API call - would otherwise surface as this exception. Handled ahead of the generic
     * {@link #handleUnexpectedException} below despite the declaration order: Spring picks the
     * most specific matching handler, not the first one declared.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleMissingResource(NoResourceFoundException ex) {
        if (ex.getResourcePath().startsWith("api/")) {
            return errorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such resource.");
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/index.html"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String errorCode, String message) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), errorCode, message);
        return ResponseEntity.status(status).body(body);
    }
}
