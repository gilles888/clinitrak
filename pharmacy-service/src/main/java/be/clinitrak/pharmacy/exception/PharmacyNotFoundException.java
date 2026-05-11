package be.clinitrak.pharmacy.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception levée lorsqu'une ressource pharmacie n'est pas trouvée dans le tenant courant.
 *
 * <p>Mappée sur un HTTP 404 Not Found par le {@link GlobalExceptionHandler}.
 */
public class PharmacyNotFoundException extends PharmacyException {

    /**
     * Crée une exception avec un message descriptif incluant l'identifiant.
     *
     * @param resourceType type de ressource non trouvée (ex: "Drug", "DrugStock")
     * @param id           identifiant de la ressource non trouvée
     */
    public PharmacyNotFoundException(String resourceType, UUID id) {
        super(resourceType + " introuvable : " + id, HttpStatus.NOT_FOUND);
    }

    /**
     * Crée une exception avec un message personnalisé.
     *
     * @param message message descriptif de l'erreur
     */
    public PharmacyNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
