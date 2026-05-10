package be.clinitrak.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service de génération et de validation des JWT (jjwt 0.12.x).
 *
 * <ul>
 *   <li>Access token : validité configurable (défaut 15 min), signé HS256</li>
 *   <li>Claims embarqués : sub (email), tenantId, userId, roles</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtService(
        @Value("${clinitrak.security.jwt.secret}") String base64Secret,
        @Value("${clinitrak.security.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    /**
     * Génère un access token JWT pour un utilisateur.
     *
     * @param email    sujet (email de l'utilisateur)
     * @param userId   UUID de l'utilisateur
     * @param tenantId UUID du tenant courant
     * @param extraClaims claims additionnels (ex: roles)
     * @return JWT signé
     */
    public String generateAccessToken(String email, UUID userId, UUID tenantId, Map<String, Object> extraClaims) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(email)
            .claim("userId", userId.toString())
            .claim("tenantId", tenantId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Extrait le sujet (email) d'un token.
     *
     * @param token JWT à parser
     * @return sujet du token
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait le tenantId d'un token.
     *
     * @param token JWT à parser
     * @return UUID du tenant
     */
    public UUID extractTenantId(String token) {
        String tenantIdStr = extractClaim(token, claims -> claims.get("tenantId", String.class));
        return UUID.fromString(tenantIdStr);
    }

    /**
     * Extrait le userId d'un token.
     *
     * @param token JWT à parser
     * @return UUID de l'utilisateur
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userIdStr);
    }

    /**
     * Valide un token JWT.
     *
     * @param token token à valider
     * @return true si le token est valide et non expiré
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
     * Retourne la durée de validité de l'access token en secondes.
     *
     * @return durée en secondes
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }
}
