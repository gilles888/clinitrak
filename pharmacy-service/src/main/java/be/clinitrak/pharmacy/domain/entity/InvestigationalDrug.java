package be.clinitrak.pharmacy.domain.entity;

import be.clinitrak.pharmacy.domain.enums.DrugCategory;
import be.clinitrak.pharmacy.domain.enums.DrugForm;
import be.clinitrak.pharmacy.domain.enums.DrugRegulatoryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

/**
 * Médicament expérimental (IMP/NIMP/Placebo) utilisé dans les études cliniques.
 *
 * <p>Chaque médicament est rattaché à une étude et un tenant. Le code de randomisation
 * est chiffré en AES avant persistance via l'{@link be.clinitrak.pharmacy.service.EncryptionService}.
 */
@Entity
@Table(name = "pharmacy_drugs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class InvestigationalDrug extends BaseEntity {

    /** Identifiant de l'étude clinique associée. */
    @Column(nullable = false)
    private String studyId;

    /** Dénomination commerciale ou nom de code du médicament. */
    @Column(nullable = false)
    private String drugName;

    /** Dénomination commune internationale (DCI). */
    private String inn;

    /** Dosage (ex: "100mg", "5mg/ml"). */
    private String dosage;

    /** Forme pharmaceutique du médicament. */
    @Enumerated(EnumType.STRING)
    private DrugForm form;

    /** Nom du fabricant ou du promoteur de l'étude. */
    private String manufacturer;

    /** Numéro de lot du médicament. */
    private String batchNumber;

    /** Date de péremption du médicament. */
    private LocalDate expiryDate;

    /** Conditions de stockage (température, lumière, etc.). */
    @Column(columnDefinition = "TEXT")
    private String storageConditions;

    /** Catégorie réglementaire du médicament (IMP, NIMP, Placebo). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrugCategory category;

    /** Statut réglementaire du médicament (PENDING, APPROVED, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrugRegulatoryStatus regulatoryStatus;

    /** Code de randomisation chiffré en AES/ECB/PKCS5. */
    @Column(name = "randomization_code_encrypted")
    private String randomizationCodeEncrypted;

    /** Identifiant du tenant propriétaire de cet enregistrement. */
    @Column(nullable = false)
    private String tenantId;
}
