package be.clinitrak.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configuration du rate limiting Redis pour le gateway CliniTrak.
 * Définit la stratégie de résolution de la clé de rate limiting.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Résolveur de clé pour le rate limiting.
     * Utilise l'adresse IP du client comme clé de bucket Redis.
     * En production, on peut utiliser l'userId depuis le header X-User-Id.
     *
     * @return KeyResolver basé sur l'IP du client
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Utilise l'userId si disponible (requête authentifiée), sinon l'IP
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            String remoteAddr = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
            return Mono.just(remoteAddr);
        };
    }
}
