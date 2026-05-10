package be.clinitrak.ctc.dto;

import be.clinitrak.ctc.domain.enums.DeskType;
import be.clinitrak.ctc.domain.enums.Priority;
import be.clinitrak.ctc.domain.enums.RequestType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de création d'une demande desk CTC.
 *
 * <p>Contient toutes les informations nécessaires pour initier une demande :
 * étude concernée, type de desk, informations du demandeur et nature de la demande.
 *
 * @param studyId               identifiant UUID de l'étude (obligatoire)
 * @param deskType              type de desk académique ou commercial (obligatoire)
 * @param requestorName         nom complet du demandeur (obligatoire)
 * @param requestorEmail        email du demandeur (doit être valide si fourni)
 * @param requestorOrganization organisation ou service du demandeur
 * @param requestType           type de demande (obligatoire)
 * @param priority              priorité de la demande (MEDIUM par défaut si null)
 * @param deadline              date limite souhaitée par le demandeur
 * @param notes                 notes ou informations complémentaires libres
 */
public record TrialDeskRequestCreateRequest(
    @NotBlank String studyId,
    @NotNull DeskType deskType,
    @NotBlank String requestorName,
    @Email String requestorEmail,
    String requestorOrganization,
    @NotNull RequestType requestType,
    Priority priority,
    LocalDate deadline,
    String notes
) {}
