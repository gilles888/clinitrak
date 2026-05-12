package be.clinitrak.exchange.exception;

import java.util.UUID;

/**
 * Exception levée lorsqu'une ressource Exchange n'est pas trouvée.
 *
 * <p>Mappée sur un HTTP 404 Not Found par le {@link GlobalExceptionHandler}.
 */
public class ExchangeNotFoundException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif incluant l'identifiant.
     *
     * @param resourceType type de ressource non trouvée (ex: "Demande", "Utilisateur externe")
     * @param id           identifiant de la ressource non trouvée
     */
    public ExchangeNotFoundException(String resourceType, UUID id) {
        super(resourceType + " introuvable : " + id);
    }

    /**
     * Crée une exception avec un message personnalisé.
     *
     * @param message message descriptif de l'erreur
     */
    public ExchangeNotFoundException(String message) {
        super(message);
    }
}
