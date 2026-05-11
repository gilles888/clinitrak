package be.clinitrak.pharmacy.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique pour le pharmacy-service.
 *
 * <p>Levée pour les violations des règles métier (ex: stock insuffisant,
 * médicament non disponible, tenant non résolu).
 *
 * <p>Mappée sur le statut HTTP fourni par le {@link GlobalExceptionHandler}.
 */
public class PharmacyException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Crée une exception avec un message et un statut HTTP.
     *
     * @param message description de l'erreur métier
     * @param status  statut HTTP à retourner au client
     */
    public PharmacyException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Crée une exception avec message, cause et statut HTTP.
     *
     * @param message description de l'erreur métier
     * @param cause   exception d'origine
     * @param status  statut HTTP à retourner au client
     */
    public PharmacyException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Retourne le statut HTTP associé à cette exception.
     *
     * @return statut HTTP
     */
    public HttpStatus getStatus() {
        return status;
    }
}
