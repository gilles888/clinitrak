package be.clinitrak.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO d'invitation d'un utilisateur dans un tenant.
 *
 * @param email     adresse email de l'invité
 * @param firstName prénom de l'invité
 * @param lastName  nom de famille de l'invité
 * @param role      rôle à assigner dans le tenant
 */
public record UserInviteRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String role
) {}
