package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de création/modification d'un contact d'étude clinique.
 *
 * @param contactType  type de contact (obligatoire)
 * @param firstName    prénom du contact (obligatoire)
 * @param lastName     nom de famille du contact (obligatoire)
 * @param email        adresse email (format validé)
 * @param phone        numéro de téléphone
 * @param organization organisation ou institution
 * @param isPrimary    indique si c'est le contact principal
 */
public record ContactRequest(
    @NotNull(message = "Le type de contact est obligatoire")
    ContactType contactType,

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut dépasser 100 caractères")
    String firstName,

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut dépasser 100 caractères")
    String lastName,

    @Email(message = "L'adresse email n'est pas valide")
    @Size(max = 255, message = "L'email ne peut dépasser 255 caractères")
    String email,

    @Size(max = 50, message = "Le téléphone ne peut dépasser 50 caractères")
    String phone,

    @Size(max = 255, message = "L'organisation ne peut dépasser 255 caractères")
    String organization,

    boolean isPrimary
) {}
