package be.clinitrak.ctc.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique pour le ctc-service.
 *
 * <p>Levée pour les violations des règles métier (ex: transition de statut invalide,
 * tenant non résolu, contrainte d'unicité violée).
 *
 * <p>Mappée sur le statut HTTP fourni (par défaut 400 Bad Request) par le {@link GlobalExceptionHandler}.
 */
public class CtcException extends RuntimeException {

    private final HttpStatus httpStatus;

    /**
     * Crée une exception avec un message descriptif (statut 400 par défaut).
     *
     * @param message description de l'erreur métier
     */
    public CtcException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    /**
     * Crée une exception avec un message et un statut HTTP spécifique.
     *
     * @param message    description de l'erreur métier
     * @param httpStatus statut HTTP à retourner
     */
    public CtcException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Crée une exception avec message, cause et statut HTTP.
     *
     * @param message    description de l'erreur métier
     * @param cause      exception d'origine
     * @param httpStatus statut HTTP à retourner
     */
    public CtcException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * Retourne le statut HTTP associé à cette exception.
     *
     * @return statut HTTP
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
