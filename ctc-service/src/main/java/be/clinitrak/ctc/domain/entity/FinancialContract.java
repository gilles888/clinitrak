package be.clinitrak.ctc.domain.entity;

import be.clinitrak.ctc.domain.enums.BillingSchedule;
import be.clinitrak.ctc.domain.enums.ContractStatus;
import be.clinitrak.ctc.domain.enums.ContractType;
import be.clinitrak.ctc.domain.enums.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Représente un contrat financier associé à une étude clinique.
 *
 * <p>Gère les conventions, avenants et contrats promoteur, avec le montant,
 * la devise, la périodicité de facturation et les conditions de paiement.
 */
@Getter
@Setter
@Entity
@Table(name = "ctc_financial_contracts")
public class FinancialContract extends BaseEntity {

    /** Identifiant UUID de l'étude dans le study-service (référence externe). */
    @Column(name = "study_id", nullable = false, length = 255)
    private String studyId;

    /** Type de contrat (convention, avenant, promoteur). */
    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 50)
    private ContractType contractType;

    /** Date de signature ou d'entrée en vigueur du contrat. */
    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    /** Montant du contrat avec deux décimales. */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Devise du contrat (EUR par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency = Currency.EUR;

    /** Périodicité de facturation (mensuelle, trimestrielle, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_schedule", length = 50)
    private BillingSchedule billingSchedule;

    /** Conditions et modalités de paiement décrites librement. */
    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    /** Statut courant du contrat (DRAFT par défaut). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContractStatus status = ContractStatus.DRAFT;

    /** Identifiant du tenant (UUID sous forme de String). */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;
}
