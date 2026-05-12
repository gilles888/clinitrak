package be.clinitrak.document.exception;

import java.util.UUID;

/**
 * Exception levée lorsqu'un document est introuvable en base de données.
 */
public class DocumentNotFoundException extends RuntimeException {

    /**
     * Construit une exception avec l'identifiant du document non trouvé.
     *
     * @param id identifiant UUID du document introuvable
     */
    public DocumentNotFoundException(UUID id) {
        super("Document introuvable avec l'identifiant : " + id);
    }

    /**
     * Construit une exception avec un message personnalisé.
     *
     * @param message message d'erreur
     */
    public DocumentNotFoundException(String message) {
        super(message);
    }
}
