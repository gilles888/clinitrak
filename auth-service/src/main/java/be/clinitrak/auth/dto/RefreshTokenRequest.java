package be.clinitrak.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO immuable (record) pour le renouvellement de l'access token.
 *
 * @param refreshToken token de rafraîchissement opaque obtenu lors du login
 */
@Schema(description = "Requête de rafraîchissement du token d'accès")
public record RefreshTokenRequest(

    @Schema(description = "Refresh token opaque")
    @NotBlank(message = "Le refresh token est obligatoire")
    String refreshToken
) {}
