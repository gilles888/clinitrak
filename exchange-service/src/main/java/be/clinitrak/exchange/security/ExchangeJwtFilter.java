package be.clinitrak.exchange.security;

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

/**
 * Filtre Spring Security qui valide le JWT externe des utilisateurs externes.
 *
 * <p>Utilisé pour les endpoints {@code /api/v1/exchange/requests/**} qui requièrent
 * un JWT émis par l'exchange-service lui-même (via {@code POST /api/v1/exchange/auth/login}).
 *
 * <p>Chaîne de traitement :
 * <ol>
 *   <li>Extrait le token du header {@code Authorization: Bearer &lt;token&gt;}</li>
 *   <li>Valide la signature et l'expiration via {@link ExchangeJwtService}</li>
 *   <li>Extrait l'externalUserId et le rôle, puis positionne l'authentification</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeJwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final ExchangeJwtService exchangeJwtService;

    /**
     * Valide le JWT externe et positionne le contexte de sécurité.
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
            // Seulement si ce token est un JWT externe valide et qu'aucune auth n'est déjà posée
            if (exchangeJwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String externalUserId = exchangeJwtService.extractExternalUserId(jwt).toString();
                String role = exchangeJwtService.extractRole(jwt);

                List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_EXTERNAL_" + role))
                    : List.of();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    externalUserId, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT externe valide — externalUserId: {}, role: {}", externalUserId, role);
            }
        } catch (Exception e) {
            log.warn("Impossible de valider le token JWT externe : {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
