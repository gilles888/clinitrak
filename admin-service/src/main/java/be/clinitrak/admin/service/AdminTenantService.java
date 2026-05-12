package be.clinitrak.admin.service;

import be.clinitrak.admin.domain.entity.AdminTenant;
import be.clinitrak.admin.domain.entity.SystemAuditLog;
import be.clinitrak.admin.domain.enums.ModuleType;
import be.clinitrak.admin.domain.enums.SubscriptionType;
import be.clinitrak.admin.domain.enums.TenantStatus;
import be.clinitrak.admin.domain.repository.AdminTenantRepository;
import be.clinitrak.admin.domain.repository.SystemAuditLogRepository;
import be.clinitrak.admin.dto.TenantConfigUpdateRequest;
import be.clinitrak.admin.dto.TenantConfiguration;
import be.clinitrak.admin.dto.TenantCreateRequest;
import be.clinitrak.admin.dto.TenantResponse;
import be.clinitrak.admin.dto.TenantStatisticsResponse;
import be.clinitrak.admin.dto.UserInviteRequest;
import be.clinitrak.admin.dto.UserInviteResponse;
import be.clinitrak.admin.exception.AdminException;
import be.clinitrak.admin.exception.AdminNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des tenants dans le admin-service.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>CRUD des tenants avec validation de l'unicité du slug</li>
 *   <li>Mise à jour de la configuration JSONB</li>
 *   <li>Invitation d'utilisateurs (délégation simulée vers l'auth-service)</li>
 *   <li>Calcul de statistiques d'utilisation par tenant</li>
 * </ul>
 *
 * <p>Toutes les opérations d'écriture sont tracées dans {@link SystemAuditLog}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTenantService {

    private final AdminTenantRepository tenantRepository;
    private final SystemAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crée un nouveau tenant dans la plateforme.
     *
     * <p>Valide l'unicité du slug avant la création.
     * Active les modules par défaut si aucun n'est spécifié.
     *
     * @param request DTO de création du tenant
     * @return représentation du tenant créé
     * @throws AdminException si le slug est déjà utilisé
     */
    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request) {
        if (tenantRepository.existsBySlugAndDeletedFalse(request.slug())) {
            throw new AdminException("Le slug '" + request.slug() + "' est déjà utilisé par un autre tenant.");
        }

        AdminTenant tenant = new AdminTenant();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setDomain(request.domain());
        tenant.setSubscriptionType(request.subscriptionType() != null
            ? request.subscriptionType()
            : SubscriptionType.BASIC);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedAt(LocalDateTime.now());

        Set<ModuleType> modules = (request.activeModules() != null && !request.activeModules().isEmpty())
            ? request.activeModules()
            : EnumSet.of(ModuleType.STUDIES);
        tenant.setActiveModules(modules);

        tenant = tenantRepository.save(tenant);

        logAudit("CREATE_TENANT", "AdminTenant", tenant.getId().toString(), null, toJson(tenant));

        log.info("Tenant créé : slug={}, id={}", tenant.getSlug(), tenant.getId());
        return toResponse(tenant);
    }

    /**
     * Retourne la liste de tous les tenants non supprimés.
     *
     * @return liste des tenants actifs ou inactifs
     */
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAllByDeletedFalse().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Met à jour la configuration JSONB d'un tenant existant.
     *
     * @param tenantId UUID du tenant à mettre à jour
     * @param request  DTO contenant les nouveaux paramètres de configuration
     * @return représentation mise à jour du tenant
     * @throws AdminNotFoundException si le tenant n'existe pas
     */
    @Transactional
    public TenantResponse updateConfiguration(UUID tenantId, TenantConfigUpdateRequest request) {
        AdminTenant tenant = findTenantOrThrow(tenantId);
        String oldConfig = tenant.getConfiguration();

        TenantConfiguration config = new TenantConfiguration(
            request.ceNumberFormat() != null ? request.ceNumberFormat() : "YYYY/NNNN",
            request.timezone() != null ? request.timezone() : "Europe/Brussels",
            request.defaultLanguage() != null ? request.defaultLanguage() : "fr",
            request.maxUsers() != null ? request.maxUsers() : 50,
            request.storageQuotaGb() != null ? request.storageQuotaGb() : 10,
            Map.of()
        );

        tenant.setConfiguration(toJson(config));
        tenant = tenantRepository.save(tenant);

        logAudit("UPDATE_CONFIG", "AdminTenant", tenantId.toString(), oldConfig, tenant.getConfiguration());

        log.info("Configuration mise à jour pour le tenant : id={}", tenantId);
        return toResponse(tenant);
    }

    /**
     * Simule l'invitation d'un utilisateur dans un tenant.
     *
     * <p>Dans l'architecture finale, cette méthode délègue à l'auth-service
     * via Feign pour créer le compte et envoyer le mail d'invitation.
     * Ici, l'invitation est simulée et tracée dans le journal d'audit.
     *
     * @param tenantId UUID du tenant destinataire
     * @param request  DTO contenant les informations de l'invité
     * @return réponse d'invitation avec le statut
     * @throws AdminNotFoundException si le tenant n'existe pas
     */
    @Transactional
    public UserInviteResponse inviteUser(UUID tenantId, UserInviteRequest request) {
        AdminTenant tenant = findTenantOrThrow(tenantId);

        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            throw new AdminException("Impossible d'inviter un utilisateur dans un tenant suspendu.");
        }

        String inviteDetails = String.format(
            "{\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"role\":\"%s\",\"tenantSlug\":\"%s\"}",
            request.email(), request.firstName(), request.lastName(), request.role(), tenant.getSlug()
        );
        logAudit("INVITE_USER", "User", request.email(), null, inviteDetails);

        log.info("Invitation simulée pour {} dans le tenant {}", request.email(), tenant.getSlug());

        return new UserInviteResponse(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.role(),
            "INVITATION_SENT"
        );
    }

    /**
     * Retourne les statistiques d'utilisation d'un tenant.
     *
     * <p>Les valeurs actuelles sont des métriques simulées. Dans l'architecture
     * finale, elles seront agrégées depuis les services étude, pharmacy, etc.
     *
     * @param tenantId UUID du tenant
     * @return statistiques d'utilisation du tenant
     * @throws AdminNotFoundException si le tenant n'existe pas
     */
    public TenantStatisticsResponse getTenantStatistics(UUID tenantId) {
        AdminTenant tenant = findTenantOrThrow(tenantId);

        Map<String, Long> moduleUsage = tenant.getActiveModules().stream()
            .collect(Collectors.toMap(
                module -> module.name().toLowerCase(),
                module -> simulateModuleCount(module)
            ));

        return new TenantStatisticsResponse(
            tenant.getId().toString(),
            tenant.getName(),
            simulateUserCount(tenant),
            simulateStudyCount(tenant),
            simulateActiveStudyCount(tenant),
            moduleUsage
        );
    }

    // ===== Méthodes privées =====

    /**
     * Recherche un tenant par UUID et lève une exception s'il n'existe pas.
     *
     * @param tenantId UUID du tenant recherché
     * @return tenant trouvé
     * @throws AdminNotFoundException si non trouvé ou supprimé
     */
    private AdminTenant findTenantOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
            .filter(t -> !t.isDeleted())
            .orElseThrow(() -> new AdminNotFoundException(
                "Tenant introuvable avec l'identifiant : " + tenantId
            ));
    }

    /**
     * Convertit une entité {@link AdminTenant} en DTO {@link TenantResponse}.
     *
     * @param tenant entité à convertir
     * @return DTO de réponse
     */
    private TenantResponse toResponse(AdminTenant tenant) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getDomain(),
            tenant.getLogoUrl(),
            tenant.getActiveModules(),
            tenant.getConfiguration(),
            tenant.getSubscriptionType(),
            tenant.getSubscriptionType().getLabel(),
            tenant.getStatus(),
            tenant.getStatus().getLabel(),
            tenant.getCreatedAt()
        );
    }

    /**
     * Trace une action administrative dans le journal d'audit système.
     *
     * @param action     code de l'action (ex: CREATE_TENANT)
     * @param entityType type de l'entité concernée
     * @param entityId   identifiant de l'entité
     * @param oldValue   valeur avant modification (null si création)
     * @param newValue   valeur après modification
     */
    private void logAudit(String action, String entityType, String entityId,
                          String oldValue, String newValue) {
        try {
            SystemAuditLog log = new SystemAuditLog();
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setTimestamp(LocalDateTime.now());

            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                log.setUserId(auth.getName());
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            AdminTenantService.log.warn("Impossible d'enregistrer le log d'audit : {}", e.getMessage());
        }
    }

    /**
     * Sérialise un objet en JSON, retourne null en cas d'erreur.
     *
     * @param object objet à sérialiser
     * @return chaîne JSON ou null
     */
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Erreur de sérialisation JSON : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Simule un nombre d'utilisateurs pour un tenant (données mockées).
     *
     * @param tenant tenant cible
     * @return nombre simulé d'utilisateurs
     */
    private long simulateUserCount(AdminTenant tenant) {
        return switch (tenant.getSubscriptionType()) {
            case ENTERPRISE -> 42L;
            case PROFESSIONAL -> 15L;
            default -> 5L;
        };
    }

    /**
     * Simule un nombre total d'études pour un tenant (données mockées).
     *
     * @param tenant tenant cible
     * @return nombre simulé d'études
     */
    private long simulateStudyCount(AdminTenant tenant) {
        return tenant.getActiveModules().contains(ModuleType.STUDIES) ? 28L : 0L;
    }

    /**
     * Simule un nombre d'études actives pour un tenant (données mockées).
     *
     * @param tenant tenant cible
     * @return nombre simulé d'études actives
     */
    private long simulateActiveStudyCount(AdminTenant tenant) {
        return tenant.getActiveModules().contains(ModuleType.STUDIES) ? 12L : 0L;
    }

    /**
     * Simule un compteur d'entités pour un module donné (données mockées).
     *
     * @param module module à simuler
     * @return nombre simulé d'entités dans ce module
     */
    private long simulateModuleCount(ModuleType module) {
        return switch (module) {
            case STUDIES -> 28L;
            case ETHICS -> 14L;
            case CTC -> 7L;
            case PHARMACY -> 156L;
            case EXCHANGE -> 23L;
            case BILLING -> 89L;
            case DOCUMENTS -> 312L;
        };
    }
}
