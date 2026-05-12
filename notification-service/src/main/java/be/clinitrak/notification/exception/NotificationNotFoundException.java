package be.clinitrak.notification.exception;

import java.util.UUID;

/**
 * Exception levée lorsqu'une notification est introuvable en base de données.
 */
public class NotificationNotFoundException extends RuntimeException {

    /**
     * Construit une exception avec l'identifiant de la notification non trouvée.
     *
     * @param id identifiant UUID de la notification introuvable
     */
    public NotificationNotFoundException(UUID id) {
        super("Notification introuvable avec l'identifiant : " + id);
    }
}
