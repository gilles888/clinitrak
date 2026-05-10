package be.clinitrak.study.controller;

import be.clinitrak.study.config.SecurityConfig;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.StudyType;
import be.clinitrak.study.domain.enums.SponsorType;
import be.clinitrak.study.dto.*;
import be.clinitrak.study.exception.GlobalExceptionHandler;
import be.clinitrak.study.exception.StudyNotFoundException;
import be.clinitrak.study.security.JwtAuthenticationFilter;
import be.clinitrak.study.security.JwtService;
import be.clinitrak.study.service.StudyService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la couche contrôleur pour {@link StudyController}.
 *
 * <p>Utilise @WebMvcTest pour tester uniquement la couche MVC.
 * Le {@link StudyService} est mocké via @MockBean.
 */
@WebMvcTest(StudyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("StudyController — Tests MVC")
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudyService studyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private ObjectMapper objectMapper;
    private static final UUID STUDY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/v1/studies — devrait retourner 200 avec la liste paginée")
    @WithMockUser(roles = "CTC_PM")
    void searchStudies_shouldReturn200WithPagedResults() throws Exception {
        // given
        StudySummaryResponse summary = buildMockSummary();
        Page<StudySummaryResponse> page = new PageImpl<>(List.of(summary));
        when(studyService.searchStudies(any(StudySearchCriteria.class), any(Pageable.class)))
            .thenReturn(page);

        // when / then
        mockMvc.perform(get("/api/v1/studies")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].studyNumber").value("ST-2026-00001"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/studies — devrait retourner 201 avec l'étude créée")
    @WithMockUser(roles = "CTC_PM")
    void createStudy_shouldReturn201WithCreatedStudy() throws Exception {
        // given
        StudyCreateRequest request = new StudyCreateRequest(
            "Étude de test",
            "TEST",
            StudyType.INTERVENTIONAL,
            SponsorType.ACADEMIC,
            "CUSL",
            null, null, null, null, null, 50, false, null, null, null, null
        );
        StudyResponse response = buildMockResponse();
        when(studyService.createStudy(any(StudyCreateRequest.class))).thenReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/studies")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(STUDY_ID.toString()))
            .andExpect(jsonPath("$.studyNumber").value("ST-2026-00001"));
    }

    @Test
    @DisplayName("GET /api/v1/studies/{id} — devrait retourner 404 si étude introuvable")
    @WithMockUser(roles = "CTC_PM")
    void getStudy_shouldReturn404_whenNotFound() throws Exception {
        // given
        when(studyService.getStudy(STUDY_ID))
            .thenThrow(new StudyNotFoundException(STUDY_ID));

        // when / then
        mockMvc.perform(get("/api/v1/studies/{id}", STUDY_ID)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Étude clinique introuvable"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/v1/studies/{id}/status — devrait retourner 200 avec le nouveau statut")
    @WithMockUser(roles = "CTC_PM")
    void updateStatus_shouldReturn200_whenStatusUpdated() throws Exception {
        // given
        StatusUpdateRequest request = new StatusUpdateRequest(
            StudyStatus.APPROVED,
            java.time.LocalDate.of(2026, 5, 9),
            "Approbation CE reçue"
        );
        StudyResponse response = buildMockResponse();
        when(studyService.updateStatus(eq(STUDY_ID), any(StatusUpdateRequest.class))).thenReturn(response);

        // when / then
        mockMvc.perform(patch("/api/v1/studies/{id}/status", STUDY_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(STUDY_ID.toString()));
    }

    @Test
    @DisplayName("DELETE /api/v1/studies/{id} — devrait retourner 403 sans rôle admin")
    @WithMockUser(roles = "CTC_CRA")
    void deleteStudy_shouldReturn403_withoutAdminRole() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/v1/studies/{id}", STUDY_ID)
                .with(csrf()))
            .andExpect(status().isForbidden());

        verify(studyService, never()).deleteStudy(any());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private StudySummaryResponse buildMockSummary() {
        return new StudySummaryResponse(
            STUDY_ID,
            "ST-2026-00001",
            "Étude de test",
            "TEST",
            StudyType.INTERVENTIONAL,
            StudyStatus.DRAFT,
            "Brouillon",
            SponsorType.ACADEMIC,
            "CUSL",
            null,
            null,
            null,
            null,
            50,
            0,
            false
        );
    }

    private StudyResponse buildMockResponse() {
        return new StudyResponse(
            STUDY_ID,
            UUID.randomUUID(),
            "ST-2026-00001",
            null, null, null,
            "Étude de test",
            "TEST",
            StudyType.INTERVENTIONAL,
            "Interventionnel",
            SponsorType.ACADEMIC,
            "Académique",
            "CUSL",
            null,
            null,
            null,
            null,
            StudyStatus.DRAFT,
            "Brouillon",
            null, null, null,
            50,
            0,
            false,
            null,
            Instant.now(),
            Instant.now(),
            "test@cusl.be"
        );
    }
}
