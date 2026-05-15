package be.clinitrak.ethics.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre de résolution du tenant courant pour le ethics-service.
 *
 * <p>Stratégie de résolution (par ordre de priorité) :
 * <ol>
 *   <li>UUID déjà positionné par {@code JwtAuthenticationFilter} (Spring Security) — priorité absolue</li>
 *   <li>Header {@code X-Tenant-ID} (slug du tenant)</li>
 *   <li>Sous-domaine de l'Host (ex: {@code saintluc.clinitrak.be} → slug "saintluc")</li>
 * </ol>
 *
 * <p>Le tenant résolu est stocké dans le {@link TenantContext} pour la durée de la requête.
 * Si Spring Security a déjà résolu l'UUID tenant depuis le JWT, ce filtre ne l'écrase pas.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantResolver tenantResolver;

    /**
     * Résout le tenant depuis la requête et le positionne dans le {@link TenantContext}.
     *
     * @param request     requête HTTP entrante
     * @param response    réponse HTTP
     * @param filterChain chaîne de filtres
     * @throws ServletException en cas d'erreur de servlet
     * @throws IOException      en cas d'erreur d'entrée/sortie
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Spring Security's JwtAuthenticationFilter (runs before this servlet filter) may have
            // already set a valid UUID tenant in TenantContext. Do not overwrite it with the slug.
            if (!isValidUuid(TenantContext.getTenantId())) {
                String tenantSlug = resolveSlug(request);
                if (StringUtils.hasText(tenantSlug)) {
                    log.debug("Tenant résolu via filtre : {} pour {}", tenantSlug, request.getRequestURI());
                    TenantContext.setTenantId(tenantSlug);
                } else {
                    log.debug("Aucun tenant résolu par TenantFilter pour : {}", request.getRequestURI());
                }
            } else {
                log.debug("Tenant UUID déjà positionné par JwtAuthenticationFilter : {} pour {}",
                    TenantContext.getTenantId(), request.getRequestURI());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Résout le slug du tenant depuis la requête.
     *
     * @param request requête HTTP entrante
     * @return slug du tenant ou null si non trouvé
     */
    private String resolveSlug(HttpServletRequest request) {
        // 1. Header X-Tenant-ID (priorité maximale)
        String headerValue = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(headerValue)) {
            return headerValue.trim().toLowerCase();
        }

        // 2. Sous-domaine de l'Host
        return tenantResolver.resolveFromHost(request.getServerName());
    }

    private boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
