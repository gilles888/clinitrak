package be.clinitrak.admin.controller;

import be.clinitrak.admin.dto.AuditLogResponse;
import be.clinitrak.admin.dto.SystemHealthResponse;
import be.clinitrak.admin.dto.TenantConfigUpdateRequest;
import be.clinitrak.admin.dto.TenantCreateRequest;
import be.clinitrak.admin.dto.TenantResponse;
import be.clinitrak.admin.dto.TenantStatisticsResponse;
import be.clinitrak.admin.dto.UserInviteRequest;
import be.clinitrak.admin.dto.UserInviteResponse;
import be.clinitrak.admin.service.AdminTenantService;
import be.clinitrak.admin.service.SystemAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour l'administration de la plateforme CliniTrak.
 *
 * <p>Tous les endpoints requièrent le rôle {@code ROLE_SUPER_ADMIN}.
 *
 * <ul>
 *   <li>Gestion des tenants (création, liste, configuration)</li>
 *   <li>Invitation des utilisateurs dans un tenant</li>
 *   <li>Journal d'audit système avec filtres</li>
 *   <li>Santé globale de la plateforme</li>
 *   <li>Statistiques d'utilisation par tenant</li>
 * </ul>
 *
 * <p>Préfixe de base : {@code /api/v1/admin}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "API d'administration de la plateforme CliniTrak")
public class AdminController {

    private final AdminTenantService tenantService;
    private final SystemAuditLogService auditLogService;

    // ===== Gestion des tenants =====

