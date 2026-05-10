package be.clinitrak.ctc.domain.entity;

import be.clinitrak.ctc.domain.enums.DeskType;
import be.clinitrak.ctc.domain.enums.Priority;
import be.clinitrak.ctc.domain.enums.RequestStatus;
import be.clinitrak.ctc.domain.enums.RequestType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Représente une demande soumise au desk du Centre de Thérapie Cellulaire.
 *
 * <p>Une demande est initiée par un chercheur ou coordinateur pour une étude clinique
 * référencée dans le study-service. Elle est assignée à un responsable CTC
 * qui la traite selon sa priorité et son type.
 */
@Getter
@Setter
@Entity
@Table(name = "ctc_desk_requests")
public class TrialDeskRequest extends BaseEntity {

    /** Identifiant UUID de l'étude dans le study-service (référence externe). */
    @Column(name = "study_id", nullable = false, length = 255)
    private String studyId;

    /** Type de desk (académique ou commercial). */
    @Enumerated(EnumType.STRING)
    @Column(name = "desk_type", nullable = false, length = 50)
    private DeskType deskType;

    /** Date à laquelle la demande a été soumise. */
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /** Nom complet du demandeur. */
    @Column(name = "requestor_name", nullable = false, length = 255)
    private String requestorName;

    /** Email de contact du demandeur. */
    @Column(name = "requestor_email", length = 255)
    private String requestorEmail;

    /** Organisation ou service du demandeur. */
    @Column(name = "requestor_organization", length = 255)
    private String requestorOrganization;

    /** Type de demande (nouvelle étude, amendement, extension, clôture). */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private RequestType requestType;

    /** Statut courant de la demande (PENDING par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RequestStatus status = RequestStatus.PENDING;

    /** UUID de l'utilisateur assigné au traitement de cette demande. */
    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    /** Priorité de traitement (MEDIUM par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    private Priority priority = Priority.MEDIUM;

    /** Date limite de traitement de la demande. */
    @Column(name = "deadline")
    private LocalDate deadline;

    /** Notes ou commentaires libres associés à la demande. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Identifiant du tenant (UUID sous forme de String). */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
