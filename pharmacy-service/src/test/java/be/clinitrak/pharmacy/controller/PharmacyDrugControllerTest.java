package be.clinitrak.pharmacy.controller;

import be.clinitrak.pharmacy.config.SecurityConfig;
import be.clinitrak.pharmacy.domain.enums.DrugCategory;
import be.clinitrak.pharmacy.domain.enums.DrugForm;
import be.clinitrak.pharmacy.domain.enums.DrugRegulatoryStatus;
import be.clinitrak.pharmacy.dto.*;
import be.clinitrak.pharmacy.exception.GlobalExceptionHandler;
import be.clinitrak.pharmacy.security.JwtAuthenticationFilter;
import be.clinitrak.pharmacy.security.JwtService;
import be.clinitrak.pharmacy.service.*;
import be.clinitrak.pharmacy.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la couche contrôleur pour les APIs pharmacie.
 *
 * <p>Utilise @WebMvcTest pour tester uniquement la couche MVC.
 * Les services sont mockés via @MockBean.
 */
@WebMvcTest({
    PharmacyDrugController.class,
    PharmacyDashboardController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("PharmacyDrugController — Tests MVC")
class PharmacyDrugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DrugService drugService;

    @MockBean
    private PharmacyDashboardService dashboardService;

    @MockBean
    private PharmacyAlertService alertService;

    @MockBean
    private PdfReportService pdfReportService;

    @MockBean
    private EmergencyUnblindingService unblindingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private ObjectMapper objectMapper;

    private static final UUID DRUG_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TENANT_ID = "tenant-test-001";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        TenantContext.setTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("POST /api/v1/pharmacy/drugs — devrait retourner 201 avec le médicament créé")
    @WithMockUser(roles = "PHARMACIST")
    void createDrug_shouldReturn201() throws Exception {
        // Given
        DrugCreateRequest request = new DrugCreateRequest(
            "study-001", "TestDrug-A", "testdrug", "100mg",
            DrugForm.TABLET, "TestPharma", "LOT-001",
            LocalDate.now().plusYears(2), "Conserver à température ambiante",
            DrugCategory.IMP, null
        );
        DrugResponse response = buildDrugResponse();
        when(drugService.create(any(DrugCreateRequest.class), any())).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/pharmacy/drugs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(DRUG_ID.toString()))
            .andExpect(jsonPath("$.drugName").value("TestDrug-A"))
            .andExpect(jsonPath("$.regulatoryStatus").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/pharmacy/drugs — devrait retourner 200 avec la liste des médicaments")
    @WithMockUser(roles = "PHARMACIST")
    void getDrugs_shouldReturn200() throws Exception {
        // Given
        List<DrugResponse> drugs = List.of(buildDrugResponse());
        when(drugService.getAll(any())).thenReturn(drugs);

        // When / Then
        mockMvc.perform(get("/api/v1/pharmacy/drugs")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].drugName").value("TestDrug-A"));
    }

    @Test
    @DisplayName("GET /api/v1/pharmacy/dashboard — devrait retourner 200 avec les indicateurs")
    @WithMockUser(roles = "PHARMACIST")
    void getDashboard_shouldReturn200() throws Exception {
        // Given
        PharmacyDashboardResponse dashboard = new PharmacyDashboardResponse(
            5L, 3L, 1L, 2L, 10L, 0L, List.of()
        );
        when(dashboardService.getDashboard(any())).thenReturn(dashboard);

        // When / Then
        mockMvc.perform(get("/api/v1/pharmacy/dashboard")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalDrugs").value(5))
            .andExpect(jsonPath("$.availableStocks").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/pharmacy/alerts — devrait retourner 200 avec les alertes")
    @WithMockUser(roles = "PHARMACIST")
    void getAlerts_shouldReturn200() throws Exception {
        // Given
        PharmacyAlertsResponse alertsResponse = new PharmacyAlertsResponse(List.of(), 0);
        when(alertService.getAlerts(any())).thenReturn(alertsResponse);

        // When / Then
        mockMvc.perform(get("/api/v1/pharmacy/alerts")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alerts").isArray())
            .andExpect(jsonPath("$.criticalCount").value(0));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private DrugResponse buildDrugResponse() {
        return new DrugResponse(
            DRUG_ID,
            "study-001",
            "TestDrug-A",
            "testdrug",
            "100mg",
            DrugForm.TABLET,
            "Comprimé",
            "TestPharma",
            "LOT-001",
            LocalDate.now().plusYears(2),
            "Conserver à température ambiante",
            DrugCategory.IMP,
            "IMP",
            DrugRegulatoryStatus.PENDING,
            "En attente",
            null
        );
    }
}
