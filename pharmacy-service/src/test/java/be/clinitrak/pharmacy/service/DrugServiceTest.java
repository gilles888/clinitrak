package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import be.clinitrak.pharmacy.domain.enums.DrugCategory;
import be.clinitrak.pharmacy.domain.enums.DrugForm;
import be.clinitrak.pharmacy.domain.enums.DrugRegulatoryStatus;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.DrugCreateRequest;
import be.clinitrak.pharmacy.dto.DrugResponse;
import be.clinitrak.pharmacy.mapper.PharmacyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du {@link DrugService} avec Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DrugService — Tests unitaires")
class DrugServiceTest {

    @Mock
    private InvestigationalDrugRepository repository;

    @Mock
    private PharmacyMapper mapper;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private DrugService drugService;

    private static final String TENANT_ID = "tenant-test-001";
    private static final UUID DRUG_ID = UUID.randomUUID();

    private DrugCreateRequest buildRequest(String randomizationCode) {
        return new DrugCreateRequest(
            "study-001",
            "TestDrug-A",
            "testdrug",
            "100mg",
            DrugForm.TABLET,
            "TestPharma",
            "LOT-001",
            LocalDate.now().plusYears(2),
            "Conserver à température ambiante",
            DrugCategory.IMP,
            randomizationCode
        );
    }

    private InvestigationalDrug buildDrug() {
        InvestigationalDrug drug = new InvestigationalDrug();
        drug.setStudyId("study-001");
        drug.setDrugName("TestDrug-A");
        drug.setCategory(DrugCategory.IMP);
        drug.setForm(DrugForm.TABLET);
        drug.setRegulatoryStatus(DrugRegulatoryStatus.PENDING);
        drug.setTenantId(TENANT_ID);
        return drug;
    }

    private DrugResponse buildResponse(InvestigationalDrug drug) {
        return new DrugResponse(
            DRUG_ID, drug.getStudyId(), drug.getDrugName(),
            null, null, drug.getForm(), "Comprimé",
            null, null, null, null,
            drug.getCategory(), "IMP", DrugRegulatoryStatus.PENDING, "En attente",
            null
        );
    }

    @Test
    @DisplayName("createDrug — le statut réglementaire doit être PENDING à la création")
    void createDrug_shouldSetStatusPending() {
        // Given
        DrugCreateRequest req = buildRequest(null);
        InvestigationalDrug saved = buildDrug();
        when(repository.save(any(InvestigationalDrug.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(buildResponse(saved));

        // When
        drugService.create(req, TENANT_ID);

        // Then
        ArgumentCaptor<InvestigationalDrug> captor = ArgumentCaptor.forClass(InvestigationalDrug.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRegulatoryStatus()).isEqualTo(DrugRegulatoryStatus.PENDING);
    }

    @Test
    @DisplayName("createDrug — le code de randomisation doit être chiffré")
    void createDrug_shouldEncryptRandomizationCode() {
        // Given
        DrugCreateRequest req = buildRequest("CODE-RANDOM-42");
        when(encryptionService.encrypt("CODE-RANDOM-42")).thenReturn("ENCRYPTED_CODE");
        InvestigationalDrug saved = buildDrug();
        saved.setRandomizationCodeEncrypted("ENCRYPTED_CODE");
        when(repository.save(any(InvestigationalDrug.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(buildResponse(saved));

        // When
        drugService.create(req, TENANT_ID);

        // Then
        verify(encryptionService).encrypt("CODE-RANDOM-42");
        ArgumentCaptor<InvestigationalDrug> captor = ArgumentCaptor.forClass(InvestigationalDrug.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRandomizationCodeEncrypted()).isEqualTo("ENCRYPTED_CODE");
    }

    @Test
    @DisplayName("getAll — doit filtrer par tenantId")
    void getAll_shouldFilterByTenant() {
        // Given
        InvestigationalDrug drug1 = buildDrug();
        InvestigationalDrug drug2 = buildDrug();
        drug2.setDrugName("AnotherDrug");
        List<InvestigationalDrug> drugs = List.of(drug1, drug2);
        when(repository.findByTenantIdAndDeletedFalse(TENANT_ID)).thenReturn(drugs);
        when(mapper.toResponse(any(InvestigationalDrug.class))).thenReturn(buildResponse(drug1));

        // When
        List<DrugResponse> result = drugService.getAll(TENANT_ID);

        // Then
        verify(repository).findByTenantIdAndDeletedFalse(TENANT_ID);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("createDrug — ne doit pas lever d'exception si randomizationCode est null")
    void createDrug_withNullRandomizationCode_shouldNotThrow() {
        // Given
        DrugCreateRequest req = buildRequest(null);
        InvestigationalDrug saved = buildDrug();
        when(repository.save(any(InvestigationalDrug.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(buildResponse(saved));

        // When / Then — ne doit pas lever d'exception
        drugService.create(req, TENANT_ID);

        verify(encryptionService, never()).encrypt(any());
        ArgumentCaptor<InvestigationalDrug> captor = ArgumentCaptor.forClass(InvestigationalDrug.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRandomizationCodeEncrypted()).isNull();
    }
}
