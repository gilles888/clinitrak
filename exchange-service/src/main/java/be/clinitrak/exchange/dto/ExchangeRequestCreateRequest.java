package be.clinitrak.exchange.dto;

import be.clinitrak.exchange.domain.enums.ExchangeRequestType;
import be.clinitrak.exchange.domain.enums.TargetModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de création d'une demande d'échange.
 *
 * @param targetModule  module cible (CE ou CTC)
 * @param requestType   type de demande
 * @param title         titre court de la demande
 * @param description   description détaillée (facultatif)
 */
public record ExchangeRequestCreateRequest(
    @NotNull TargetModule targetModule,
    @NotNull ExchangeRequestType requestType,
    @NotBlank String title,
    String description
) {}
