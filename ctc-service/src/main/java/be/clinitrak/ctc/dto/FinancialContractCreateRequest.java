package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.BillingSchedule;
import be.clinitrak.ctc.domain.enums.ContractType;
import be.clinitrak.ctc.domain.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de création d'un contrat financier.
 *
 * @param studyId         identifiant UUID de l'étude (obligatoire)
 * @param contractType    type de contrat (obligatoire)
 * @param contractDate    date du contrat (obligatoire)
 * @param amount          montant du contrat (obligatoire)
 * @param currency        devise (EUR par défaut si null)
 * @param billingSchedule périodicité de facturation
 * @param paymentTerms    conditions de paiement libres
 */
public record FinancialContractCreateRequest(
    @NotBlank String studyId,
    @NotNull ContractType contractType,
    @NotNull LocalDate contractDate,
    @NotNull BigDecimal amount,
    Currency currency,
    BillingSchedule billingSchedule,
    String paymentTerms
) {}
