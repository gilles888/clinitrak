package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.BillingSchedule;
import be.clinitrak.ctc.domain.enums.ContractStatus;
import be.clinitrak.ctc.domain.enums.ContractType;
import be.clinitrak.ctc.domain.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un contrat financier.
 *
 * @param id                   identifiant UUID du contrat
 * @param studyId              identifiant de l'étude associée
 * @param contractType         type de contrat
 * @param contractTypeLabel    libellé français du type de contrat
 * @param contractDate         date du contrat
 * @param amount               montant
 * @param currency             devise
 * @param currencyLabel        libellé français de la devise
 * @param billingSchedule      périodicité de facturation
 * @param billingScheduleLabel libellé français de la périodicité
 * @param paymentTerms         conditions de paiement
 * @param status               statut courant
 * @param statusLabel          libellé français du statut
 * @param createdAt            timestamp de création
 */
public record FinancialContractResponse(
    UUID id,
    String studyId,
    ContractType contractType,
    String contractTypeLabel,
    LocalDate contractDate,
    BigDecimal amount,
    Currency currency,
    String currencyLabel,
    BillingSchedule billingSchedule,
    String billingScheduleLabel,
    String paymentTerms,
    ContractStatus status,
    String statusLabel,
    LocalDateTime createdAt
) {}
