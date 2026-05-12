package be.clinitrak.admin.controller;

import be.clinitrak.admin.domain.enums.ModuleType;
import be.clinitrak.admin.domain.enums.SubscriptionType;
import be.clinitrak.admin.domain.repository.AdminTenantRepository;
import be.clinitrak.admin.dto.TenantCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration pour {@link AdminController}.
 *
 * <p>Utilise TestContainers pour démarrer une vraie base de données PostgreSQL
 * et {@link SpringBootTest} pour charger le contexte Spring complet.
 *
 * <p>Les Liquibase migrations sont exécutées automatiquement au démarrage.
 * Les scénarios testés :
 * <ul>
 *   <li>Création d'un tenant avec succès (201)</li>
 *   <li>Rejet d'un slug dupliqué (400)</li>
 *   <li>Liste des tenants (inclut le tenant de seed Liquibase)</li>
 *   <li>Rejet si rôle insuffisant (403)</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("AdminController — Tests d'intégration TestContainers")
class AdminControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("clinitrak_admin_test")
        .withUsername("clinitrak")
        .withPassword("clinitrak_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Clé JWT de test (HS256 — 256 bits minimum en Base64)
        registry.add("clinitrak.security.jwt.secret",
            () -> "dGVzdC1zZWNyZXQta2V5LWZvci1jbGluaXRyYWstYWRtaW4tc2VydmljZS10ZXN0");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminTenantRepository tenantRepository;

    /** Constante de base URL pour les endpoints admin. */
    private static final String BASE_URL = "/api/v1/admin";

    @BeforeEach
    void setUp() {
        // Supprime les tenants créés par les tests précédents (pas le tenant seed)
        tenantRepository.findAllByDeletedFalse().stream()
            .filter(t -> !t.getSlug().equals("saintluc"))
            .forEach(t -> {
                t.setDeleted(true);
                tenantRepository.save(t);
            });
    }

    // ===== createTenant =====

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("POST /tenants — 201 : crée un nouveau tenant avec succès")
    void createTenant_shouldReturn201_whenValidRequest() throws Exception {
        TenantCreateRequest request = new TenantCreateRequest(
            "Hôpital Erasme",
            "erasme",
            "www.erasme.be",
            Set.of(ModuleType.STUDIES, ModuleType.ETHICS),
            SubscriptionType.PROFESSIONAL
        );

        mockMvc.perform(post(BASE_URL + "/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.name", is("Hôpital Erasme")))
            .andExpect(jsonPath("$.slug", is("erasme")))
            .andExpect(jsonPath("$.domain", is("www.erasme.be")))
            .andExpect(jsonPath("$.status", is("ACTIVE")))
            .andExpect(jsonPath("$.subscriptionType", is("PROFESSIONAL")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("POST /tenants — 400 : rejette un slug déjà utilisé")
    void createTenant_shouldReturn400_whenSlugAlreadyExists() throws Exception {
        // Le slug "saintluc" est créé par le seed Liquibase V1__init_admin.sql
        TenantCreateRequest request = new TenantCreateRequest(
            "Autre établissement",
            "saintluc",
            null,
            null,
            null
        );

        mockMvc.perform(post(BASE_URL + "/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Erreur métier")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("POST /tenants — 400 : rejette un slug avec caractères invalides")
    void createTenant_shouldReturn400_whenSlugInvalid() throws Exception {
        TenantCreateRequest request = new TenantCreateRequest(
            "Test",
            "Slug Invalide!",
            null,
            null,
            null
        );

        mockMvc.perform(post(BASE_URL + "/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Données invalides")));
    }

    @Test
    @WithMockUser(roles = "CTC_CRA")
    @DisplayName("POST /tenants — 403 : accès refusé si rôle insuffisant")
    void createTenant_shouldReturn403_whenInsufficientRole() throws Exception {
        TenantCreateRequest request = new TenantCreateRequest(
            "Test Tenant",
            "test-tenant",
            null,
            null,
            null
        );

        mockMvc.perform(post(BASE_URL + "/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ===== listTenants =====

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("GET /tenants — 200 : retourne au moins le tenant seed")
    void listTenants_shouldReturn200_withAtLeastSeedTenant() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].slug", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("GET /tenants — 200 : retourne le tenant seed saintluc")
    void listTenants_shouldContainSeedTenant() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.slug == 'saintluc')].name",
                hasSize(1)));
    }

    @Test
    @DisplayName("GET /tenants — 401 : non authentifié")
    void listTenants_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tenants"))
            .andExpect(status().isUnauthorized());
    }

    // ===== system/health =====

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("GET /system/health — 200 : retourne le statut UP quand la DB est accessible")
    void getSystemHealth_shouldReturn200_withUpStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/system/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.services.database", is("UP")))
            .andExpect(jsonPath("$.checkedAt", notNullValue()));
    }

    // ===== audit-logs =====

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("GET /audit-logs — 200 : retourne une page vide si aucun log")
    void getAuditLogs_shouldReturn200_withEmptyPage() throws Exception {
        mockMvc.perform(get(BASE_URL + "/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()))
            .andExpect(jsonPath("$.totalElements", notNullValue()));
    }
}
