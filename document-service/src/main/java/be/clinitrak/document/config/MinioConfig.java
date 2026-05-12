package be.clinitrak.document.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client MinIO pour le stockage S3-compatible des documents.
 * Crée automatiquement le bucket par défaut s'il n'existe pas au démarrage.
 */
@Slf4j
@Configuration
public class MinioConfig {

    /** URL du serveur MinIO. */
    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    /** Clé d'accès (access key) MinIO. */
    @Value("${minio.accessKey:minioadmin}")
    private String accessKey;

    /** Clé secrète (secret key) MinIO. */
    @Value("${minio.secretKey:minioadmin}")
    private String secretKey;

    /** Nom du bucket par défaut pour les documents CliniTrak. */
    @Value("${minio.bucket:clinitrak-documents}")
    private String defaultBucket;

    /**
     * Crée et configure le client MinIO.
     * Vérifie l'existence du bucket par défaut et le crée si nécessaire.
     *
     * @return client MinIO configuré
     */
    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(defaultBucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
                log.info("Bucket MinIO '{}' créé avec succès", defaultBucket);
            } else {
                log.info("Bucket MinIO '{}' déjà existant", defaultBucket);
            }
        } catch (Exception ex) {
            log.warn("Impossible de vérifier/créer le bucket MinIO '{}' : {}. " +
                    "Le service démarrera quand même, mais les uploads échoueront.", defaultBucket, ex.getMessage());
        }

        return client;
    }
}
