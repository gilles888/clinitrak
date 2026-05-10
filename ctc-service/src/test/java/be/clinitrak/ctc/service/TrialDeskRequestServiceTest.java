package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.TrialDeskRequest;
import be.clinitrak.ctc.domain.enums.DeskType;
import be.clinitrak.ctc.domain.enums.Priority;
import be.clinitrak.ctc.domain.enums.RequestStatus;
import be.clinitrak.ctc.domain.enums.RequestType;
import be.clinitrak.ctc.domain.repository.TrialDeskRequestRepository;
import be.clinitrak.ctc.dto.AssignRequest;
import be.clinitrak.ctc.dto.StatusUpdateRequest;
import be.clinitrak.ctc.dto.TrialDeskRequestCreateRequest;
import be.clinitrak.ctc.dto.TrialDeskRequestResponse;
import be.clinitrak.ctc.mapper.CtcMapper;
import be.clinitrak.ctc.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link TrialDeskRequestService}.
 *
 * <p>Utilise Mockito pour isoler le service de ses dépendances.
 * Le {@link TenantContext} est mocké statiquement pour simuler un tenant résolu.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrialDeskRequestService — Tests unitaires")
class TrialDeskRequestServiceTest {

    @Mock
    private TrialDeskRequestRepository repository;

    @Mock
    private CtcMapper mapper;

    @InjectMocks
    private TrialDeskRequestService service;

    private static final String TENANT_ID = UUID.randomUUID().toString();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final String STUDY_ID = UUID.randomUUID().toString();

    private MockedStatic<TenantContext> mockedTenantContext;

    @BeforeEach
    void setUp() {
        mockedTenantContext = mockStatic(TenantContext.class);
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        mockedTenantContext.close();
    }

    // ----------------------------------------------------------------
    // createRequest
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createRequest — devrait positionner le statut à PENDING")
    void createRequest_shouldSetStatusPending() {
        // given
        TrialDeskRequestCreateRequest request = buildCreateRequest();
        TrialDeskRequest entity = buildEntity();
        TrialDeskRequestResponse expectedResponse = buildResponse(entity);

        when(repository.save(any(TrialDeskRequest.class))).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(expectedResponse);

        // when
        TrialDeskRequestResponse result = service.create(request, TENANT_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RequestStatus.PENDING);
        verify(repository).save(any(TrialDeskRequest.class));
    }

