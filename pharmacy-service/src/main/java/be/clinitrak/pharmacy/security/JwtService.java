package be.clinitrak.pharmacy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service de validation des JWT pour le pharmacy-service (jjwt 0.12.x).
 *
 * <p>Ce service ne génère pas de tokens — il valide uniquement les tokens émis
 * par l'auth-service, en utilisant le même secret partagé.
 *
 * <ul>
 *   <li>Claims attendus : sub (email), tenantId (UUID), userId (UUID), roles (List)</li>
 *   <li>Algorithme : HS256</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;

    /**
     * Crée le service JWT avec la clé secrète partagée avec l'auth-service.
     *
     * @param base64Secret secret encodé en Base64 (identique à l'auth-service)
     */
    public JwtService(
        @Value("${clinitrak.security.jwt.secret}") String base64Secret
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /**
     * Valide un token JWT : vérifie la signature et l'expiration.
     *
     * @param token token JWT à valider
     * @return {@code true} si le token est valide et non expiré
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrait le sujet (email) du token.
     *
     * @param token token JWT valide
     * @return email de l'utilisateur
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait l'UUID du tenant depuis le token.
     *
     * @param token token JWT valide
     * @return UUID du tenant
     */
    public UUID extractTenantId(String token) {
        String tenantIdStr = extractClaim(token, claims -> claims.get("tenantId", String.class));
        return UUID.fromString(tenantIdStr);
    }

    /**
     * Extrait l'UUID de l'utilisateur depuis le token.
     *
     * @param token token JWT valide
     * @return UUID de l'utilisateur
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userIdStr);
    }

    /**
     * Extrait la liste des rôles depuis le token.
     *
     * @param token token JWT valide
     * @return liste des rôles (ex: ["ROLE_PHARMACIST", "ROLE_SUPER_ADMIN"])
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * Extrait un claim spécifique du token via une fonction de résolution.
     *
     * @param token          token JWT
     * @param claimsResolver fonction d'extraction du claim
     * @param <T>            type du claim retourné
     * @return valeur du claim extrait
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }
}
