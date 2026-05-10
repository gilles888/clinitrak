package be.clinitrak.ctc.domain.entity;

import be.clinitrak.ctc.domain.enums.VisitStatus;
import be.clinitrak.ctc.domain.enums.VisitType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Représente une visite de monitoring réalisée dans le cadre d'une étude clinique.
 *
 * <p>Le CRA (Clinical Research Associate) effectue des visites périodiques
 * sur le site de l'étude. Les constats sont documentés et des délais de correction
 * peuvent être imposés pour les écarts identifiés.
 */
@Getter
@Setter
@Entity
@Table(name = "ctc_monitoring_visits")
public class MonitoringVisit extends BaseEntity {

    /** Identifiant UUID de l'étude dans le study-service (référence externe). */
    @Column(name = "study_id", nullable = false, length = 255)
    private String studyId;

    /** Date à laquelle la visite a été réalisée ou est planifiée. */
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    /** Type de visite (initiation, routine, clôture). */
    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false, length = 50)
    private VisitType visitType;

    /** Nom complet du CRA ayant effectué la visite. */
    @Column(name = "monitor_name", nullable = false, length = 255)
    private String monitorName;

    /** Constats de la visite documentés par le CRA. */
    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    /** Date limite pour corriger les écarts identifiés lors de la visite. */
    @Column(name = "correction_deadline")
    private LocalDate correctionDeadline;

    /** Statut courant de la visite (PLANNED par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VisitStatus status = VisitStatus.PLANNED;

    /** Rapport complet de la visite (rédigé après la visite). */
    @Column(name = "report", columnDefinition = "TEXT")
    private String report;

    /** Identifiant du tenant (UUID sous forme de String). */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
