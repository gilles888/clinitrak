package be.clinitrak.admin.dto;

/**
 * DTO de réponse à une invitation d'utilisateur.
 *
 * @param email     adresse email de l'invité
 * @param firstName prénom
 * @param lastName  nom de famille
 * @param role      rôle assigné
 * @param status    statut de l'invitation (ex: "INVITATION_SENT")
 */
public record UserInviteResponse(
    String email,
    String firstName,
    String lastName,
    String role,
    String status
) {}
