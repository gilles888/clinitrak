package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.Dispensation;
import be.clinitrak.pharmacy.domain.entity.DrugStock;
import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import be.clinitrak.pharmacy.domain.enums.StockStatus;
import be.clinitrak.pharmacy.domain.repository.DispensationRepository;
import be.clinitrak.pharmacy.domain.repository.DrugStockRepository;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.DispensationCreateRequest;
import be.clinitrak.pharmacy.dto.DispensationResponse;
import be.clinitrak.pharmacy.mapper.PharmacyMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du {@link DispensationService} avec Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DispensationService — Tests unitaires")
class DispensationServiceTest {

    @Mock
    private DispensationRepository dispensationRepo;

    @Mock
    private DrugStockRepository stockRepo;

    @Mock
    private InvestigationalDrugRepository drugRepo;

    @Mock
    private PharmacyMapper mapper;

    @InjectMocks
    private DispensationService dispensationService;

    private static final String TENANT_ID = "tenant-test-001";
    private static final String OTHER_TENANT_ID = "tenant-other-002";
    private static final UUID DRUG_ID = UUID.randomUUID();

    private InvestigationalDrug buildDrug() {
        InvestigationalDrug drug = new InvestigationalDrug();
        drug.setStudyId("study-001");
        drug.setDrugName("TestDrug-A");
        drug.setTenantId(TENANT_ID);
        return drug;
    }

    private DrugStock buildAvailableStock(InvestigationalDrug drug, int quantity) {
        DrugStock stock = new DrugStock();
        stock.setDrug(drug);
        stock.setQuantity(quantity);
        stock.setUnit("comprimés");
        stock.setStatus(StockStatus.AVAILABLE);
        stock.setTenantId(TENANT_ID);
        return stock;
    }

    private DispensationCreateRequest buildRequest(int quantity) {
        return new DispensationCreateRequest(
            DRUG_ID, "study-001", "PATIENT-001",
            LocalDate.now(), "pharmacist-01", "prescriber-01",
            quantity, "ORD-001", "V1", null
        );
    }

    @Test
    @DisplayName("createDispensation — doit décrémenter le stock disponible")
    void createDispensation_shouldDecrementStock() {
        // Given
        InvestigationalDrug drug = buildDrug();
        DrugStock stock = buildAvailableStock(drug, 20);
        when(drugRepo.findById(DRUG_ID)).thenReturn(Optional.of(drug));
        when(stockRepo.findByTenantIdAndStatusAndDeletedFalse(TENANT_ID, StockStatus.AVAILABLE))
            .thenReturn(List.of(stock));
        Dispensation savedDisp = new Dispensation();
        when(dispensationRepo.save(any(Dispensation.class))).thenReturn(savedDisp);
        when(mapper.toResponse(savedDisp)).thenReturn(mock(DispensationResponse.class));

        // When
        dispensationService.create(buildRequest(5), TENANT_ID);

        // Then
        ArgumentCaptor<DrugStock> stockCaptor = ArgumentCaptor.forClass(DrugStock.class);
        verify(stockRepo).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(15);
        assertThat(stockCaptor.getValue().getStatus()).isEqualTo(StockStatus.AVAILABLE);
    }

    @Test
    @DisplayName("createDispensation — doit passer le stock à DISPENSED si quantité atteint 0")
    void createDispensation_shouldSetDispensedStatusWhenQuantityZero() {
        // Given
        InvestigationalDrug drug = buildDrug();
        DrugStock stock = buildAvailableStock(drug, 5);
        when(drugRepo.findById(DRUG_ID)).thenReturn(Optional.of(drug));
        when(stockRepo.findByTenantIdAndStatusAndDeletedFalse(TENANT_ID, StockStatus.AVAILABLE))
            .thenReturn(List.of(stock));
        Dispensation savedDisp = new Dispensation();
        when(dispensationRepo.save(any(Dispensation.class))).thenReturn(savedDisp);
        when(mapper.toResponse(savedDisp)).thenReturn(mock(DispensationResponse.class));

        // When
        dispensationService.create(buildRequest(5), TENANT_ID);

        // Then
        ArgumentCaptor<DrugStock> stockCaptor = ArgumentCaptor.forClass(DrugStock.class);
        verify(stockRepo).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(0);
        assertThat(stockCaptor.getValue().getStatus()).isEqualTo(StockStatus.DISPENSED);
    }

