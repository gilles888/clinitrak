package be.clinitrak.ctc.domain.entity;

import be.clinitrak.ctc.domain.enums.AnalysisType;
import be.clinitrak.ctc.domain.enums.DataFormat;
import be.clinitrak.ctc.domain.enums.StatisticsStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Représente une demande d'analyse statistique pour une étude clinique.
 *
 * <p>Les chercheurs soumettent des demandes d'analyse au service de biostatistique
 * du CTC. Chaque demande spécifie le type d'analyse, le format de sortie
 * souhaité et la date limite de livraison.
 */
@Getter
@Setter
@Entity
@Table(name = "ctc_statistics_requests")
public class StatisticsRequest extends BaseEntity {

    /** Identifiant UUID de l'étude dans le study-service (référence externe). */
    @Column(name = "study_id", nullable = false, length = 255)
    private String studyId;

    /** Date à laquelle la demande a été soumise. */
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /** Nom complet du chercheur demandeur. */
    @Column(name = "requestor_name", nullable = false, length = 255)
    private String requestorName;

    /** Date limite à laquelle les résultats doivent être livrés. */
    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    /** Type d'analyse statistique demandée. */
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 50)
    private AnalysisType analysisType;

    /** Format de sortie souhaité pour les résultats. */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_format", nullable = false, length = 50)
    private DataFormat dataFormat;

    /** Statut courant de la demande (PENDING par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatisticsStatus status = StatisticsStatus.PENDING;

    /** Date effective de livraison des résultats (renseignée à la livraison). */
    @Column(name = "delivered_date")
    private LocalDate deliveredDate;

    /** Identifiant du tenant (UUID sous forme de String). */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
