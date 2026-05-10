package be.clinitrak.study.domain.entity;

import be.clinitrak.study.domain.enums.StudyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Historique des changements de statut d'une étude clinique.
 *
 * <p>Chaque transition de statut crée une nouvelle entrée dans cette table,
 * permettant un audit complet du cycle de vie réglementaire de l'étude.
 */
@Getter
@Setter
@Entity
@Table(
    name = "study_status_history",
    indexes = {
        @Index(name = "idx_status_history_study", columnList = "study_id"),
        @Index(name = "idx_status_history_tenant", columnList = "tenant_id")
    }
)
public class StudyStatusHistory extends BaseEntity {

    /**
     * Étude clinique concernée par ce changement de statut.
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
     * Nouveau statut de l'étude après ce changement.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StudyStatus status;

    /**
     * Date effective du changement de statut.
     */
    @Column(name = "status_date", nullable = false)
    private LocalDate statusDate;

    /**
     * Commentaire ou justification du changement de statut.
     * Obligatoire pour certaines transitions (ex: suspension, retrait).
     */
    @Column(name = "comment", length = 2000)
    private String comment;

    /**
     * Identifiant (email) de l'utilisateur ayant effectué le changement.
     */
    @Column(name = "changed_by", length = 255)
    private String changedBy;
}
