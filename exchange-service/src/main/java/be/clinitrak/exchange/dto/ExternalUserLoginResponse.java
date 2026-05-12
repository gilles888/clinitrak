package be.clinitrak.exchange.dto;

/**
 * DTO de réponse à une connexion réussie d'un utilisateur externe.
 *
 * @param accessToken token JWT externe à inclure dans les requêtes suivantes
 * @param tokenType   type du token, toujours "Bearer"
 * @param user        informations de l'utilisateur connecté
 */
public record ExternalUserLoginResponse(
    String accessToken,
    String tokenType,
    ExternalUserResponse user
) {}
