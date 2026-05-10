package be.clinitrak.auth.service;

import be.clinitrak.auth.domain.entity.RefreshToken;
import be.clinitrak.auth.domain.entity.Role;
import be.clinitrak.auth.domain.entity.Tenant;
import be.clinitrak.auth.domain.entity.User;
import be.clinitrak.auth.domain.repository.RefreshTokenRepository;
import be.clinitrak.auth.domain.repository.RoleRepository;
import be.clinitrak.auth.domain.repository.TenantRepository;
import be.clinitrak.auth.domain.repository.UserRepository;
import be.clinitrak.auth.dto.LoginRequest;
import be.clinitrak.auth.dto.LoginResponse;
import be.clinitrak.auth.dto.RefreshTokenRequest;
import be.clinitrak.auth.dto.RegisterRequest;
import be.clinitrak.auth.exception.AuthException;
import be.clinitrak.auth.security.JwtService;
import be.clinitrak.auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service principal d'authentification.
 *
 * <p>Gère le cycle de vie complet des tokens :
 * login → access token (15min) + refresh token (7j) → refresh → logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int REFRESH_TOKEN_DAYS = 7;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authentifie un utilisateur et génère les tokens JWT.
     *
     * @param request     credentials de l'utilisateur
     * @param httpRequest requête HTTP pour extraction IP/UserAgent
     * @return paire access + refresh token avec informations utilisateur
     * @throws AuthException si les credentials sont invalides ou le compte verrouillé
     */
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AuthException("Tenant non résolu — fournir le header X-Tenant-ID");
        }

        Tenant tenant = tenantRepository.findById(UUID.fromString(tenantId))
            .orElseThrow(() -> new AuthException("Tenant introuvable : " + tenantId));

        User user = userRepository.findByEmailAndTenantId(request.email(), UUID.fromString(tenantId))
            .orElseThrow(() -> new AuthException("Identifiants invalides"));

        // Vérification verrouillage
        if (user.isAccountLocked()) {
            if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
                throw new AuthException("Compte temporairement verrouillé. Réessayez plus tard.");
            }
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email() + ":" + tenantId,
                    request.password()
                )
            );
        } catch (AuthenticationException e) {
            handleFailedLogin(user);
            throw new AuthException("Identifiants invalides");
        }

        // Succès : réinitialisation du compteur d'échecs
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        String accessToken = jwtService.generateAccessToken(
            user.getEmail(),
            user.getId(),
            user.getTenantId(),
            Map.of("roles", roleNames, "tenantSlug", tenant.getSlug())
        );

        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
            .token(refreshTokenValue)
            .user(user)
            .tenantId(user.getTenantId())
            .expiresAt(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .ipAddress(extractIpAddress(httpRequest))
            .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Login réussi : {} (tenant: {})", user.getEmail(), tenant.getSlug());

        return LoginResponse.of(
            accessToken,
            refreshTokenValue,
            jwtService.getAccessTokenExpirationSeconds(),
            new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getTenantId(),
                roleNames
            )
        );
    }

    /**
     * Renouvelle l'access token à partir d'un refresh token valide (rotation).
     *
     * @param request     contient le refresh token opaque
     * @param httpRequest requête HTTP courante
     * @return nouveau couple de tokens
     * @throws AuthException si le refresh token est invalide, révoqué ou expiré
     */
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new AuthException("Refresh token invalide"));

        if (!storedToken.isUsable()) {
            throw new AuthException("Refresh token expiré ou révoqué");
        }

        // Rotation : révocation de l'ancien token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        Tenant tenant = tenantRepository.findById(storedToken.getTenantId())
            .orElseThrow(() -> new AuthException("Tenant introuvable"));

        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        String newAccessToken = jwtService.generateAccessToken(
            user.getEmail(),
            user.getId(),
            user.getTenantId(),
            Map.of("roles", roleNames, "tenantSlug", tenant.getSlug())
        );

        String newRefreshTokenValue = UUID.randomUUID().toString();
        RefreshToken newRefreshToken = RefreshToken.builder()
            .token(newRefreshTokenValue)
            .user(user)
            .tenantId(user.getTenantId())
            .expiresAt(Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS))
            .userAgent(httpRequest.getHeader("User-Agent"))
            .ipAddress(extractIpAddress(httpRequest))
            .build();
        refreshTokenRepository.save(newRefreshToken);

        return LoginResponse.of(
            newAccessToken,
            newRefreshTokenValue,
            jwtService.getAccessTokenExpirationSeconds(),
            new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getTenantId(),
                roleNames
            )
        );
    }

    /**
     * Déconnecte l'utilisateur en révoquant tous ses refresh tokens actifs.
     *
     * @param userId identifiant de l'utilisateur
     */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Logout : tous les refresh tokens révoqués pour userId={}", userId);
    }

    /**
     * Enregistre un nouvel utilisateur dans le tenant courant.
     *
     * @param request données d'inscription
     * @return l'utilisateur créé (sans mot de passe)
     * @throws AuthException si l'email est déjà utilisé ou le rôle demandé est invalide
     */
    @Transactional
    public User register(RegisterRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AuthException("Tenant non résolu");
        }

        UUID tenantUuid = UUID.fromString(tenantId);

        if (userRepository.existsByEmailIgnoreCaseAndTenantId(request.email(), tenantUuid)) {
            throw new AuthException("Un compte existe déjà avec cet email dans ce tenant");
        }

        Role role = roleRepository.findByNameAndTenantId(request.roleName(), null)
            .orElseThrow(() -> new AuthException("Rôle demandé inconnu : " + request.roleName()));

        User user = User.builder()
            .email(request.email().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.password()))
            .firstName(request.firstName())
            .lastName(request.lastName())
            .tenantId(tenantUuid)
            .roles(Set.of(role))
            .enabled(true)
            .build();

        User saved = userRepository.save(user);
        log.info("Nouvel utilisateur enregistré : {} (tenant: {})", saved.getEmail(), tenantId);
        return saved;
    }

    /** Incrémente le compteur d'échecs et verrouille si nécessaire. */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
            log.warn("Compte verrouillé après {} tentatives : {}", attempts, user.getEmail());
        }
        userRepository.save(user);
    }

    /** Extrait l'IP réelle en tenant compte des proxies (X-Forwarded-For). */
    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
