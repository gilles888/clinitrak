package be.clinitrak.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

/**
 * DTO immuable (record) pour la réponse de login.
 *
 * @param accessToken   JWT access token (validité : 15 minutes)
 * @param refreshToken  opaque refresh token (validité : 7 jours)
 * @param tokenType     type de token (toujours "Bearer")
 * @param expiresIn     durée de validité de l'access token en secondes
 * @param user          informations de base de l'utilisateur connecté
 */
@Schema(description = "Réponse d'authentification")
public record LoginResponse(

    @Schema(description = "JWT access token")
    String accessToken,

    @Schema(description = "Refresh token opaque pour renouveler l'access token")
    String refreshToken,

    @Schema(description = "Type de token", example = "Bearer")
    String tokenType,

    @Schema(description = "Durée de validité en secondes", example = "900")
    long expiresIn,

    @Schema(description = "Informations utilisateur")
    UserInfo user
) {

    /**
     * Informations utilisateur embarquées dans la réponse de login.
     *
     * @param id        UUID de l'utilisateur
     * @param email     email de l'utilisateur
     * @param firstName prénom
     * @param lastName  nom
     * @param tenantId  UUID du tenant courant
     * @param roles     ensemble des rôles de l'utilisateur
     */
    @Schema(description = "Informations de l'utilisateur authentifié")
    public record UserInfo(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UUID tenantId,
        Set<String> roles
    ) {}

    /** Constructeur de commodité avec type "Bearer" par défaut. */
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
