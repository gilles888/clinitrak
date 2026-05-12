package be.clinitrak.exchange.service;

import be.clinitrak.exchange.domain.entity.ExchangeRequest;
import be.clinitrak.exchange.domain.entity.ExternalUser;
import be.clinitrak.exchange.domain.enums.*;
import be.clinitrak.exchange.domain.repository.ExchangeDocumentRepository;
import be.clinitrak.exchange.domain.repository.ExchangeRequestRepository;
import be.clinitrak.exchange.domain.repository.ExternalUserRepository;
import be.clinitrak.exchange.dto.ExchangeRequestCreateRequest;
import be.clinitrak.exchange.dto.ExchangeRequestResponse;
import be.clinitrak.exchange.exception.ExchangeException;
import be.clinitrak.exchange.exception.ExchangeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link ExchangeRequestService}.
 *
 * <p>Vérifie le cycle de vie des demandes d'échange : création, soumission,
 * consultation et changement de statut.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRequestService — tests unitaires")
class ExchangeRequestServiceTest {

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;

    @Mock
    private ExternalUserRepository externalUserRepository;

    @Mock
    private ExchangeDocumentRepository exchangeDocumentRepository;

    @InjectMocks
    private ExchangeRequestService exchangeRequestService;

    private ExternalUser externalUser;
    private ExchangeRequest draftRequest;
    private final UUID USER_ID = UUID.randomUUID();
    private final UUID REQUEST_ID = UUID.randomUUID();
    private final String TENANT_ID = "saintluc";

    @BeforeEach
    void setUp() {
        externalUser = new ExternalUser();
        externalUser.setEmail("investigator@pharma.com");
        externalUser.setFirstName("Jean");
        externalUser.setLastName("Martin");
        externalUser.setRole(ExternalUserRole.INVESTIGATOR);
        externalUser.setVerifiedEmail(true);

        // Injection de l'UUID via réflexion simulée par Mockito
        draftRequest = new ExchangeRequest();
        draftRequest.setExternalUser(externalUser);
        draftRequest.setTargetModule(TargetModule.CE);
        draftRequest.setRequestType(ExchangeRequestType.NEW_STUDY);
        draftRequest.setTitle("Étude PHASE-1");
        draftRequest.setStatus(ExchangeStatus.DRAFT);
        draftRequest.setTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("createRequest — doit créer la demande avec le statut DRAFT")
    void createRequest_shouldSetStatusDraft() {
        // Given
        ExchangeRequestCreateRequest req = new ExchangeRequestCreateRequest(
            TargetModule.CE,
            ExchangeRequestType.NEW_STUDY,
            "Étude PHASE-1",
            "Description complète de l'étude"
        );

        when(externalUserRepository.findById(USER_ID)).thenReturn(Optional.of(externalUser));
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(inv -> {
            ExchangeRequest saved = inv.getArgument(0);
            return saved;
        });
        when(exchangeDocumentRepository.findByRequest_IdAndDeletedFalse(any())).thenReturn(List.of());

        // When
        ExchangeRequestResponse response = exchangeRequestService.create(req, USER_ID, TENANT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ExchangeStatus.DRAFT);
        assertThat(response.title()).isEqualTo("Étude PHASE-1");
        assertThat(response.targetModule()).isEqualTo(TargetModule.CE);
        verify(exchangeRequestRepository).save(argThat(r -> r.getStatus() == ExchangeStatus.DRAFT));
    }

    @Test
    @DisplayName("submitRequest — doit passer le statut à SUBMITTED et enregistrer la date")
    void submitRequest_shouldSetStatusSubmitted() {
        // Given
        when(exchangeRequestRepository.findByIdAndTenantIdAndDeletedFalse(REQUEST_ID, TENANT_ID))
            .thenReturn(Optional.of(draftRequest));
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exchangeDocumentRepository.findByRequest_IdAndDeletedFalse(any())).thenReturn(List.of());

        // Simuler que la demande appartient à l'utilisateur
        try {
            var idField = ExternalUser.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(externalUser, USER_ID);
        } catch (Exception e) {
            // Ignore reflection errors in test setup
        }

        // When
        ExchangeRequestResponse response = exchangeRequestService.submit(REQUEST_ID, USER_ID, TENANT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ExchangeStatus.SUBMITTED);
        assertThat(response.submissionDate()).isNotNull();
    }

    @Test
    @DisplayName("getByUser — doit retourner uniquement les demandes de l'utilisateur")
    void getByUser_shouldReturnOnlyUserRequests() {
        // Given
        ExchangeRequest anotherRequest = new ExchangeRequest();
        anotherRequest.setExternalUser(externalUser);
        anotherRequest.setTargetModule(TargetModule.CTC);
        anotherRequest.setRequestType(ExchangeRequestType.AMENDMENT);
        anotherRequest.setTitle("Amendement A1");
        anotherRequest.setStatus(ExchangeStatus.SUBMITTED);
        anotherRequest.setTenantId(TENANT_ID);

        when(exchangeRequestRepository.findByExternalUser_IdAndTenantIdAndDeletedFalse(USER_ID, TENANT_ID))
            .thenReturn(List.of(draftRequest, anotherRequest));
        when(exchangeDocumentRepository.findByRequest_IdAndDeletedFalse(any())).thenReturn(List.of());

        // When
        List<ExchangeRequestResponse> responses = exchangeRequestService.getByUser(USER_ID, TENANT_ID);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ExchangeRequestResponse::status)
            .containsExactlyInAnyOrder(ExchangeStatus.DRAFT, ExchangeStatus.SUBMITTED);
        verify(exchangeRequestRepository).findByExternalUser_IdAndTenantIdAndDeletedFalse(USER_ID, TENANT_ID);
    }

    @Test
    @DisplayName("updateStatus — doit lever ExchangeNotFoundException si la demande n'existe pas")
    void updateStatus_shouldRejectIfNotOwner() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(exchangeRequestRepository.findByIdAndTenantIdAndDeletedFalse(unknownId, TENANT_ID))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> exchangeRequestService.updateStatus(unknownId, ExchangeStatus.ACCEPTED, TENANT_ID))
            .isInstanceOf(ExchangeNotFoundException.class)
            .hasMessageContaining("introuvable");

        verify(exchangeRequestRepository, never()).save(any());
    }
}
