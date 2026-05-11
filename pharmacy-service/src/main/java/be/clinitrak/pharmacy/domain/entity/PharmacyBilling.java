package be.clinitrak.pharmacy.domain.entity;

import be.clinitrak.pharmacy.domain.enums.BillingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Facturation pharmacie pour une période et une étude données.
 *
 * <p>Regroupe les coûts de dispensation d'une étude sur une période donnée
 * et génère une facture destinée au promoteur.
 */
@Entity
@Table(name = "pharmacy_billing")
@Getter
@Setter
public class PharmacyBilling extends BaseEntity {

    /** Identifiant de l'étude clinique facturée. */
    @Column(nullable = false)
    private String studyId;

    /** Période de facturation (ex: "2026-Q1", "2026-01"). */
    private String billingPeriod;

    /** Montant total facturé en euros. */
    private BigDecimal totalCost;

    /** Numéro de facture unique. */
    private String invoiceNumber;

    /** Statut de la facture (DRAFT, SENT, PAID, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingStatus status;

    /** Date d'envoi de la facture au promoteur. */
    private LocalDate sentDate;

    /** Date de réception du paiement. */
    private LocalDate paidDate;

    /** Identifiant du tenant propriétaire de cet enregistrement. */
    @Column(nullable = false)
    private String tenantId;
}
