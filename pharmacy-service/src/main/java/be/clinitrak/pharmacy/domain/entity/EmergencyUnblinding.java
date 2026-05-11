package be.clinitrak.pharmacy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Levée d'insu d'urgence pour un patient dans une étude en aveugle.
 *
 * <p>Le traitement réel ({@code treatment}) n'est révélé qu'après approbation
 * par le responsable pharmacien ({@code approvedBy}).
 * Le code de randomisation est déchiffré depuis l'{@link InvestigationalDrug}
 * associé à l'étude du patient.
 */
@Entity
@Table(name = "pharmacy_emergency_unblinding")
@Getter
@Setter
public class EmergencyUnblinding extends BaseEntity {

    /** Identifiant de l'étude clinique concernée. */
    @Column(nullable = false)
    private String studyId;

    /** Code anonymisé du patient (ex: "STLUC-001-001"). */
    @Column(nullable = false)
    private String patientCode;

    /** Date et heure de la demande de levée d'insu. */
    @Column(nullable = false)
    private LocalDateTime requestDate;

    /** Identifiant de la personne ayant fait la demande. */
    @Column(nullable = false)
    private String requestedBy;

    /** Raison médicale justifiant la levée d'insu. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    /** Traitement révélé après approbation (code de randomisation déchiffré). */
    @Column(columnDefinition = "TEXT")
    private String treatment;

    /** Identifiant du pharmacien ayant approuvé la levée d'insu. */
    private String approvedBy;

    /** Date et heure de l'approbation. */
    private LocalDateTime approvedAt;

    /** Identifiant du tenant propriétaire de cet enregistrement. */
    @Column(nullable = false)
    private String tenantId;
}
