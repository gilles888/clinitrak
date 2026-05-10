package be.clinitrak.ctc.security;

import be.clinitrak.ctc.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtre Spring Security qui valide le JWT présent dans le header Authorization.
 *
 * <p>Chaîne de traitement :
 * <ol>
 *   <li>Extrait le token du header {@code Authorization: Bearer &lt;token&gt;}</li>
 *   <li>Valide la signature et l'expiration via {@link JwtService}</li>
 *   <li>Extrait les rôles du token et positionne l'authentification dans le SecurityContext</li>
 *   <li>Alimente le {@link TenantContext} avec le tenantId UUID extrait du token</li>
 * </ol>
 *
 * <p>Le ctc-service ne dispose pas de UserDetailsService propre :
 * l'authentification est entièrement basée sur les claims du JWT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtService jwtService;

    /**
     * Valide le JWT et positionne le contexte de sécurité et de tenant.
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

        final String authHeader = request.getHeader(AUTH_HEADER);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (jwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String subject = jwtService.extractSubject(jwt);
                String tenantId = jwtService.extractTenantId(jwt).toString();
                List<String> roles = jwtService.extractRoles(jwt);

                List<SimpleGrantedAuthority> authorities = roles != null
                    ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                    : List.of();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    subject, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Surcharge le TenantContext avec l'UUID issu du token (plus fiable que le header)
                TenantContext.setTenantId(tenantId);
                log.debug("JWT valide — utilisateur: {}, tenant: {}", subject, tenantId);
            }
        } catch (Exception e) {
            log.warn("Impossible de valider le token JWT : {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Nettoie le ThreadLocal pour éviter les fuites entre requêtes (pool de threads)
            TenantContext.clear();
        }
    }
}
