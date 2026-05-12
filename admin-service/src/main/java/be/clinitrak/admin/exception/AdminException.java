package be.clinitrak.admin.exception;

/**
 * Exception métier générique pour le admin-service.
 *
 * <p>Levée pour les violations des règles métier (ex: slug dupliqué,
 * opération interdite sur un tenant suspendu).
 *
 * <p>Mappée sur un HTTP 400 Bad Request par le {@link GlobalExceptionHandler}.
 */
public class AdminException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif de la règle métier violée.
     *
     * @param message description de l'erreur métier
     */
    public AdminException(String message) {
        super(message);
    }

    /**
     * Crée une exception avec message et cause.
     *
     * @param message description de l'erreur métier
     * @param cause   exception d'origine
     */
    public AdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
