package be.clinitrak.study.domain.entity;

import be.clinitrak.study.domain.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité représentant une étude clinique dans CliniTrak.
 *
 * <p>Une étude clinique regroupe toutes les informations d'un protocole de recherche :
 * identifications réglementaires, promoteur, investigateur, statut et données de suivi.
 *
 * <p>Le champ {@code studyNumber} est généré automatiquement au format {@code ST-{YYYY}-{seq5}}
 * s'il n'est pas fourni lors de la création.
 */
@Getter
@Setter
@Entity
@Table(
    name = "clinical_studies",
    indexes = {
        @Index(name = "idx_study_tenant", columnList = "tenant_id"),
        @Index(name = "idx_study_number", columnList = "study_number"),
        @Index(name = "idx_study_ethics", columnList = "ethics_number"),
        @Index(name = "idx_study_eudract", columnList = "eudract_number"),
        @Index(name = "idx_study_status", columnList = "current_status")
    }
)
public class ClinicalStudy extends BaseEntity {

    /**
     * Identifiant du tenant propriétaire de cette étude.
     * Assure l'isolation des données entre institutions.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Numéro interne unique de l'étude au format {@code ST-{YYYY}-{seq5}}.
     * Généré automatiquement si absent à la création.
     */
    @Column(name = "study_number", length = 30)
    private String studyNumber;

    /**
     * Numéro d'approbation du Comité d'Éthique (CE).
     * Format variable selon l'institution.
     */
    @Column(name = "ethics_number", length = 100)
    private String ethicsNumber;

    /**
     * Numéro EudraCT (European Union Drug Regulating Authorities Clinical Trials).
     * Format : YYYY-NNNNNN-CC
     */
    @Column(name = "eudract_number", length = 20)
    private String eudractNumber;

    /**
     * Numéro CTIS (Clinical Trials Information System) de l'Union Européenne.
     * Remplace progressivement le numéro EudraCT sous le Règlement UE 536/2014.
     */
    @Column(name = "ctis_number", length = 30)
    private String ctisNumber;

    /**
     * Titre complet de l'étude clinique.
     * Correspond au titre officiel du protocole.
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Acronyme ou titre court de l'étude (ex: "CAPRICORN", "SOLID-AF").
     */
    @Column(name = "acronym", length = 50)
    private String acronym;

    /**
     * Type d'étude : interventionnelle, observationnelle ou accès élargi.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "study_type", nullable = false, length = 30)
    private StudyType studyType;

    /**
     * Type de promoteur : académique, commercial ou institutionnel.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sponsor_type", nullable = false, length = 30)
    private SponsorType sponsorType;

    /**
     * Nom du promoteur de l'étude.
     */
    @Column(name = "sponsor", length = 255)
    private String sponsor;

    /**
     * Nom de l'investigateur principal (PI).
     */
    @Column(name = "principal_investigator", length = 255)
    private String principalInvestigator;

    /**
     * Domaine thérapeutique (ex: "Oncologie", "Cardiologie", "Neurologie").
     */
    @Column(name = "therapeutic_area", length = 100)
    private String therapeuticArea;

    /**
     * Phase de l'essai clinique (I à IV, ou N/A pour les études observationnelles).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 20)
    private StudyPhase phase;

    /**
     * Statut réglementaire courant de l'étude.
     * Chaque changement de statut est tracé dans {@code StudyStatusHistory}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private StudyStatus currentStatus = StudyStatus.DRAFT;

    /**
     * Date de début prévue ou effective de l'étude.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Date de fin prévue ou effective de l'étude.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Date d'approbation par les autorités compétentes.
     */
    @Column(name = "approval_date")
    private LocalDate approvalDate;

    /**
     * Nombre cible de patients à inclure dans l'étude.
     */
    @Column(name = "target_enrollment")
    private Integer targetEnrollment;

    /**
     * Nombre actuel de patients inclus.
     * Mis à jour lors de l'ajout de patients.
     */
    @Column(name = "current_enrollment")
    private Integer currentEnrollment = 0;

    /**
     * Indique si les Cliniques Universitaires Saint-Luc (CUSL) sont le promoteur.
     * Permet un filtrage rapide des études internes vs externes.
     */
    @Column(name = "is_sponsor_cusl", nullable = false)
    private boolean isSponsorCusl = false;

    /**
     * Description libre du protocole de l'étude.
     * Stocké en TEXT pour permettre des descriptions longues.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
