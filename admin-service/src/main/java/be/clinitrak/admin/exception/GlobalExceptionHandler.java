package be.clinitrak.admin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions REST pour le admin-service.
 *
 * <p>Retourne des réponses conformes à la RFC 7807 (Problem Details for HTTP APIs)
 * via {@link ProblemDetail}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI PROBLEM_BASE_URI = URI.create("https://clinitrak.be/problems/");

    /**
     * Gère les ressources Admin non trouvées.
     *
     * @param ex      exception AdminNotFoundException
     * @param request requête web courante
     * @return 404 Not Found avec ProblemDetail
     */
    @ExceptionHandler(AdminNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAdminNotFoundException(AdminNotFoundException ex, WebRequest request) {
        log.warn("Ressource Admin non trouvée : {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(PROBLEM_BASE_URI.resolve("admin-not-found"));
        problem.setTitle("Ressource introuvable");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Gère les violations de règles métier Admin.
     *
     * @param ex      exception AdminException
     * @param request requête web courante
     * @return 400 Bad Request avec ProblemDetail
     */
    @ExceptionHandler(AdminException.class)
    public ResponseEntity<ProblemDetail> handleAdminException(AdminException ex, WebRequest request) {
        log.warn("Erreur métier Admin : {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(PROBLEM_BASE_URI.resolve("admin-business-error"));
        problem.setTitle("Erreur métier");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Gère les accès refusés.
     *
     * @param ex      exception d'accès refusé
     * @param request requête web courante
     * @return 403 Forbidden avec ProblemDetail
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Accès refusé : {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Accès refusé");
        problem.setType(PROBLEM_BASE_URI.resolve("access-denied"));
        problem.setTitle("Accès refusé");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Gère les erreurs d'authentification Spring Security.
     *
     * @param ex      exception Spring Security
     * @param request requête web courante
     * @return 401 Unauthorized avec ProblemDetail
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleSpringAuthException(AuthenticationException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Non authentifié");
        problem.setType(PROBLEM_BASE_URI.resolve("unauthenticated"));
        problem.setTitle("Non authentifié");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Gère les erreurs de validation {@code @Valid}.
     *
     * @param ex      exception de validation
     * @param headers headers HTTP
     * @param status  statut HTTP
     * @param request requête web
     * @return 400 Bad Request avec détail des champs invalides
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valeur invalide",
                (a, b) -> a
            ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation échouée — voir 'fieldErrors' pour les détails"
        );
        problem.setType(PROBLEM_BASE_URI.resolve("validation-error"));
        problem.setTitle("Données invalides");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Fallback pour toute exception non gérée spécifiquement.
     *
     * @param ex      exception inattendue
     * @param request requête web courante
     * @return 500 Internal Server Error avec ProblemDetail minimal
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        log.error("Exception non gérée : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur interne est survenue. Contactez l'administrateur si le problème persiste."
        );
        problem.setType(PROBLEM_BASE_URI.resolve("internal-error"));
        problem.setTitle("Erreur interne");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
