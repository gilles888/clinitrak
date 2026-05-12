package be.clinitrak.document.exception;

/**
 * Exception levée lors d'une erreur de stockage MinIO (upload, download, delete).
 */
public class StorageException extends RuntimeException {

    /**
     * Construit une exception de stockage avec un message descriptif.
     *
     * @param message description de l'erreur de stockage
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Construit une exception de stockage avec un message et la cause originale.
     *
     * @param message description de l'erreur de stockage
     * @param cause   exception originale levée par le client MinIO
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
