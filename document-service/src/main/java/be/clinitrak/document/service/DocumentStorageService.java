package be.clinitrak.document.service;

import be.clinitrak.document.exception.StorageException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Service de stockage de fichiers dans MinIO.
 * Encapsule les opérations d'upload, de génération d'URLs présignées et de suppression.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final MinioClient minioClient;

    /** Nom du bucket MinIO par défaut. */
    @Value("${minio.bucket:clinitrak-documents}")
    private String defaultBucket;

    /** Durée de validité des URLs présignées en minutes. */
    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 15;

    /**
     * Upload un fichier multipart vers MinIO.
     *
     * @param file      fichier à uploader
     * @param objectKey chemin de stockage dans MinIO (ex: studyId/PDF/uuid-filename.pdf)
     * @throws StorageException si l'upload échoue
     */
    public void uploadFile(MultipartFile file, String objectKey) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Fichier uploadé dans MinIO : bucket={}, key={}, size={} bytes",
                    defaultBucket, objectKey, file.getSize());
        } catch (Exception ex) {
            throw new StorageException("Erreur lors de l'upload du fichier vers MinIO : " + objectKey, ex);
        }
    }

    /**
     * Upload un tableau d'octets (PDF généré) vers MinIO.
     *
     * @param content     contenu binaire à stocker
     * @param objectKey   chemin de stockage dans MinIO
     * @param contentType type MIME du contenu
     * @throws StorageException si l'upload échoue
     */
    public void uploadBytes(byte[] content, String objectKey, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectKey)
                    .stream(inputStream, content.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("Contenu binaire uploadé dans MinIO : bucket={}, key={}, size={} bytes",
                    defaultBucket, objectKey, content.length);
        } catch (Exception ex) {
            throw new StorageException("Erreur lors de l'upload du contenu binaire vers MinIO : " + objectKey, ex);
        }
    }

    /**
     * Génère une URL présignée pour le téléchargement d'un objet MinIO.
     * L'URL est valable {@value #PRESIGNED_URL_EXPIRY_MINUTES} minutes.
     *
     * @param objectKey chemin de l'objet dans MinIO
     * @return URL présignée temporaire de téléchargement
     * @throws StorageException si la génération de l'URL échoue
     */
    public String generatePresignedDownloadUrl(String objectKey) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(PRESIGNED_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .build());
            log.debug("URL présignée générée pour : {}", objectKey);
            return url;
        } catch (Exception ex) {
            throw new StorageException("Erreur lors de la génération de l'URL présignée pour : " + objectKey, ex);
        }
    }

    /**
     * Supprime un objet du bucket MinIO.
     *
     * @param objectKey chemin de l'objet à supprimer
     * @throws StorageException si la suppression échoue
     */
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectKey)
                    .build());
            log.info("Objet supprimé de MinIO : bucket={}, key={}", defaultBucket, objectKey);
        } catch (Exception ex) {
            throw new StorageException("Erreur lors de la suppression de l'objet MinIO : " + objectKey, ex);
        }
    }
}