    /**
     * Crée un nouveau tenant dans la plateforme.
     *
     * <p>Valide l'unicité du slug et initialise les modules activés.
     *
     * @param request DTO de création du tenant (validé via @Valid)
     * @return 201 Created avec le tenant créé
     */
    @PostMapping("/tenants")
    @Operation(
        summary = "Créer un tenant",
        description = "Crée un nouvel établissement hospitalier dans la plateforme CliniTrak",
        responses = {
            @ApiResponse(responseCode = "201", description = "Tenant créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Slug déjà utilisé ou données invalides"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        log.info("Création d'un nouveau tenant : slug={}", request.slug());
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne la liste de tous les tenants actifs de la plateforme.
     *
     * @return 200 OK avec la liste des tenants
     */
    @GetMapping("/tenants")
    @Operation(
        summary = "Lister les tenants",
        description = "Retourne tous les tenants non supprimés de la plateforme",
        responses = {
            @ApiResponse(responseCode = "200", description = "Liste des tenants"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<List<TenantResponse>> listTenants() {
        return ResponseEntity.ok(tenantService.listTenants());
    }

    /**
     * Met à jour la configuration JSONB d'un tenant.
     *
     * @param id      UUID du tenant à configurer
     * @param request DTO contenant les nouveaux paramètres
     * @return 200 OK avec le tenant mis à jour
     */
    @PutMapping("/tenants/{id}/configuration")
    @Operation(
        summary = "Mettre à jour la configuration d'un tenant",
        description = "Met à jour les paramètres de configuration JSONB d'un tenant existant",
        responses = {
            @ApiResponse(responseCode = "200", description = "Configuration mise à jour"),
            @ApiResponse(responseCode = "404", description = "Tenant introuvable"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<TenantResponse> updateConfiguration(
        @Parameter(description = "UUID du tenant") @PathVariable UUID id,
        @Valid @RequestBody TenantConfigUpdateRequest request
    ) {
        log.info("Mise à jour de la configuration du tenant : id={}", id);
        return ResponseEntity.ok(tenantService.updateConfiguration(id, request));
    }

    /**
     * Invite un utilisateur dans un tenant.
     *
     * <p>Simule l'envoi d'une invitation. Dans l'architecture finale,
     * délègue à l'auth-service pour la création de compte et l'envoi d'email.
     *
     * @param id      UUID du tenant destinataire
     * @param request DTO contenant les informations de l'invité
     * @return 201 Created avec le statut de l'invitation
     */
    @PostMapping("/tenants/{id}/users/invite")
    @Operation(
        summary = "Inviter un utilisateur",
        description = "Invite un utilisateur à rejoindre un tenant spécifique avec un rôle défini",
        responses = {
            @ApiResponse(responseCode = "201", description = "Invitation envoyée"),
            @ApiResponse(responseCode = "400", description = "Tenant suspendu ou données invalides"),
            @ApiResponse(responseCode = "404", description = "Tenant introuvable"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<UserInviteResponse> inviteUser(
        @Parameter(description = "UUID du tenant") @PathVariable UUID id,
        @Valid @RequestBody UserInviteRequest request
    ) {
        log.info("Invitation de {} dans le tenant : id={}", request.email(), id);
        UserInviteResponse response = tenantService.inviteUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne les statistiques d'utilisation d'un tenant.
     *
     * @param id UUID du tenant
     * @return 200 OK avec les statistiques du tenant
     */
    @GetMapping("/tenants/{id}/statistics")
    @Operation(
        summary = "Statistiques d'un tenant",
        description = "Retourne les métriques d'utilisation d'un tenant (utilisateurs, études, etc.)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Statistiques du tenant"),
            @ApiResponse(responseCode = "404", description = "Tenant introuvable"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<TenantStatisticsResponse> getTenantStatistics(
        @Parameter(description = "UUID du tenant") @PathVariable UUID id
    ) {
        return ResponseEntity.ok(tenantService.getTenantStatistics(id));
    }

    // ===== Journal d'audit =====

    /**
     * Retourne les logs d'audit système avec filtres optionnels et pagination.
     *
     * <p>Tous les filtres sont cumulatifs (AND logique).
     *
     * @param tenantId filtre par tenant (optionnel)
     * @param userId   filtre par utilisateur (optionnel)
     * @param action   filtre par action (optionnel, ex: CREATE_TENANT)
     * @param from     date de début (optionnel, format ISO)
     * @param to       date de fin (optionnel, format ISO)
     * @param pageable paramètres de pagination (défaut : 20 par page, tri par timestamp desc)
     * @return 200 OK avec la page de logs
     */
    @GetMapping("/audit-logs")
    @Operation(
        summary = "Journal d'audit système",
        description = "Retourne les logs d'audit avec filtres cumulatifs sur tenant, utilisateur, action et dates",
        responses = {
            @ApiResponse(responseCode = "200", description = "Page de logs d'audit"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
        @Parameter(description = "Filtre par identifiant de tenant")
        @RequestParam(required = false) String tenantId,

        @Parameter(description = "Filtre par identifiant d'utilisateur")
        @RequestParam(required = false) String userId,

        @Parameter(description = "Filtre par action (ex: CREATE_TENANT, INVITE_USER)")
        @RequestParam(required = false) String action,

        @Parameter(description = "Date de début (format ISO : yyyy-MM-dd'T'HH:mm:ss)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

        @Parameter(description = "Date de fin (format ISO : yyyy-MM-dd'T'HH:mm:ss)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

        @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.findAll(tenantId, userId, action, from, to, pageable));
    }

    // ===== Santé système =====

    /**
     * Retourne le rapport de santé global de la plateforme CliniTrak.
     *
     * <p>Vérifie la connectivité à la base de données et retourne les statuts
     * indicatifs de chaque micro-service.
     *
     * @return 200 OK avec le rapport de santé
     */
    @GetMapping("/system/health")
    @Operation(
        summary = "Santé de la plateforme",
        description = "Retourne le statut de santé global et par service de la plateforme",
        responses = {
            @ApiResponse(responseCode = "200", description = "Rapport de santé"),
            @ApiResponse(responseCode = "403", description = "Rôle SUPER_ADMIN requis")
        }
    )
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        return ResponseEntity.ok(auditLogService.getSystemHealth());
    }
}
