package be.clinitrak.ethics.domain.entity;

import be.clinitrak.ethics.domain.enums.ReviewDecision;
import be.clinitrak.ethics.domain.enums.ReviewType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Avis éthique soumis au Comité d'Éthique pour une étude clinique.
 *
 * <p>Chaque avis est identifié par un numéro CE unique au format {@code AAAA/NNNN}
 * généré automatiquement par le {@link be.clinitrak.ethics.service.SequenceGeneratorService}.
 * La décision suit un cycle de vie PENDING → APPROVED | REJECTED | MORE_INFO_REQUESTED | WITHDRAWN.
 */
@Getter
@Setter
@Entity
@Table(
    name = "ethics_reviews",
    indexes = {
        @Index(name = "idx_er_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_er_study_id", columnList = "study_id"),
        @Index(name = "idx_er_decision", columnList = "decision"),
        @Index(name = "idx_er_submission_date", columnList = "submission_date")
    }
)
public class EthicsReview extends BaseEntity {

    /** Identifiant du tenant propriétaire de cet avis. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Identifiant de l'étude dans le study-service (référence externe). */
    @Column(name = "study_id", nullable = false)
    private UUID studyId;

    /**
     * Numéro CE unique par tenant, au format AAAA/NNNN (ex: 2026/0042).
     * Généré automatiquement lors de la création.
     */
    @Column(name = "ethics_number", length = 20, unique = true)
    private String ethicsNumber;

    /** Type d'avis soumis au Comité. */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 50)
    private ReviewType reviewType;

    /** Date de soumission de l'avis au secrétariat CE. */
    @Column(name = "submission_date", nullable = false)
    private LocalDate submissionDate;

    /** Date à laquelle le Comité a examiné l'avis (lors de la réunion). */
    @Column(name = "review_date")
    private LocalDate reviewDate;

    /** Décision rendue par le Comité d'Éthique. */
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 50)
    private ReviewDecision decision = ReviewDecision.PENDING;

    /** Date officielle de la décision. */
    @Column(name = "decision_date")
    private LocalDate decisionDate;

    /** Commentaires ou conditions associés à la décision. */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /** Date du prochain examen obligatoire (pour les études en cours). */
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    /** Nom du rapporteur désigné pour cet avis. */
    @Column(name = "rapporteur_name", length = 255)
    private String rapporteurName;

    /** Indique si un rappel a déjà été envoyé pour cet avis. */
    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent = false;
}
