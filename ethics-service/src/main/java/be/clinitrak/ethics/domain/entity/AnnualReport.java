package be.clinitrak.ethics.domain.entity;

import be.clinitrak.ethics.domain.enums.AnnualReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Rapport annuel attendu par le Comité d'Éthique pour une étude en cours.
 *
 * <p>Des rappels automatiques sont envoyés à J-60, J-30 et J-0 (date d'échéance dépassée).
 * Les flags {@code reminderSent60}, {@code reminderSent30} et {@code reminderSent0}
 * permettent de s'assurer qu'un seul rappel est envoyé par seuil.
 */
@Getter
@Setter
@Entity
@Table(
    name = "annual_reports",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ar_study_year_tenant",
        columnNames = {"study_id", "report_year", "tenant_id"}
    ),
    indexes = {
        @Index(name = "idx_ar_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_ar_study_id", columnList = "study_id"),
        @Index(name = "idx_ar_due_date", columnList = "due_date"),
        @Index(name = "idx_ar_status", columnList = "status")
    }
)
public class AnnualReport extends BaseEntity {

    /** Identifiant du tenant propriétaire de ce rapport. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Identifiant de l'étude concernée (référence externe vers study-service). */
    @Column(name = "study_id", nullable = false)
    private UUID studyId;

    /** Année de référence du rapport (ex: 2026). */
    @Column(name = "report_year", nullable = false)
    private int reportYear;

    /** Date d'échéance pour la soumission du rapport. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Date de réception effective du rapport (null si non reçu). */
    @Column(name = "received_date")
    private LocalDate receivedDate;

    /** Statut courant du rapport annuel. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private AnnualReportStatus status = AnnualReportStatus.PENDING;

    /** Indique si le rappel à 60 jours avant échéance a été envoyé. */
    @Column(name = "reminder_sent_60", nullable = false)
    private boolean reminderSent60 = false;

    /** Indique si le rappel à 30 jours avant échéance a été envoyé. */
    @Column(name = "reminder_sent_30", nullable = false)
    private boolean reminderSent30 = false;

    /** Indique si le rappel à échéance dépassée (J-0) a été envoyé. */
    @Column(name = "reminder_sent_0", nullable = false)
    private boolean reminderSent0 = false;

    /** Date du dernier rappel envoyé. */
    @Column(name = "last_reminder_date")
    private LocalDate lastReminderDate;

    /** Notes libres sur ce rapport annuel. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
