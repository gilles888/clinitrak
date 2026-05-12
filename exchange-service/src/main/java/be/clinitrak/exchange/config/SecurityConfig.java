package be.clinitrak.exchange.config;

import be.clinitrak.exchange.security.ExchangeJwtFilter;
import be.clinitrak.exchange.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration Spring Security 6 pour le exchange-service.
 *
 * <ul>
 *   <li>JWT stateless (pas de session serveur)</li>
 *   <li>CSRF désactivé (API REST protégée par JWT)</li>
 *   <li>CORS configuré pour les origines Angular et le portail externe</li>
 *   <li>{@code @PreAuthorize} activé via {@code @EnableMethodSecurity}</li>
 *   <li>Endpoints publics : /api/v1/exchange/auth/**, /api/v1/exchange/public/**</li>
 *   <li>Endpoints internes : /api/v1/exchange/internal/** (JWT interne, rôles CTC_MANAGER/SUPER_ADMIN/CE_MEMBER)</li>
 *   <li>Endpoints externes : /api/v1/exchange/requests/** (JWT externe via ExchangeJwtFilter)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/actuator/health",
        "/actuator/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api/v1/exchange/auth/**",
        "/api/v1/exchange/public/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ExchangeJwtFilter exchangeJwtFilter;

    @Value("${clinitrak.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    /**
     * Chaîne de filtres principale : stateless JWT, pas de CSRF, CORS activé.
     *
     * <p>Les deux filtres JWT sont appliqués : le filtre interne d'abord, puis le filtre externe.
     *
     * @param http configuration HttpSecurity
     * @return {@link SecurityFilterChain} configuré
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers("/api/v1/exchange/internal/**")
                    .hasAnyRole("CTC_MANAGER", "SUPER_ADMIN", "CE_MEMBER")
                .requestMatchers("/api/v1/exchange/requests/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(exchangeJwtFilter, JwtAuthenticationFilter.class)
            .build();
    }

    /**
     * Encodeur BCrypt (force 12) pour les mots de passe des utilisateurs externes.
     *
     * @return {@link PasswordEncoder} BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configuration CORS : autorise les origines Angular configurées.
     *
     * @return source de configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Tenant-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
