package be.clinitrak.ethics.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Correspondance générée par le Comité d'Éthique.
 *
 * <p>Représente une lettre ou un email généré depuis un template Thymeleaf,
 * avec optionnellement un PDF associé. Le champ {@code sent} indique si
 * la correspondance a été envoyée au destinataire.
 */
@Getter
@Setter
@Entity
@Table(
    name = "correspondence",
    indexes = {
        @Index(name = "idx_corr_study_id", columnList = "study_id"),
        @Index(name = "idx_corr_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_corr_sent", columnList = "sent")
    }
)
public class Correspondence extends BaseEntity {

    /** Identifiant du tenant propriétaire de cette correspondance. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Identifiant de l'étude concernée. */
    @Column(name = "study_id", nullable = false)
    private UUID studyId;

    /** Identifiant de l'avis CE associé (nullable si correspondance générale). */
    @Column(name = "review_id")
    private UUID reviewId;

    /** Identifiant du template utilisé pour générer cette correspondance. */
    @Column(name = "template_id")
    private UUID templateId;

    /** Date de génération du document. */
    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    /** Date d'envoi effectif (null si non encore envoyé). */
    @Column(name = "sent_date")
    private LocalDate sentDate;

    /** Email du destinataire. */
    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    /** Nom complet du destinataire. */
    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    /** Sujet de l'email ou de la lettre. */
    @Column(name = "subject", length = 255)
    private String subject;

    /** Contenu HTML généré depuis le template. */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Chemin vers le fichier PDF généré (null si pas de PDF). */
    @Column(name = "pdf_path")
    private String pdfPath;

    /** Indique si la correspondance a été envoyée au destinataire. */
    @Column(name = "sent", nullable = false)
    private boolean sent = false;

    /** Message d'erreur en cas d'échec d'envoi (null si envoi réussi). */
    @Column(name = "send_error")
    private String sendError;
}