    @Test
    @DisplayName("createRequest — devrait positionner la date de demande à aujourd'hui")
    void createRequest_shouldSetRequestDateToday() {
        // given
        TrialDeskRequestCreateRequest request = buildCreateRequest();
        TrialDeskRequest savedEntity = buildEntity();
        savedEntity.setRequestDate(LocalDate.now());
        TrialDeskRequestResponse expectedResponse = buildResponse(savedEntity);

        when(repository.save(any(TrialDeskRequest.class))).thenAnswer(inv -> {
            TrialDeskRequest arg = inv.getArgument(0);
            assertThat(arg.getRequestDate()).isEqualTo(LocalDate.now());
            return savedEntity;
        });
        when(mapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // when
        service.create(request, TENANT_ID);

        // then
        verify(repository).save(argThat(r -> LocalDate.now().equals(r.getRequestDate())));
    }

    // ----------------------------------------------------------------
    // assignRequest
    // ----------------------------------------------------------------

    @Test
    @DisplayName("assignRequest — devrait mettre à jour l'assigné et passer le statut à ASSIGNED")
    void assignRequest_shouldUpdateAssigneeAndStatus() {
        // given
        TrialDeskRequest entity = buildEntity();
        entity.setId(REQUEST_ID);
        entity.setStatus(RequestStatus.PENDING);

        AssignRequest assignRequest = new AssignRequest("user-456", Priority.HIGH, LocalDate.now().plusWeeks(2));

        TrialDeskRequest savedEntity = buildEntity();
        savedEntity.setId(REQUEST_ID);
        savedEntity.setStatus(RequestStatus.ASSIGNED);
        savedEntity.setAssignedTo("user-456");
        savedEntity.setPriority(Priority.HIGH);

        TrialDeskRequestResponse expectedResponse = buildResponse(savedEntity);
        expectedResponse = new TrialDeskRequestResponse(
            REQUEST_ID, STUDY_ID, DeskType.ACADEMIC, "Académique",
            LocalDate.now(), "Dr. Martin", null, null,
            RequestType.NEW_STUDY, "Nouvelle étude",
            RequestStatus.ASSIGNED, "Assignée", "user-456",
            Priority.HIGH, "Haute", LocalDate.now().plusWeeks(2), null, LocalDateTime.now()
        );

        when(repository.findByTenantIdAndDeletedFalse(TENANT_ID)).thenReturn(List.of(entity));
        when(repository.save(any(TrialDeskRequest.class))).thenReturn(savedEntity);
        when(mapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // when
        TrialDeskRequestResponse result = service.assign(REQUEST_ID, assignRequest, TENANT_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(result.assignedTo()).isEqualTo("user-456");
        verify(repository).save(argThat(r ->
            RequestStatus.ASSIGNED.equals(r.getStatus()) && "user-456".equals(r.getAssignedTo())
        ));
    }

    // ----------------------------------------------------------------
    // getAll
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAll — devrait filtrer par tenant")
    void getAll_shouldFilterByTenant() {
        // given
        TrialDeskRequest entity = buildEntity();
        TrialDeskRequestResponse response = buildResponse(entity);

        when(repository.findByTenantIdAndDeletedFalse(TENANT_ID)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        // when
        List<TrialDeskRequestResponse> results = service.getAll(TENANT_ID);

        // then
        assertThat(results).hasSize(1);
        verify(repository).findByTenantIdAndDeletedFalse(TENANT_ID);
        verify(repository, never()).findByTenantIdAndDeletedFalse("autre-tenant");
    }

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    @Test
    @DisplayName("updateStatus — devrait mettre à jour le statut")
    void updateStatus_shouldUpdateStatus() {
        // given
        TrialDeskRequest entity = buildEntity();
        entity.setId(REQUEST_ID);
        entity.setStatus(RequestStatus.ASSIGNED);

        StatusUpdateRequest statusRequest = new StatusUpdateRequest(RequestStatus.IN_PROGRESS, "En cours de traitement");

        TrialDeskRequest savedEntity = buildEntity();
        savedEntity.setId(REQUEST_ID);
        savedEntity.setStatus(RequestStatus.IN_PROGRESS);

        TrialDeskRequestResponse expectedResponse = buildResponse(savedEntity);
        expectedResponse = new TrialDeskRequestResponse(
            REQUEST_ID, STUDY_ID, DeskType.ACADEMIC, "Académique",
            LocalDate.now(), "Dr. Martin", null, null,
            RequestType.NEW_STUDY, "Nouvelle étude",
            RequestStatus.IN_PROGRESS, "En cours", null,
            Priority.MEDIUM, "Moyenne", null, "En cours de traitement", LocalDateTime.now()
        );

        when(repository.findByTenantIdAndDeletedFalse(TENANT_ID)).thenReturn(List.of(entity));
        when(repository.save(any(TrialDeskRequest.class))).thenReturn(savedEntity);
        when(mapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // when
        TrialDeskRequestResponse result = service.updateStatus(REQUEST_ID, statusRequest, TENANT_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(RequestStatus.IN_PROGRESS);
        verify(repository).save(argThat(r -> RequestStatus.IN_PROGRESS.equals(r.getStatus())));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private TrialDeskRequestCreateRequest buildCreateRequest() {
        return new TrialDeskRequestCreateRequest(
            STUDY_ID, DeskType.ACADEMIC, "Dr. Martin", "martin@hospital.be",
            "Département de recherche", RequestType.NEW_STUDY, Priority.MEDIUM,
            LocalDate.now().plusMonths(1), null
        );
    }

    private TrialDeskRequest buildEntity() {
        TrialDeskRequest entity = new TrialDeskRequest();
        entity.setId(REQUEST_ID);
        entity.setStudyId(STUDY_ID);
        entity.setDeskType(DeskType.ACADEMIC);
        entity.setRequestDate(LocalDate.now());
        entity.setRequestorName("Dr. Martin");
        entity.setRequestType(RequestType.NEW_STUDY);
        entity.setStatus(RequestStatus.PENDING);
        entity.setPriority(Priority.MEDIUM);
        entity.setTenantId(TENANT_ID);
        return entity;
    }

    private TrialDeskRequestResponse buildResponse(TrialDeskRequest entity) {
        return new TrialDeskRequestResponse(
            entity.getId(),
            entity.getStudyId(),
            entity.getDeskType(),
            entity.getDeskType() != null ? entity.getDeskType().getLabel() : null,
            entity.getRequestDate(),
            entity.getRequestorName(),
            entity.getRequestorEmail(),
            entity.getRequestorOrganization(),
            entity.getRequestType(),
            entity.getRequestType() != null ? entity.getRequestType().getLabel() : null,
            entity.getStatus(),
            entity.getStatus() != null ? entity.getStatus().getLabel() : null,
            entity.getAssignedTo(),
            entity.getPriority(),
            entity.getPriority() != null ? entity.getPriority().getLabel() : null,
            entity.getDeadline(),
            entity.getNotes(),
            LocalDateTime.now()
        );
    }
}
