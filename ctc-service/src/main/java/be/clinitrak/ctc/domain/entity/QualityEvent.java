package be.clinitrak.ctc.domain.entity;

import be.clinitrak.ctc.domain.enums.EventStatus;
import be.clinitrak.ctc.domain.enums.EventType;
import be.clinitrak.ctc.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Représente un événement qualité survenu dans le cadre d'une étude clinique.
 *
 * <p>Inclut les déviations protocolaires, les événements indésirables graves (EIG),
 * les actions correctives et préventives (CAPA), et les audits.
 * Chaque événement suit un cycle de vie : OPEN → IN_PROGRESS → CLOSED.
 */
@Getter
@Setter
@Entity
@Table(name = "ctc_quality_events")
public class QualityEvent extends BaseEntity {

    /** Identifiant UUID de l'étude dans le study-service (référence externe). */
    @Column(name = "study_id", nullable = false, length = 255)
    private String studyId;

    /** Type d'événement qualité (déviation, EIG, CAPA, audit). */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    /** Date à laquelle l'événement s'est produit ou a été détecté. */
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /** Sévérité de l'événement (LOW, MEDIUM, HIGH, CRITICAL). */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private Severity severity;

    /** Description détaillée de l'événement. */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Analyse de la cause racine de l'événement. */
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    /** Action corrective mise en place pour éviter la récurrence. */
    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    /** Statut courant de l'événement (OPEN par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EventStatus status = EventStatus.OPEN;

    /** Date à laquelle l'événement a été clôturé (renseignée à la clôture). */
    @Column(name = "closure_date")
    private LocalDate closureDate;

    /** Identifiant du tenant (UUID sous forme de String). */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
