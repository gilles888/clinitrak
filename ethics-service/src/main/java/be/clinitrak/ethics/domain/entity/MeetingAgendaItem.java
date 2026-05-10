package be.clinitrak.ethics.domain.entity;

import be.clinitrak.ethics.domain.enums.ReviewDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Point de l'ordre du jour d'une réunion du Comité d'Éthique.
 *
 * <p>Chaque item peut être associé à une étude (via {@code studyId}) ou concerner
 * un point administratif (type "DIVERS"). La décision prise lors de la réunion
 * est enregistrée dans le champ {@code decision}.
 */
@Getter
@Setter
@Entity
@Table(
    name = "meeting_agenda_items",
    indexes = {
        @Index(name = "idx_mai_meeting_id", columnList = "meeting_id"),
        @Index(name = "idx_mai_tenant_id", columnList = "tenant_id")
    }
)
public class MeetingAgendaItem extends BaseEntity {

    /** Identifiant du tenant propriétaire de cet item. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Réunion à laquelle appartient cet item de l'ordre du jour. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mai_meeting"))
    private Meeting meeting;

    /**
     * Identifiant de l'étude concernée (référence externe vers study-service).
     * Peut être null pour les points non liés à une étude (ex: "DIVERS").
     */
    @Column(name = "study_id")
    private UUID studyId;

    /** Ordre d'affichage dans l'ordre du jour. */
    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    /** Type de point à l'ordre du jour (ex: "ETUDE", "DIVERS"). */
    @Column(name = "item_type", length = 50)
    private String itemType;

    /** Durée estimée ou effective du point en minutes. */
    @Column(name = "duration_minutes")
    private int durationMinutes;

    /** Décision prise lors de la réunion pour ce point (nullable avant la réunion). */
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 50)
    private ReviewDecision decision;

    /** Commentaires ou résumé des discussions sur ce point. */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
