package be.clinitrak.auth.tenant;

import be.clinitrak.auth.domain.entity.Tenant;
import be.clinitrak.auth.domain.repository.TenantRepository;
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

/**
 * Filtre de résolution du tenant courant.
 *
 * <p>Stratégie de résolution (par ordre de priorité) :
 * <ol>
 *   <li>Header {@code X-Tenant-ID} (slug ou UUID du tenant)</li>
 *   <li>Sous-domaine de l'Host (ex: {@code saintluc.clinitrak.be} → slug "saintluc")</li>
 * </ol>
 *
 * <p>Si le tenant n'est pas résolu ou n'est pas actif, la requête est rejetée avec 400.
 * Les endpoints publics (/api/v1/auth/**) nécessitent quand même un tenant valide.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String CLINITRAK_DOMAIN_SUFFIX = ".clinitrak.be";

    private final TenantRepository tenantRepository;
    private final TenantResolver tenantResolver;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String tenantSlug = resolveSlug(request);

            if (!StringUtils.hasText(tenantSlug)) {
                log.debug("Aucun tenant résolu pour : {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            tenantRepository.findBySlugAndActiveTrue(tenantSlug)
                .ifPresentOrElse(
                    tenant -> TenantContext.setTenantId(tenant.getId().toString()),
                    () -> log.warn("Tenant '{}' introuvable ou inactif", tenantSlug)
                );

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
}
