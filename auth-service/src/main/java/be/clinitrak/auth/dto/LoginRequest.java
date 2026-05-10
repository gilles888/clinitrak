package be.clinitrak.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO immuable (record) pour la requête de login.
 *
 * @param email    adresse email de l'utilisateur
 * @param password mot de passe en clair (transporté en HTTPS uniquement)
 */
@Schema(description = "Requête d'authentification")
public record LoginRequest(

    @Schema(description = "Email de l'utilisateur", example = "marie.dupont@saintluc.be")
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    String email,

    @Schema(description = "Mot de passe", example = "P@ssw0rd!")
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    String password
) {}
