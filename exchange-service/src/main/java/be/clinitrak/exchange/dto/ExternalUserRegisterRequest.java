package be.clinitrak.exchange.dto;

import be.clinitrak.exchange.domain.enums.ExternalUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO d'enregistrement d'un nouvel utilisateur externe.
 *
 * @param email        adresse email unique (identifiant de connexion)
 * @param password     mot de passe en clair (sera hashé en BCrypt)
 * @param firstName    prénom
 * @param lastName     nom de famille
 * @param organization organisation ou institution (facultatif)
 * @param role         rôle externe de l'utilisateur
 */
public record ExternalUserRegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    String organization,
    @NotNull ExternalUserRole role
) {}
