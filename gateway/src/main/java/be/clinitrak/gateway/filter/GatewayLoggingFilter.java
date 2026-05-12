package be.clinitrak.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre de logging global du gateway CliniTrak.
 * Enregistre chaque requête entrante avec : méthode HTTP, chemin, tenant, utilisateur et durée.
 */
@Slf4j
@Component
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

    /**
     * Priorité du filtre (après le filtre JWT pour avoir accès aux headers injectés).
     */
    @Override
    public int getOrder() {
        return -99;
    }

    /**
     * Intercepte chaque requête pour la logger avant et après le traitement.
     * Calcule et affiche la durée totale de traitement en millisecondes.
     *
     * @param exchange échange HTTP courant
     * @param chain    chaîne de filtres suivante
     * @return Mono indiquant la fin du traitement
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String tenant = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        String user = exchange.getRequest().getHeaders().getFirst("X-User-Email");
        String remoteAddr = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        log.info("[GATEWAY] → {} {} | tenant={} | user={} | ip={}",
                method, path,
                tenant != null ? tenant : "-",
                user != null ? user : "anonymous",
                remoteAddr);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("[GATEWAY] ← {} {} | status={} | duration={}ms",
                            method, path, status, duration);
                });
    }
}
