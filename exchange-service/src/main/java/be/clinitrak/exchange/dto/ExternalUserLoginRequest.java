package be.clinitrak.exchange.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de connexion d'un utilisateur externe.
 *
 * @param email    adresse email
 * @param password mot de passe en clair
 */
public record ExternalUserLoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
