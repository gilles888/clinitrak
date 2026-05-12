package be.clinitrak.exchange.exception;

/**
 * Exception métier générique pour le exchange-service.
 *
 * <p>Levée pour les violations des règles métier (ex: email déjà utilisé,
 * transition de statut invalide, email non vérifié).
 *
 * <p>Mappée sur un HTTP 400 Bad Request par le {@link GlobalExceptionHandler}.
 */
public class ExchangeException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif de la règle métier violée.
     *
     * @param message description de l'erreur métier
     */
    public ExchangeException(String message) {
        super(message);
    }

    /**
     * Crée une exception avec message et cause.
     *
     * @param message description de l'erreur métier
     * @param cause   exception d'origine
     */
    public ExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
