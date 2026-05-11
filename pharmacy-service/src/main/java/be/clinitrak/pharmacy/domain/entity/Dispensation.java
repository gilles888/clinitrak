package be.clinitrak.pharmacy.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Dispensation d'un médicament expérimental à un patient.
 *
 * <p>Lors de la création d'une dispensation, le stock disponible est automatiquement
 * décrémenté. Le retour éventuel du médicament non utilisé est tracé via
 * {@code returnDate} et {@code returnQuantity}.
 */
@Entity
@Table(name = "pharmacy_dispensations")
@Getter
@Setter
public class Dispensation extends BaseEntity {

    /** Médicament dispensé. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id")
    private InvestigationalDrug drug;

    /** Identifiant de l'étude clinique associée. */
    @Column(nullable = false)
    private String studyId;

    /** Code anonymisé du patient (ex: "STLUC-001-001"). */
    @Column(nullable = false)
    private String patientCode;

    /** Date de la dispensation. */
    @Column(nullable = false)
    private LocalDate dispensationDate;

    /** Identifiant (email ou matricule) du pharmacien ayant dispensé. */
    @Column(nullable = false)
    private String pharmacistId;

    /** Identifiant (email ou matricule) du médecin prescripteur. */
    @Column(nullable = false)
    private String prescriberId;

    /** Quantité dispensée. */
    @Column(nullable = false)
    private Integer quantity;

    /** Numéro ou référence de l'ordonnance. */
    private String prescription;

    /** Numéro de la visite d'étude lors de laquelle la dispensation a eu lieu. */
    private String visitNumber;

    /** Date de retour du médicament non consommé (si applicable). */
    private LocalDate returnDate;

    /** Quantité retournée (si applicable). */
    private Integer returnQuantity;

    /** Notes libres du pharmacien. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Identifiant du tenant propriétaire de cet enregistrement. */
    @Column(nullable = false)
    private String tenantId;
}
