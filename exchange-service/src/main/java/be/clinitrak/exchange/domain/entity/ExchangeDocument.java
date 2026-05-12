package be.clinitrak.exchange.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Document attaché à une demande d'échange.
 *
 * <p>Stocke les métadonnées du fichier (nom, chemin, taille, type MIME).
 * Le fichier physique est géré par le document-service (MinIO).
 */
@Getter
@Setter
@Entity
@Table(name = "exchange_documents")
public class ExchangeDocument extends BaseEntity {

    /** Demande d'échange à laquelle ce document est rattaché. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private ExchangeRequest request;

    /** Nom original du fichier tel qu'envoyé par l'utilisateur. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Chemin de stockage du fichier dans MinIO. */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** Taille du fichier en octets. */
    @Column(name = "file_size")
    private Long fileSize;

    /** Type MIME du fichier (ex: application/pdf, image/png). */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** Date et heure de téléversement du document. */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /** Identifiant du tenant auquel appartient ce document. */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
