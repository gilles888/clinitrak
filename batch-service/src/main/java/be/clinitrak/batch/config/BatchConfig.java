package be.clinitrak.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration Spring Batch du batch-service.
 * Active le traitement batch et la planification des jobs.
 * Spring Batch crée automatiquement ses tables de métadonnées
 * via {@code spring.batch.jdbc.initialize-schema: always}.
 */
@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfig {}
