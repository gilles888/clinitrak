package be.clinitrak.auth.security;

import be.clinitrak.auth.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre Spring Security qui valide le JWT présent dans le header Authorization.
 *
 * <p>Chaîne de traitement :
 * <ol>
 *   <li>Extrait le token du header {@code Authorization: Bearer <token>}</li>
 *   <li>Valide la signature et l'expiration via {@link JwtService}</li>
 *   <li>Charge les {@link UserDetails} et positionne l'authentification dans le SecurityContext</li>
 *   <li>Alimente le {@link TenantContext} avec le tenantId extrait du token</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

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
                String email = jwtService.extractSubject(jwt);
                String tenantId = jwtService.extractTenantId(jwt).toString();

                UserDetails userDetails = userDetailsService.loadUserByUsername(email + ":" + tenantId);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Propage le tenant dans le contexte du thread courant
                TenantContext.setTenantId(tenantId);
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
