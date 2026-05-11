package be.clinitrak.pharmacy.domain.entity;

import be.clinitrak.pharmacy.domain.enums.StockStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Unité de stock d'un médicament expérimental.
 *
 * <p>Représente un lot de médicament reçu par la pharmacie. Tout stock est
 * initialement placé en quarantaine ({@link StockStatus#QUARANTINE}) jusqu'à
 * validation par le pharmacien responsable.
 */
@Entity
@Table(name = "pharmacy_stocks")
@Getter
@Setter
public class DrugStock extends BaseEntity {

    /** Médicament associé à ce stock. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id")
    private InvestigationalDrug drug;

    /** Identifiant de l'étude clinique associée. */
    @Column(nullable = false)
    private String studyId;

    /** Quantité disponible dans ce lot. */
    @Column(nullable = false)
    private Integer quantity;

    /** Unité de mesure (ex: "comprimés", "flacons", "ml"). */
    private String unit;

    /** Date de réception du lot en pharmacie. */
    private LocalDate receivedDate;

    /** Date de péremption de ce lot. */
    private LocalDate expiryDate;

    /** Numéro de lot du fabricant. */
    private String batchNumber;

    /** Emplacement physique du stock en pharmacie (ex: "Réfrigérateur A-3"). */
    private String location;

    /** Statut courant du stock. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus status;

    /** Journal des températures de stockage (données brutes JSON ou texte libre). */
    @Column(columnDefinition = "TEXT")
    private String temperatureLog;

    /** Identifiant du tenant propriétaire de cet enregistrement. */
    @Column(nullable = false)
    private String tenantId;
}
