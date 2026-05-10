package be.clinitrak.ctc.controller;

import be.clinitrak.ctc.config.SecurityConfig;
import be.clinitrak.ctc.domain.enums.DeskType;
import be.clinitrak.ctc.domain.enums.Priority;
import be.clinitrak.ctc.domain.enums.RequestStatus;
import be.clinitrak.ctc.domain.enums.RequestType;
import be.clinitrak.ctc.dto.*;
import be.clinitrak.ctc.exception.GlobalExceptionHandler;
import be.clinitrak.ctc.security.JwtAuthenticationFilter;
import be.clinitrak.ctc.security.JwtService;
import be.clinitrak.ctc.service.TrialDeskRequestService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la couche contrôleur pour les APIs desk CTC.
 *
 * <p>Utilise @WebMvcTest pour tester uniquement la couche MVC.
 * Les services sont mockés via @MockBean.
 */
@WebMvcTest(TrialDeskRequestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TrialDeskRequestController — Tests MVC")
class TrialDeskRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrialDeskRequestService service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private ObjectMapper objectMapper;

    private static final UUID REQUEST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String STUDY_ID  = "660e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/ctc/desk-requests — devrait retourner 201 avec la demande créée")
    @WithMockUser(roles = "CTC_MANAGER")
    void createRequest_shouldReturn201() throws Exception {
        // given
        TrialDeskRequestCreateRequest request = new TrialDeskRequestCreateRequest(
            STUDY_ID, DeskType.ACADEMIC, "Dr. Martin", "martin@hospital.be",
            null, RequestType.NEW_STUDY, Priority.MEDIUM, null, null
        );
        TrialDeskRequestResponse response = buildMockResponse();
        when(service.create(any(TrialDeskRequestCreateRequest.class), any())).thenReturn(response);

        // when / then
        mockMvc.perform(post("/api/v1/ctc/desk-requests")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/ctc/desk-requests — devrait retourner 200 avec la liste")
    @WithMockUser(roles = "CTC_MANAGER")
    void getAll_shouldReturn200() throws Exception {
        // given
        when(service.getAll(any())).thenReturn(List.of(buildMockResponse()));

        // when / then
        mockMvc.perform(get("/api/v1/ctc/desk-requests")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(REQUEST_ID.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/ctc/desk-requests/{id}/assign — devrait retourner 200")
    @WithMockUser(roles = "CTC_MANAGER")
    void assign_shouldReturn200() throws Exception {
        // given
        AssignRequest assignRequest = new AssignRequest("user-789", Priority.HIGH, LocalDate.now().plusWeeks(1));
        TrialDeskRequestResponse response = new TrialDeskRequestResponse(
            REQUEST_ID, STUDY_ID, DeskType.ACADEMIC, "Académique",
            LocalDate.now(), "Dr. Martin", null, null,
            RequestType.NEW_STUDY, "Nouvelle étude",
            RequestStatus.ASSIGNED, "Assignée", "user-789",
            Priority.HIGH, "Haute", LocalDate.now().plusWeeks(1), null, LocalDateTime.now()
        );

        when(service.assign(eq(REQUEST_ID), any(AssignRequest.class), any())).thenReturn(response);

        // when / then
        mockMvc.perform(patch("/api/v1/ctc/desk-requests/{id}/assign", REQUEST_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ASSIGNED"))
            .andExpect(jsonPath("$.assignedTo").value("user-789"));
    }

    @Test
    @DisplayName("PATCH /api/v1/ctc/desk-requests/{id}/status — devrait retourner 200")
    @WithMockUser(roles = "CTC_MANAGER")
    void updateStatus_shouldReturn200() throws Exception {
        // given
        StatusUpdateRequest statusRequest = new StatusUpdateRequest(RequestStatus.IN_PROGRESS, "Traitement démarré");
        TrialDeskRequestResponse response = new TrialDeskRequestResponse(
            REQUEST_ID, STUDY_ID, DeskType.ACADEMIC, "Académique",
            LocalDate.now(), "Dr. Martin", null, null,
            RequestType.NEW_STUDY, "Nouvelle étude",
            RequestStatus.IN_PROGRESS, "En cours", null,
            Priority.MEDIUM, "Moyenne", null, "Traitement démarré", LocalDateTime.now()
        );

        when(service.updateStatus(eq(REQUEST_ID), any(StatusUpdateRequest.class), any())).thenReturn(response);

        // when / then
        mockMvc.perform(patch("/api/v1/ctc/desk-requests/{id}/status", REQUEST_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private TrialDeskRequestResponse buildMockResponse() {
        return new TrialDeskRequestResponse(
            REQUEST_ID,
            STUDY_ID,
            DeskType.ACADEMIC,
            "Académique",
            LocalDate.now(),
            "Dr. Martin",
            "martin@hospital.be",
            null,
            RequestType.NEW_STUDY,
            "Nouvelle étude",
            RequestStatus.PENDING,
            "En attente",
            null,
            Priority.MEDIUM,
            "Moyenne",
            null,
            null,
            LocalDateTime.now()
        );
    }
}
