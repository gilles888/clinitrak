package be.clinitrak.document.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions du document-service.
 * Retourne des réponses au format RFC 7807 (ProblemDetail).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String TYPE_BASE = "https://clinitrak.be/errors/";

    /**
     * Gère les erreurs de validation Bean Validation sur les DTOs.
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
     * Gère les erreurs de document introuvable.
     *
     * @param ex exception document non trouvé
     * @return ProblemDetail 404
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocumentNotFound(DocumentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Document introuvable");
        problem.setType(URI.create(TYPE_BASE + "document-not-found"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    /**
     * Gère les erreurs de stockage MinIO.
     *
     * @param ex exception de stockage
     * @return ProblemDetail 502
     */
    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageException(StorageException ex) {
        log.error("Erreur de stockage MinIO : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Erreur lors de l'accès au stockage de documents");
        problem.setTitle("Erreur de stockage");
        problem.setType(URI.create(TYPE_BASE + "storage-error"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }

    /**
     * Gère le dépassement de taille de fichier lors de l'upload.
     *
     * @param ex exception de taille dépassée
     * @return ProblemDetail 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, "La taille du fichier dépasse la limite autorisée de 50 MB");
        problem.setTitle("Fichier trop volumineux");
        problem.setType(URI.create(TYPE_BASE + "file-too-large"));
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
        log.error("Erreur inattendue dans le document-service : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
        problem.setTitle("Erreur interne");
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        return problem;
    }
}
