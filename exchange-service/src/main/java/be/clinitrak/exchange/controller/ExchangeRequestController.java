package be.clinitrak.exchange.controller;

import be.clinitrak.exchange.domain.enums.ExchangeStatus;
import be.clinitrak.exchange.domain.enums.SenderType;
import be.clinitrak.exchange.dto.*;
import be.clinitrak.exchange.security.ExchangeJwtService;
import be.clinitrak.exchange.security.JwtService;
import be.clinitrak.exchange.service.ExchangeMessageService;
import be.clinitrak.exchange.service.ExchangeRequestService;
import be.clinitrak.exchange.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST pour les demandes d'échange.
 *
 * <p>Regroupe :
 * <ul>
 *   <li>Endpoints externes ({@code /requests/**}) — JWT externe requis</li>
 *   <li>Endpoints internes ({@code /internal/**}) — JWT interne requis, rôles CTC_MANAGER/SUPER_ADMIN/CE_MEMBER</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/exchange")
@RequiredArgsConstructor
@Tag(name = "Exchange Requests", description = "Gestion des demandes d'échange externe")
public class ExchangeRequestController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final ExchangeRequestService exchangeRequestService;
    private final ExchangeMessageService exchangeMessageService;
    private final ExchangeJwtService exchangeJwtService;
    private final JwtService jwtService;

    // =========================================================================
    // Endpoints EXTERNES (JWT externe)
    // =========================================================================

    /**
     * Crée une nouvelle demande d'échange (statut DRAFT).
     *
     * @param req     données de la demande
     * @param request requête HTTP (pour extraire le JWT externe)
     * @return demande créée
     */
    @PostMapping("/requests")
    @Operation(summary = "Créer une demande d'échange")
    public ResponseEntity<ExchangeRequestResponse> createRequest(
        @Valid @RequestBody ExchangeRequestCreateRequest req,
        HttpServletRequest request
    ) {
        UUID externalUserId = extractExternalUserId(request);
        String tenantId = resolveTenantId(request);
        return ResponseEntity.ok(exchangeRequestService.create(req, externalUserId, tenantId));
    }

    /**
     * Retourne toutes les demandes de l'utilisateur externe connecté.
     *
     * @param request requête HTTP (pour extraire le JWT externe)
     * @return liste des demandes de l'utilisateur
     */
    @GetMapping("/requests")
    @Operation(summary = "Mes demandes d'échange")
    public ResponseEntity<List<ExchangeRequestResponse>> getMyRequests(HttpServletRequest request) {
        UUID externalUserId = extractExternalUserId(request);
        String tenantId = resolveTenantId(request);
        return ResponseEntity.ok(exchangeRequestService.getByUser(externalUserId, tenantId));
    }

    /**
     * Retourne le détail d'une demande d'échange.
     *
     * @param id      UUID de la demande
     * @param request requête HTTP (pour extraire le JWT externe)
     * @return demande détaillée
     */
    @GetMapping("/requests/{id}")
    @Operation(summary = "Détail d'une demande d'échange")
    public ResponseEntity<ExchangeRequestResponse> getRequest(@PathVariable UUID id, HttpServletRequest request) {
        UUID externalUserId = extractExternalUserId(request);
        String tenantId = resolveTenantId(request);
        return ResponseEntity.ok(exchangeRequestService.getById(id, externalUserId, tenantId));
    }

    /**
     * Soumet formellement une demande d'échange (DRAFT → SUBMITTED).
     *
     * @param id      UUID de la demande
     * @param request requête HTTP (pour extraire le JWT externe)
     * @return demande mise à jour
     */
    @PostMapping("/requests/{id}/submit")
    @Operation(summary = "Soumettre une demande d'échange")
    public ResponseEntity<ExchangeRequestResponse> submitRequest(@PathVariable UUID id, HttpServletRequest request) {
        UUID externalUserId = extractExternalUserId(request);
        String tenantId = resolveTenantId(request);
        return ResponseEntity.ok(exchangeRequestService.submit(id, externalUserId, tenantId));
    }

    /**
     * Envoie un message dans une demande d'échange.
     *
     * @param id      UUID de la demande
     * @param req     données du message
     * @param request requête HTTP (pour extraire le JWT externe)
     * @return message créé
     */
    @PostMapping("/requests/{id}/messages")
    @Operation(summary = "Envoyer un message dans une demande")
    public ResponseEntity<ExchangeMessageResponse> sendMessage(
        @PathVariable UUID id,
        @Valid @RequestBody ExchangeMessageCreateRequest req,
        HttpServletRequest request
    ) {
        UUID externalUserId = extractExternalUserId(request);
        String tenantId = resolveTenantId(request);
        ExchangeMessageResponse response = exchangeMessageService.sendMessage(
            id, externalUserId.toString(), SenderType.EXTERNAL, req.content(), tenantId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retourne les messages d'une demande d'échange.
     *
     * @param id      UUID de la demande
     * @param request requête HTTP (pour extraire le tenantId)
     * @return liste des messages
     */
    @GetMapping("/requests/{id}/messages")
    @Operation(summary = "Lire les messages d'une demande")
    public ResponseEntity<List<ExchangeMessageResponse>> getMessages(@PathVariable UUID id, HttpServletRequest request) {
        String tenantId = resolveTenantId(request);
        return ResponseEntity.ok(exchangeMessageService.getByRequest(id, tenantId));
    }

    // =========================================================================
    // Endpoints INTERNES (JWT interne)
    // =========================================================================

    /**
     * Retourne toutes les demandes d'un tenant (vue interne).
     *
     * @return liste de toutes les demandes du tenant
     */
    @GetMapping("/internal/requests")
    @PreAuthorize("hasAnyRole('CTC_MANAGER','SUPER_ADMIN','CE_MEMBER')")
    @Operation(summary = "Toutes les demandes d'échange (interne)")
    public ResponseEntity<List<ExchangeRequestResponse>> getAllRequests() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(exchangeRequestService.getAllForTenant(tenantId));
    }

    /**
     * Change le statut d'une demande d'échange (interne).
     *
     * @param id     UUID de la demande
     * @param body   map contenant la clé "status"
     * @return demande mise à jour
     */
    @PatchMapping("/internal/requests/{id}/status")
    @PreAuthorize("hasAnyRole('CTC_MANAGER','SUPER_ADMIN','CE_MEMBER')")
    @Operation(summary = "Changer le statut d'une demande (interne)")
    public ResponseEntity<ExchangeRequestResponse> updateStatus(
        @PathVariable UUID id,
        @RequestBody Map<String, String> body
    ) {
        String tenantId = TenantContext.getTenantId();
        ExchangeStatus status = ExchangeStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(exchangeRequestService.updateStatus(id, status, tenantId));
    }

    /**
     * Lie une demande d'échange à une étude interne.
     *
     * @param id   UUID de la demande
     * @param body map contenant la clé "studyId"
     * @return demande mise à jour
     */
    @PatchMapping("/internal/requests/{id}/link-study")
    @PreAuthorize("hasAnyRole('CTC_MANAGER','SUPER_ADMIN','CE_MEMBER')")
    @Operation(summary = "Lier une demande à une étude interne")
    public ResponseEntity<ExchangeRequestResponse> linkStudy(
        @PathVariable UUID id,
        @RequestBody Map<String, String> body
    ) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(exchangeRequestService.linkStudy(id, body.get("studyId"), tenantId));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Extrait l'UUID de l'utilisateur externe depuis le JWT externe de la requête.
     *
     * @param request requête HTTP
     * @return UUID de l'utilisateur externe
     */
    private UUID extractExternalUserId(HttpServletRequest request) {
        String token = extractToken(request);
        return exchangeJwtService.extractExternalUserId(token);
    }

    /**
     * Résout le tenantId depuis le {@link TenantContext} ou le header X-Tenant-ID.
     *
     * @param request requête HTTP
     * @return identifiant du tenant
     */
    private String resolveTenantId(HttpServletRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader("X-Tenant-ID");
        }
        return tenantId;
    }

    /**
     * Extrait le token JWT Bearer de la requête.
     *
     * @param request requête HTTP
     * @return token JWT sans le préfixe "Bearer "
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return "";
    }
}
