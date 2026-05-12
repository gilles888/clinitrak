package be.clinitrak.exchange.controller;

import be.clinitrak.exchange.dto.*;
import be.clinitrak.exchange.service.ExternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST public pour l'authentification des utilisateurs externes.
 *
 * <p>Endpoints publics (pas de JWT requis) :
 * <ul>
 *   <li>POST /register — inscription d'un nouvel utilisateur externe</li>
 *   <li>POST /login — connexion et obtention du JWT externe</li>
 *   <li>GET /verify/{token} — vérification de l'email</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/exchange/auth")
@RequiredArgsConstructor
@Tag(name = "Exchange Auth", description = "Authentification des utilisateurs externes du portail d'échange")
public class ExchangeAuthController {

    private final ExternalUserService externalUserService;

    /**
     * Enregistre un nouvel utilisateur externe.
     *
     * @param req données d'enregistrement
     * @return 201 Created avec les informations de l'utilisateur créé
     */
    @PostMapping("/register")
    @Operation(summary = "Inscription d'un utilisateur externe",
               description = "Crée un compte utilisateur externe et envoie un email de vérification")
    public ResponseEntity<ExternalUserResponse> register(@Valid @RequestBody ExternalUserRegisterRequest req) {
        ExternalUserResponse response = externalUserService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Connecte un utilisateur externe.
     *
     * @param req données de connexion
     * @return 200 OK avec le JWT externe et les informations de l'utilisateur
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion d'un utilisateur externe",
               description = "Authentifie l'utilisateur et retourne un JWT externe valable 24h")
    public ResponseEntity<ExternalUserLoginResponse> login(@Valid @RequestBody ExternalUserLoginRequest req) {
        return ResponseEntity.ok(externalUserService.login(req));
    }

    /**
     * Vérifie l'email d'un utilisateur externe.
     *
     * @param token token de vérification envoyé par email
     * @return 200 OK avec les informations de l'utilisateur mis à jour
     */
    @GetMapping("/verify/{token}")
    @Operation(summary = "Vérification d'email",
               description = "Valide le token de vérification et active le compte")
    public ResponseEntity<ExternalUserResponse> verifyEmail(@PathVariable String token) {
        return ResponseEntity.ok(externalUserService.verifyEmail(token));
    }
}
