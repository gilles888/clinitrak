package be.clinitrak.batch.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Gestionnaire global des exceptions du batch-service.
 * Retourne des réponses au format RFC 7807 (ProblemDetail).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String TYPE_BASE = "https://clinitrak.be/errors/";

    /**
     * Gère les arguments invalides (nom de job inconnu, etc.).
     *
     * @param ex exception argument invalide
     * @return ProblemDetail 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Argument invalide");
        problem.setType(URI.create(TYPE_BASE + "invalid-argument"));
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
        log.error("Erreur inattendue dans le batch-service : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
        problem.setTitle("Erreur interne");
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }
}
