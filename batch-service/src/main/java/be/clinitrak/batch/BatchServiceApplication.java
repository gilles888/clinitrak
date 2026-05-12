package be.clinitrak.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du batch-service CliniTrak.
 * Service de traitements batch planifiés via Spring Batch.
 * Orchestre les rappels nocturnes, rapports hebdomadaires et facturation mensuelle.
 */
@SpringBootApplication
public class BatchServiceApplication {

    /**
     * Démarre le batch-service.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(BatchServiceApplication.class, args);
    }
}
