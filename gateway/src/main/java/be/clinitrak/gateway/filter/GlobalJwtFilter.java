package be.clinitrak.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Set;

/**
 * Filtre JWT global du gateway CliniTrak.
 * Intercepte toutes les requêtes, laisse passer les endpoints publics,
 * et valide le Bearer token JWT pour les endpoints protégés.
 * En cas de succès, propage les claims JWT en headers pour les services aval.
 */
@Slf4j
@Component
public class GlobalJwtFilter implements GlobalFilter, Ordered {

    @Value("${clinitrak.jwt.secret}")
    private String jwtSecret;

    /**
     * Chemins publics ne nécessitant pas de validation JWT.
     * Ces paths sont vérifiés par préfixe.
     */
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/v1/auth/",
            "/api/v1/exchange/auth/",
            "/api/v1/exchange/register",
            "/api/v1/exchange/verify",
            "/swagger-ui",
            "/api-docs",
            "/actuator/health",
            "/actuator/info"
    );

    /**
     * Priorité du filtre (le plus bas = exécuté en premier parmi les GlobalFilter).
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * Intercepte chaque requête :
     * 1. Laisse passer les paths publics sans vérification.
     * 2. Pour les autres : vérifie la présence et la validité du JWT Bearer.
     * 3. En cas de succès : propage les claims dans les headers X-User-*.
     * 4. En cas d'échec : retourne 401 Unauthorized.
     *
     * @param exchange échange HTTP courant
     * @param chain    chaîne de filtres suivante
     * @return Mono indiquant la fin du traitement
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Laisser passer les endpoints publics
        if (isPublicPath(path)) {
            log.debug("Path public, pas de validation JWT : {}", path);
            return chain.filter(exchange);
        }

        // Vérifier le header Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Requête sans token JWT vers : {}", path);
            return unauthorized(exchange, "Token JWT manquant");
        }

        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extraire les claims
            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String tenantId = claims.get("tenantId", String.class);
            List<?> roles = claims.get("roles", List.class);
            String rolesStr = roles != null ? String.join(",", roles.stream()
                    .map(Object::toString).toList()) : "";

            // Propager les claims dans les headers vers les services aval
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-User-Email", email != null ? email : "");
                        headers.set("X-User-Id", userId != null ? userId : "");
                        headers.set("X-User-Roles", rolesStr);
                        headers.set("X-Tenant-ID", tenantId != null ? tenantId : "");
                    }))
                    .build();

            log.debug("JWT validé pour {} ({}) → {}", email, tenantId, path);
            return chain.filter(mutatedExchange);

        } catch (JwtException ex) {
            log.warn("JWT invalide pour le path {} : {}", path, ex.getMessage());
            return unauthorized(exchange, "Token JWT invalide ou expiré");
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de la validation JWT pour {} : {}", path, ex.getMessage());
            return unauthorized(exchange, "Erreur de validation du token");
        }
    }

    /**
     * Retourne une réponse 401 Unauthorized avec un message JSON RFC 7807.
     *
     * @param exchange échange HTTP courant
     * @param message  message d'erreur à inclure dans la réponse
     * @return Mono complété après l'écriture de la réponse
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"type\":\"https://clinitrak.be/errors/unauthorized\"," +
                "\"title\":\"Non autorisé\"," +
                "\"status\":401," +
                "\"detail\":\"%s\"}", message);

        byte[] bytes = body.getBytes();
        org.springframework.core.io.buffer.DataBuffer buffer =
                exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Détermine si un chemin est public (ne requiert pas de JWT).
     *
     * @param path chemin de la requête
     * @return {@code true} si le chemin est public
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith)
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/register");
    }
}
