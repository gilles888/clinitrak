package be.clinitrak.ethics.domain.entity;

import be.clinitrak.ethics.domain.enums.MeetingStatus;
import be.clinitrak.ethics.domain.enums.MeetingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Réunion du Comité d'Éthique.
 *
 * <p>Une réunion contient un ordre du jour ({@link MeetingAgendaItem}) et peut être
 * ordinaire (planifiée dans le calendrier annuel) ou extraordinaire (urgence).
 * Le PV (procès-verbal) est stocké sous forme de chemin ou URL dans {@code minutesDocument}.
 */
@Getter
@Setter
@Entity
@Table(
    name = "meetings",
    indexes = {
        @Index(name = "idx_mtg_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_mtg_meeting_date", columnList = "meeting_date"),
        @Index(name = "idx_mtg_status", columnList = "status")
    }
)
public class Meeting extends BaseEntity {

    /** Identifiant du tenant propriétaire de cette réunion. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Date de la réunion. */
    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    /** Heure de début de la réunion (ex: "14:00"). */
    @Column(name = "meeting_time", length = 10)
    private String meetingTime;

    /** Type de réunion (ordinaire ou extraordinaire). */
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", length = 30)
    private MeetingType meetingType = MeetingType.ORDINARY;

    /** Lieu de la réunion. */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * Ordre du jour structuré au format JSON (liste de strings).
     * Exemple : {@code ["Point 1 : Étude XYZ", "Point 2 : Divers"]}
     */
    @Column(name = "agenda", columnDefinition = "TEXT")
    private String agenda;

    /** Chemin ou URL vers le document de procès-verbal. */
    @Column(name = "minutes_document")
    private String minutesDocument;

    /**
     * Liste des présents au format JSON (liste de noms).
     * Exemple : {@code ["Dr. Dupont", "Dr. Martin"]}
     */
    @Column(name = "attendees", columnDefinition = "TEXT")
    private String attendees;

    /** Statut courant de la réunion. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private MeetingStatus status = MeetingStatus.PLANNED;

    /** Notes libres sur la réunion. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
