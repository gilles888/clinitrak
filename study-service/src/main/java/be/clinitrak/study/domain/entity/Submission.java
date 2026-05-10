package be.clinitrak.study.domain.entity;

import be.clinitrak.study.domain.enums.SubmissionStatus;
import be.clinitrak.study.domain.enums.SubmissionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Soumission réglementaire associée à une étude clinique.
 *
 * <p>Trace toutes les soumissions aux autorités compétentes :
 * dossier initial, amendements, rapports annuels et finaux.
 */
@Getter
@Setter
@Entity
@Table(
    name = "submissions",
    indexes = {
        @Index(name = "idx_submission_study", columnList = "study_id"),
        @Index(name = "idx_submission_tenant", columnList = "tenant_id"),
        @Index(name = "idx_submission_status", columnList = "status")
    }
)
public class Submission extends BaseEntity {

    /**
     * Étude clinique concernée par cette soumission.
     * Chargé en mode LAZY pour éviter les jointures inutiles.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private ClinicalStudy study;

    /**
     * Identifiant du tenant pour l'isolation des données.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Type de soumission (initiale, amendement, rapport annuel, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 30)
    private SubmissionType submissionType;

    /**
     * Date effective d'envoi aux autorités.
     */
    @Column(name = "submission_date", nullable = false)
    private LocalDate submissionDate;

    /**
     * Date limite réglementaire pour cette soumission.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Statut actuel de la soumission.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    /**
     * Identifiant de la personne ayant soumis le dossier.
     */
    @Column(name = "submitted_by", length = 255)
    private String submittedBy;

    /**
     * Date de réception par les autorités (si connue).
     */
    @Column(name = "received_date")
    private LocalDate receivedDate;

    /**
     * Numéro de référence attribué par les autorités lors de la réception.
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    /**
     * Commentaires libres sur la soumission.
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
