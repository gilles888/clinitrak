package be.clinitrak.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions du notification-service.
 * Retourne des réponses au format RFC 7807 (ProblemDetail).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String TYPE_BASE = "https://clinitrak.be/errors/";

    /**
     * Gère les erreurs de validation Bean Validation.
     *
     * @param ex exception de validation
     * @return ProblemDetail 400 avec le détail des champs invalides
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valeur invalide",
                        (a, b) -> a));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La requête contient des champs invalides");
        problem.setTitle("Erreur de validation");
        problem.setType(URI.create(TYPE_BASE + "validation-error"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Gère les erreurs de notification introuvable.
     *
     * @param ex exception notification non trouvée
     * @return ProblemDetail 404
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ProblemDetail handleNotificationNotFound(NotificationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Notification introuvable");
        problem.setType(URI.create(TYPE_BASE + "notification-not-found"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    /**
     * Gère toutes les exceptions non prévues.
     *
     * @param ex exception non gérée
     * @return ProblemDetail 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Erreur inattendue dans le notification-service : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
        problem.setTitle("Erreur interne");
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }
}
