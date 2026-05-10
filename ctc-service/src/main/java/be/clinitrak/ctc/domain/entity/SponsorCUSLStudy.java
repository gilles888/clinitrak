package be.clinitrak.ctc.domain.entity;

import be.clinitrak.ctc.domain.enums.RegulatoryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente une étude clinique gérée par le CTC en tant que sponsor CUSL.
 *
 * <p>Pour les études dont les Cliniques Universitaires Saint-Luc (CUSL) sont promoteur,
 * le CTC assure la gestion globale : assignation du chef de projet, coordination
 * des CRA, suivi budgétaire et avancement réglementaire.
 */
@Getter
@Setter
@Entity
@Table(name = "ctc_sponsor_studies")
public class SponsorCUSLStudy extends BaseEntity {

    /** Identifiant UUID de l'étude dans le study-service (unique par tenant). */
    @Column(name = "study_id", nullable = false, unique = true, length = 255)
    private String studyId;

    /** UUID de l'utilisateur chef de projet assigné à cette étude. */
    @Column(name = "project_manager_id", length = 255)
    private String projectManagerId;

    /**
     * Liste des UUID des CRA (Clinical Research Associates) assignés à l'étude.
     * Stockée dans la table de jointure {@code ctc_sponsor_cra_assignments}.
     */
    @ElementCollection
    @CollectionTable(
        name = "ctc_sponsor_cra_assignments",
        joinColumns = @JoinColumn(name = "sponsor_study_id")
    )
    @Column(name = "cra_id")
    private List<String> craIds = new ArrayList<>();

    /** Budget total alloué à l'étude (montant en euros). */
    @Column(name = "budget_total", precision = 15, scale = 2)
    private BigDecimal budgetTotal;

    /** Montant déjà dépensé sur le budget de l'étude. */
    @Column(name = "budget_spent", precision = 15, scale = 2)
    private BigDecimal budgetSpent = BigDecimal.ZERO;

    /**
     * Jalons de l'étude au format JSON libre.
     * Stocké en colonnes JSONB PostgreSQL pour une flexibilité maximale.
     */
    @Column(name = "milestones", columnDefinition = "jsonb")
    private String milestones;

    /** Statut réglementaire courant de l'étude (IN_PREPARATION par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "regulatory_status", nullable = false, length = 50)
    private RegulatoryStatus regulatoryStatus = RegulatoryStatus.IN_PREPARATION;

    /** Identifiant du tenant (UUID sous forme de String). */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
