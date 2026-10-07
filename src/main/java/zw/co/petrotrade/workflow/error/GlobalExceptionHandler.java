package zw.co.petrotrade.workflow.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import zw.co.petrotrade.workflow.transport.DateRequiredException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// Every error leaves here as {status, error, message}, with a message a person can act on.
// Internal details (stack traces, framework wording) go to the log, never to the screen.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, "That record doesn't exist. It may have been deleted.");
    }

    @ExceptionHandler({IllegalArgumentException.class, DateRequiredException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // role checks: our own messages explain what's missing; @PreAuthorize just says "Access Denied"
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        String message = ex instanceof AuthorizationDeniedException
                ? "You don't have permission to do that."
                : ex.getMessage();
        return build(HttpStatus.FORBIDDEN, message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Someone else changed this at the same time. Refresh and try again.");
    }

    // e.g. "Registration number is required"; also report filters bound from the address, e.g. ?status=SOMETHING
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> "typeMismatch".equals(error.getCode())
                        ? "\"" + error.getRejectedValue() + "\" isn't a valid " + inSentence(label(error.getField()))
                        : label(error.getField()) + " "
                        + ("NotBlank".equals(error.getCode()) || "NotNull".equals(error.getCode())
                        ? "is required"
                        : error.getDefaultMessage()))
                .distinct()
                .collect(Collectors.joining(". ", "", "."));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    // a value in the address that has the wrong type, e.g. /users/abc or ?status=SOMETHING
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "\"" + ex.getValue() + "\" isn't a valid " + inSentence(label(ex.getName())) + ".");
    }

    // "Driver ID" -> "driver ID", "ID" stays "ID"
    private static String inSentence(String label) {
        return label.length() > 1 && !Character.isUpperCase(label.charAt(1))
                ? Character.toLowerCase(label.charAt(0)) + label.substring(1)
                : label;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, label(ex.getParameterName()) + " is required.");
    }

    // malformed JSON, a number where text was expected, an unknown role/status, ...
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Some of the information sent was in the wrong format. Check the form and try again.");
    }

    // a database rule caught something the code didn't, e.g. a duplicate
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "This clashes with data that's already saved (for example a duplicate). Check the details and try again.");
    }

    // calls this API makes to Keycloak (creating logins, assigning roles)
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleKeycloak(RestClientException ex) {
        log.error("Keycloak admin call failed", ex);
        return build(HttpStatus.BAD_GATEWAY,
                "The sign-in server (Keycloak) didn't respond as expected, so nothing was changed. Try again, or ask IT to check the Keycloak setup.");
    }

    // Anything else: keep Spring's status (404 unknown address, 405 wrong method, ...) but never its wording
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        HttpStatusCode status = ex instanceof org.springframework.web.ErrorResponse spring
                ? spring.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;
        if (status.is5xxServerError()) {
            log.error("Unexpected error", ex);
        }
        String message = switch (status.value()) {
            case 404 -> "That page or record doesn't exist.";
            case 405 -> "That action isn't available here.";
            case 400, 415 -> "That request wasn't valid. Check the form and try again.";
            default -> "Something went wrong on our side. Please try again, and contact IT if it keeps happening.";
        };
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return build(resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }

    // registrationNumber -> "Registration number", id -> "ID"
    private static String label(String field) {
        if ("id".equalsIgnoreCase(field) || field.endsWith("Id")) {
            return field.length() <= 2 ? "ID" : label(field.substring(0, field.length() - 2)) + " ID";
        }
        String words = field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
