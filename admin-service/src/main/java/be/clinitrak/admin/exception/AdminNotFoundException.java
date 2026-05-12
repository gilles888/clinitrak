package be.clinitrak.admin.exception;

import java.util.UUID;

/**
 * Exception levée lorsqu'une ressource Admin n'est pas trouvée.
 *
 * <p>Mappée sur un HTTP 404 Not Found par le {@link GlobalExceptionHandler}.
 */
public class AdminNotFoundException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif incluant l'identifiant.
     *
     * @param resourceType type de ressource non trouvée (ex: "Tenant")
     * @param id           identifiant de la ressource non trouvée
     */
    public AdminNotFoundException(String resourceType, UUID id) {
        super(resourceType + " introuvable : " + id);
    }

    /**
     * Crée une exception avec un message personnalisé.
     *
     * @param message message descriptif de l'erreur
     */
    public AdminNotFoundException(String message) {
        super(message);
    }
}
