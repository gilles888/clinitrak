package be.clinitrak.ethics.exception;

import java.util.UUID;

/**
 * Exception levée lorsqu'une ressource CE n'est pas trouvée dans le tenant courant.
 *
 * <p>Mappée sur un HTTP 404 Not Found par le {@link GlobalExceptionHandler}.
 */
public class EthicsNotFoundException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif incluant l'identifiant.
     *
     * @param resourceType type de ressource non trouvée (ex: "Avis CE", "Réunion")
     * @param id           identifiant de la ressource non trouvée
     */
    public EthicsNotFoundException(String resourceType, UUID id) {
        super(resourceType + " introuvable : " + id);
    }

    /**
     * Crée une exception avec un message personnalisé.
     *
     * @param message message descriptif de l'erreur
     */
    public EthicsNotFoundException(String message) {
        super(message);
    }
}
