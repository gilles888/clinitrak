package be.clinitrak.exchange.dto;

import be.clinitrak.exchange.domain.enums.ExchangeRequestType;
import be.clinitrak.exchange.domain.enums.ExchangeStatus;
import be.clinitrak.exchange.domain.enums.TargetModule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de représentation complète d'une demande d'échange.
 *
 * @param id                 UUID de la demande
 * @param externalUserId     UUID de l'utilisateur externe
 * @param externalUserName   nom complet de l'utilisateur externe
 * @param targetModule       module cible
 * @param targetModuleLabel  libellé du module cible
 * @param requestType        type de demande
 * @param requestTypeLabel   libellé du type de demande
 * @param title              titre de la demande
 * @param description        description détaillée
 * @param submissionDate     date de soumission formelle
 * @param status             statut courant
 * @param statusLabel        libellé du statut
 * @param internalStudyId    identifiant de l'étude interne (après acceptation)
 * @param documents          liste des documents attachés
 * @param createdAt          date de création
 */
public record ExchangeRequestResponse(
    UUID id,
    UUID externalUserId,
    String externalUserName,
    TargetModule targetModule,
    String targetModuleLabel,
    ExchangeRequestType requestType,
    String requestTypeLabel,
    String title,
    String description,
    LocalDateTime submissionDate,
    ExchangeStatus status,
    String statusLabel,
    String internalStudyId,
    List<ExchangeDocumentResponse> documents,
    LocalDateTime createdAt
) {}
