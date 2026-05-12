package be.clinitrak.exchange.security;

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
import java.util.UUID;
import java.util.function.Function;

/**
 * Service JWT dédié aux utilisateurs externes du exchange-service (jjwt 0.12.x).
 *
 * <p>Génère et valide les tokens JWT pour les {@code ExternalUser}, distincts
 * des tokens JWT internes émis par l'auth-service.
 *
 * <ul>
 *   <li>Claims spécifiques : externalUserId (UUID), role (ExternalUserRole)</li>
 *   <li>Algorithme : HS256</li>
 *   <li>Durée de validité : 24 heures</li>
 * </ul>
 */
@Slf4j
@Service
public class ExchangeJwtService {

    /** Durée de validité du token externe : 24 heures en millisecondes. */
    private static final long TOKEN_VALIDITY_MS = 24L * 60 * 60 * 1000;

    private final SecretKey signingKey;

    /**
     * Crée le service JWT externe avec la clé secrète dédiée.
     *
     * @param base64ExternalSecret secret encodé en Base64 pour les JWT externes
     */
    public ExchangeJwtService(
        @Value("${clinitrak.security.jwt.external-secret}") String base64ExternalSecret
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64ExternalSecret));
    }

    /**
     * Génère un JWT pour un utilisateur externe.
     *
     * @param externalUserId UUID de l'utilisateur externe
     * @param email          email (subject du token)
     * @param role           rôle externe de l'utilisateur
     * @return token JWT signé
     */
    public String generateToken(UUID externalUserId, String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(email)
            .claim("externalUserId", externalUserId.toString())
            .claim("role", role)
            .issuedAt(new Date(now))
            .expiration(new Date(now + TOKEN_VALIDITY_MS))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Valide un token JWT externe : vérifie la signature et l'expiration.
     *
     * @param token token JWT externe à valider
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
            log.debug("JWT externe invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrait l'UUID de l'utilisateur externe depuis le token.
     *
     * @param token token JWT externe valide
     * @return UUID de l'utilisateur externe
     */
    public UUID extractExternalUserId(String token) {
        String id = extractClaim(token, claims -> claims.get("externalUserId", String.class));
        return UUID.fromString(id);
    }

    /**
     * Extrait le rôle externe depuis le token.
     *
     * @param token token JWT externe valide
     * @return rôle de l'utilisateur externe (ex: "COMPANY", "INVESTIGATOR")
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extrait le sujet (email) du token.
     *
     * @param token token JWT externe valide
     * @return email de l'utilisateur externe
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait un claim spécifique du token via une fonction de résolution.
     *
     * @param token          token JWT externe
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
