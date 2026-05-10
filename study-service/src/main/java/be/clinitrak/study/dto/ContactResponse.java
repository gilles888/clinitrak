package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.ContactType;

import java.util.UUID;

/**
 * DTO de réponse pour un contact d'étude clinique.
 *
 * @param id               identifiant UUID du contact
 * @param contactType      type de contact (enum)
 * @param contactTypeLabel libellé français du type de contact
 * @param firstName        prénom du contact
 * @param lastName         nom de famille du contact
 * @param email            adresse email
 * @param phone            numéro de téléphone
 * @param organization     organisation ou institution
 * @param isPrimary        indique si c'est le contact principal
 * @param active           indique si le contact est actif
 */
public record ContactResponse(
    UUID id,
    ContactType contactType,
    String contactTypeLabel,
    String firstName,
    String lastName,
    String email,
    String phone,
    String organization,
    boolean isPrimary,
    boolean active
) {}
