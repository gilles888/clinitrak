package be.clinitrak.document.domain.entity;

import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.domain.enums.ModuleSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

/**
 * Entité représentant un document stocké dans MinIO.
 * Supporte le versioning : chaque nouvelle version crée un nouvel enregistrement
 * lié au précédent via {@code parentDocumentId}.
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_study_id", columnList = "study_id"),
        @Index(name = "idx_documents_type", columnList = "document_type"),
        @Index(name = "idx_documents_parent", columnList = "parent_document_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    /**
     * Nom normalisé du fichier stocké dans MinIO (UUID + extension).
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Nom original du fichier tel que fourni par l'utilisateur lors de l'upload.
     */
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    /**
     * Type MIME du fichier (ex: application/pdf, image/jpeg).
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * Taille du fichier en octets.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Nom du bucket MinIO dans lequel le fichier est stocké.
     */
    @Column(name = "bucket_name", nullable = false, length = 100)
    @Builder.Default
    private String bucketName = "clinitrak-documents";

    /**
     * Chemin complet de l'objet dans MinIO (format : studyId/type/uuid-filename).
     */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /**
     * Type fonctionnel du document (PDF, XLSX, DOCX, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    /**
     * Module fonctionnel source du document.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "module_source", nullable = false, length = 50)
    private ModuleSource moduleSource;

    /**
     * Identifiant de l'étude clinique associée à ce document. Peut être null
     * pour des documents non liés à une étude spécifique.
     */
    @Column(name = "study_id")
    private UUID studyId;

    /**
     * Numéro de version du document. Commence à 1 pour le document initial,
     * incrémenté à chaque nouvelle version.
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    /**
     * Identifiant du document précédent dans la chaîne de versioning.
     * Null pour la première version d'un document.
     */
    @Column(name = "parent_document_id")
    private UUID parentDocumentId;

    /**
     * Email de l'utilisateur ayant uploadé ce document, extrait du JWT.
     */
    @Column(name = "uploaded_by")
    private String uploadedBy;

    /**
     * Description libre du document fournie lors de l'upload.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
