package be.clinitrak.study.domain.entity;

import be.clinitrak.study.domain.enums.PatientStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Patient pseudonymisé participant à une étude clinique.
 *
 * <p>Conformément au RGPD, aucune donnée directement identifiante n'est stockée.
 * L'identité réelle du patient est gérée dans le système hospitalier (DPI),
 * seul le code pseudonyme ({@code patientCode}) est conservé ici.
 */
@Getter
@Setter
@Entity
@Table(
    name = "study_patients",
    indexes = {
        @Index(name = "idx_patient_study", columnList = "study_id"),
        @Index(name = "idx_patient_tenant", columnList = "tenant_id"),
        @Index(name = "idx_patient_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_patient_code_study",
            columnNames = {"patient_code", "study_id"}
        )
    }
)
public class Patient extends BaseEntity {

    /**
     * Étude clinique dans laquelle ce patient est inscrit.
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
     * Code pseudonyme RGPD du patient, unique au sein de l'étude.
     * Ex: "PT-001", "SUBJ-2026-042".
     * Ne doit contenir aucune donnée identifiante.
     */
    @Column(name = "patient_code", nullable = false, length = 50)
    private String patientCode;

    /**
     * Date d'inclusion (ou de randomisation) dans l'étude.
     */
    @Column(name = "inclusion_date")
    private LocalDate inclusionDate;

    /**
     * Date de sortie de l'étude (retrait, fin de traitement).
     */
    @Column(name = "exclusion_date")
    private LocalDate exclusionDate;

    /**
     * Statut actuel du patient dans l'étude.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PatientStatus status = PatientStatus.SCREENED;

    /**
     * Code du centre participant (site multicentriques).
     * Ex: "CUSL", "AZ-GHE", "CHU-LGE".
     */
    @Column(name = "site_code", length = 50)
    private String siteCode;

    /**
     * Notes non identifiantes sur le suivi du patient dans l'étude.
     * Strictement limité aux informations non-PII (pas de nom, prénom, date de naissance).
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