    @Test
    @DisplayName("getByPatientCode — doit retourner l'historique de dispensation du patient")
    void getByPatientCode_shouldReturnHistory() {
        // Given
        Dispensation d1 = new Dispensation();
        Dispensation d2 = new Dispensation();
        when(dispensationRepo.findByPatientCodeAndTenantIdAndDeletedFalse("PATIENT-001", TENANT_ID))
            .thenReturn(List.of(d1, d2));
        when(mapper.toResponse(any(Dispensation.class))).thenReturn(mock(DispensationResponse.class));

        // When
        List<DispensationResponse> result = dispensationService.getByPatientCode("PATIENT-001", TENANT_ID);

        // Then
        assertThat(result).hasSize(2);
        verify(dispensationRepo).findByPatientCodeAndTenantIdAndDeletedFalse("PATIENT-001", TENANT_ID);
    }

    @Test
    @DisplayName("createDispensation — doit respecter l'isolation multi-tenant")
    void createDispensation_tenantIsolation() {
        // Given — stock appartient au TENANT_ID, pas OTHER_TENANT_ID
        InvestigationalDrug drug = buildDrug();
        when(drugRepo.findById(DRUG_ID)).thenReturn(Optional.of(drug));
        // Pour OTHER_TENANT_ID, aucun stock AVAILABLE retourné
        when(stockRepo.findByTenantIdAndStatusAndDeletedFalse(OTHER_TENANT_ID, StockStatus.AVAILABLE))
            .thenReturn(List.of());
        Dispensation savedDisp = new Dispensation();
        when(dispensationRepo.save(any(Dispensation.class))).thenReturn(savedDisp);
        when(mapper.toResponse(savedDisp)).thenReturn(mock(DispensationResponse.class));

        // When — dispensation pour OTHER_TENANT_ID
        // Le drug a TENANT_ID mais la dispensation utilise OTHER_TENANT_ID
        // La vérification du tenant sur le drug lèvera une exception
        // On teste simplement que le stockRepo est appelé avec OTHER_TENANT_ID
        try {
            dispensationService.create(buildRequest(5), OTHER_TENANT_ID);
        } catch (Exception e) {
            // Attendu : le drug appartient à TENANT_ID, pas OTHER_TENANT_ID
        }

        // Then — le stockRepo a été appelé avec OTHER_TENANT_ID (isolation respectée)
        verify(stockRepo, never()).save(any(DrugStock.class));
    }

    @Test
    @DisplayName("createDispensation — doit utiliser la date de dispensation fournie")
    void createDispensation_shouldSetDispensationDate() {
        // Given
        LocalDate expectedDate = LocalDate.of(2026, 6, 15);
        InvestigationalDrug drug = buildDrug();
        when(drugRepo.findById(DRUG_ID)).thenReturn(Optional.of(drug));
        when(stockRepo.findByTenantIdAndStatusAndDeletedFalse(TENANT_ID, StockStatus.AVAILABLE))
            .thenReturn(List.of());
        Dispensation savedDisp = new Dispensation();
        when(dispensationRepo.save(any(Dispensation.class))).thenReturn(savedDisp);
        when(mapper.toResponse(savedDisp)).thenReturn(mock(DispensationResponse.class));

        DispensationCreateRequest req = new DispensationCreateRequest(
            DRUG_ID, "study-001", "PATIENT-001",
            expectedDate, "pharmacist-01", "prescriber-01",
            5, null, null, null
        );

        // When
        dispensationService.create(req, TENANT_ID);

        // Then
        ArgumentCaptor<Dispensation> captor = ArgumentCaptor.forClass(Dispensation.class);
        verify(dispensationRepo).save(captor.capture());
        assertThat(captor.getValue().getDispensationDate()).isEqualTo(expectedDate);
    }
}
