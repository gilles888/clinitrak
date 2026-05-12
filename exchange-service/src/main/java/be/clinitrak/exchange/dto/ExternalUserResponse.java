package be.clinitrak.exchange.dto;

import be.clinitrak.exchange.domain.enums.ExternalUserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de représentation publique d'un utilisateur externe.
 *
 * @param id            UUID de l'utilisateur
 * @param email         adresse email
 * @param firstName     prénom
 * @param lastName      nom de famille
 * @param organization  organisation ou institution
 * @param role          rôle externe
 * @param roleLabel     libellé français du rôle
 * @param verifiedEmail true si l'email a été vérifié
 * @param createdAt     date de création du compte
 */
public record ExternalUserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String organization,
    ExternalUserRole role,
    String roleLabel,
    boolean verifiedEmail,
    LocalDateTime createdAt
) {}
