package be.clinitrak.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du document-service CliniTrak.
 * Service de gestion documentaire (GED) avec stockage MinIO.
 * Expose les endpoints d'upload, versioning et génération PDF.
 */
@SpringBootApplication
public class DocumentServiceApplication {

    /**
     * Démarre le document-service.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
