package be.clinitrak.study.exception;

/**
 * Exception métier générique pour le study-service.
 *
 * <p>Levée pour les violations des règles métier (ex: numéro d'étude dupliqué,
 * transition de statut invalide, tenant non résolu).
 *
 * <p>Mappée sur un HTTP 400 Bad Request par le {@link GlobalExceptionHandler}.
 */
public class StudyException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif de la règle métier violée.
     *
     * @param message description de l'erreur métier
     */
    public StudyException(String message) {
        super(message);
    }

    /**
     * Crée une exception avec message et cause.
     *
     * @param message description de l'erreur métier
     * @param cause   exception d'origine
     */
    public StudyException(String message, Throwable cause) {
        super(message, cause);
    }
}
