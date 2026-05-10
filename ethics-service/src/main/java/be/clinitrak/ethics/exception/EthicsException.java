package be.clinitrak.ethics.exception;

/**
 * Exception métier générique pour le ethics-service.
 *
 * <p>Levée pour les violations des règles métier (ex: numéro CE dupliqué,
 * transition de décision invalide, tenant non résolu).
 *
 * <p>Mappée sur un HTTP 400 Bad Request par le {@link GlobalExceptionHandler}.
 */
public class EthicsException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif de la règle métier violée.
     *
     * @param message description de l'erreur métier
     */
    public EthicsException(String message) {
        super(message);
    }

    /**
     * Crée une exception avec message et cause.
     *
     * @param message description de l'erreur métier
     * @param cause   exception d'origine
     */
    public EthicsException(String message, Throwable cause) {
        super(message, cause);
    }
}
