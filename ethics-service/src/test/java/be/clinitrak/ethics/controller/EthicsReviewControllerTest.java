package be.clinitrak.ethics.controller;

import be.clinitrak.ethics.config.SecurityConfig;
import be.clinitrak.ethics.domain.enums.ReviewDecision;
import be.clinitrak.ethics.domain.enums.ReviewType;
import be.clinitrak.ethics.dto.DecisionUpdateRequest;
import be.clinitrak.ethics.dto.EthicsDashboardResponse;
import be.clinitrak.ethics.dto.EthicsReviewCreateRequest;
import be.clinitrak.ethics.dto.EthicsReviewResponse;
import be.clinitrak.ethics.exception.EthicsNotFoundException;
import be.clinitrak.ethics.exception.GlobalExceptionHandler;
import be.clinitrak.ethics.security.JwtAuthenticationFilter;
import be.clinitrak.ethics.security.JwtService;
import be.clinitrak.ethics.service.EthicsDashboardService;
import be.clinitrak.ethics.service.EthicsReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la couche contrôleur pour les APIs CE.
 *
 * <p>Utilise @WebMvcTest pour tester uniquement la couche MVC.
 * Les services sont mockés via @MockBean.
 */
@WebMvcTest({EthicsReviewController.class, EthicsDashboardController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("EthicsReviewController — Tests MVC")
class EthicsReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EthicsReviewService reviewService;

    @MockBean
    private EthicsDashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private ObjectMapper objectMapper;

    private static final UUID REVIEW_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID STUDY_ID  = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/ethics/reviews — devrait retourner 201 avec l'avis créé")
    @WithMockUser(roles = "CE_SECRETARY")
    void createReview_shouldReturn201_withCreatedReview() throws Exception {
        // given
        EthicsReviewCreateRequest request = new EthicsReviewCreateRequest(
            STUDY_ID, ReviewType.INITIAL, LocalDate.now(), "Dr. Dupont", null
        );
        EthicsReviewResponse response = buildMockResponse();
        when(reviewService.createReview(any(EthicsReviewCreateRequest.class))).thenReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/ethics/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$.ethicsNumber").value("2026/0001"))
            .andExpect(jsonPath("$.decision").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/ethics/reviews — devrait retourner 200 avec la liste paginée")
    @WithMockUser(roles = "CE_SECRETARY")
    void getReviews_shouldReturn200_withPagedResults() throws Exception {
        // given
        EthicsReviewResponse review = buildMockResponse();
        Page<EthicsReviewResponse> page = new PageImpl<>(List.of(review));
        when(reviewService.getReviews(any(Pageable.class))).thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/ethics/reviews")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].ethicsNumber").value("2026/0001"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/ethics/reviews/{id} — devrait retourner 404 si avis introuvable")
    @WithMockUser(roles = "CE_SECRETARY")
    void getReview_shouldReturn404_whenNotFound() throws Exception {
        // given
        when(reviewService.getReview(REVIEW_ID))
            .thenThrow(new EthicsNotFoundException("Avis CE", REVIEW_ID));

        // when / then
        mockMvc.perform(get("/api/v1/ethics/reviews/{id}", REVIEW_ID)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Ressource introuvable"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/ethics/reviews/{id}/decision — devrait retourner 200 avec la décision mise à jour")
    @WithMockUser(roles = "CE_COORDINATOR")
    void updateDecision_shouldReturn200_whenDecisionUpdated() throws Exception {
        // given
        DecisionUpdateRequest request = new DecisionUpdateRequest(
            ReviewDecision.APPROVED,
            LocalDate.now(),
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            "Approuvé sans conditions"
        );
        EthicsReviewResponse response = buildMockResponse();
        when(reviewService.updateDecision(eq(REVIEW_ID), any(DecisionUpdateRequest.class)))
            .thenReturn(response);

        // when / then
        mockMvc.perform(patch("/api/v1/ethics/reviews/{id}/decision", REVIEW_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(REVIEW_ID.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/ethics/reviews/{id}/decision — devrait retourner 403 sans rôle coordinator")
    @WithMockUser(roles = "CE_SECRETARY")
    void updateDecision_shouldReturn403_withoutCoordinatorRole() throws Exception {
        // given
        DecisionUpdateRequest request = new DecisionUpdateRequest(
            ReviewDecision.APPROVED, LocalDate.now(), null, null, null
        );

        // when / then
        mockMvc.perform(patch("/api/v1/ethics/reviews/{id}/decision", REVIEW_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/ethics/dashboard — devrait retourner 200 avec les indicateurs")
    @WithMockUser(roles = "CE_SECRETARY")
    void getDashboard_shouldReturn200_withIndicators() throws Exception {
        // given
        EthicsDashboardResponse dashboard = new EthicsDashboardResponse(
            5L, 3L, 2L, null, 4L, 1L,
            Map.of("PENDING", 5L, "APPROVED", 10L),
            Collections.emptyList()
        );
        when(dashboardService.getDashboard()).thenReturn(dashboard);

        // when / then
        mockMvc.perform(get("/api/v1/ethics/dashboard")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingReviews").value(5))
            .andExpect(jsonPath("$.annualReportsDue").value(4));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private EthicsReviewResponse buildMockResponse() {
        return new EthicsReviewResponse(
            REVIEW_ID,
            UUID.randomUUID(),
            STUDY_ID,
            "2026/0001",
            ReviewType.INITIAL,
            "Initial",
            LocalDate.now(),
            null,
            ReviewDecision.PENDING,
            "En attente",
            null,
            null,
            null,
            "Dr. Dupont",
            false,
            Instant.now(),
            Instant.now()
        );
    }
}
