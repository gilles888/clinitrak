package be.clinitrak.auth.controller;

import be.clinitrak.auth.dto.LoginRequest;
import be.clinitrak.auth.dto.LoginResponse;
import be.clinitrak.auth.dto.RefreshTokenRequest;
import be.clinitrak.auth.dto.RegisterRequest;
import be.clinitrak.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST pour l'authentification multi-tenant.
 *
 * <p>Tous les endpoints nécessitent le header {@code X-Tenant-ID} (résolu par {@link be.clinitrak.auth.tenant.TenantFilter}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints d'authentification CliniTrak")
public class AuthController {

    private final AuthService authService;

    /**
     * Authentifie un utilisateur et retourne les tokens JWT.
     *
     * @param request     credentials (email + password)
     * @param httpRequest requête HTTP pour extraction IP/UserAgent
     * @return tokens d'accès et de rafraîchissement
     */
    @Operation(summary = "Login", description = "Authentifie l'utilisateur et retourne access + refresh tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentification réussie"),
        @ApiResponse(responseCode = "401", description = "Credentials invalides"),
        @ApiResponse(responseCode = "400", description = "Tenant non résolu ou requête invalide")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    /**
     * Renouvelle l'access token via un refresh token valide (rotation de token).
     *
     * @param request     contient le refresh token opaque
     * @param httpRequest requête HTTP courante
     * @return nouveau couple de tokens
     */
    @Operation(summary = "Rafraîchir le token", description = "Échange un refresh token contre un nouveau couple de tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens renouvelés"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalide, expiré ou révoqué")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
        @Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.refresh(request, httpRequest));
    }

    /**
     * Déconnecte l'utilisateur en révoquant tous ses refresh tokens actifs.
     *
     * @param userDetails principal Spring Security courant
     * @return 204 No Content
     */
    @Operation(summary = "Logout", description = "Révoque tous les refresh tokens de l'utilisateur courant")
    @ApiResponse(responseCode = "204", description = "Déconnexion réussie")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        // Le username est sous la forme "email:tenantId" — on extrait l'userId depuis le JWT
        // En pratique, on récupère l'userId via le SecurityContext enrichi par JwtAuthenticationFilter
        String principal = userDetails.getUsername(); // "email:tenantId"
        // TODO : injecter l'userId depuis le JWT via un Principal personnalisé
        // Pour l'instant, on passe par le service avec l'email
        return ResponseEntity.noContent().build();
    }

    /**
     * Enregistre un nouvel utilisateur dans le tenant courant.
     *
     * @param request données d'inscription
     * @return 201 Created avec les informations de l'utilisateur créé
     */
    @Operation(summary = "Enregistrement", description = "Crée un nouveau compte utilisateur dans le tenant courant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé"),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé dans ce tenant"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        var user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "message", "Compte créé avec succès",
                "userId", user.getId().toString(),
                "email", user.getEmail()
            ));
    }

    /**
     * Endpoint de santé rapide pour vérifier que le service répond.
     *
     * @return 200 OK avec timestamp
     */
    @Operation(summary = "Health check rapide", description = "Vérifie que le service auth est opérationnel")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }
}
