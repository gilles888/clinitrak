package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.Dispensation;
import be.clinitrak.pharmacy.domain.entity.DrugStock;
import be.clinitrak.pharmacy.domain.enums.StockStatus;
import be.clinitrak.pharmacy.domain.repository.DispensationRepository;
import be.clinitrak.pharmacy.domain.repository.DrugStockRepository;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.DispensationCreateRequest;
import be.clinitrak.pharmacy.dto.DispensationResponse;
import be.clinitrak.pharmacy.exception.PharmacyNotFoundException;
import be.clinitrak.pharmacy.mapper.PharmacyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service métier pour la gestion des dispensations de médicaments.
 *
 * <p>Lors de la création d'une dispensation, le stock AVAILABLE du médicament
 * est automatiquement décrémenté. Si la quantité atteint 0, le statut du stock
 * passe à {@link StockStatus#DISPENSED}.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DispensationService {

    private final DispensationRepository dispensationRepo;
    private final DrugStockRepository stockRepo;
    private final InvestigationalDrugRepository drugRepo;
    private final PharmacyMapper mapper;

    /**
     * Retourne toutes les dispensations non supprimées du tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des dispensations actives
     */
    @Transactional(readOnly = true)
    public List<DispensationResponse> getAll(String tenantId) {
        return dispensationRepo.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    /**
     * Retourne l'historique de dispensation d'un patient.
     *
     * @param patientCode code anonymisé du patient
     * @param tenantId    identifiant du tenant
     * @return liste des dispensations du patient
     */
    @Transactional(readOnly = true)
    public List<DispensationResponse> getByPatientCode(String patientCode, String tenantId) {
        return dispensationRepo.findByPatientCodeAndTenantIdAndDeletedFalse(patientCode, tenantId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    /**
     * Crée une dispensation et décrémente le stock disponible correspondant.
     *
     * <p>Recherche le premier stock AVAILABLE pour ce médicament et décrémente sa quantité.
     * Si la quantité atteint 0, le stock est marqué comme DISPENSED.
     *
     * @param req      données de la dispensation
     * @param tenantId identifiant du tenant
     * @return DTO de la dispensation créée
     * @throws PharmacyNotFoundException si le médicament n'existe pas
     */
    public DispensationResponse create(DispensationCreateRequest req, String tenantId) {
        // Vérification que le médicament existe
        drugRepo.findById(req.drugId())
            .filter(d -> d.getTenantId().equals(tenantId) && !d.isDeleted())
            .orElseThrow(() -> new PharmacyNotFoundException("Drug not found: " + req.drugId()));

        // Décrémentation du stock disponible
        List<DrugStock> availableStocks = stockRepo.findByTenantIdAndStatusAndDeletedFalse(tenantId, StockStatus.AVAILABLE)
            .stream()
            .filter(s -> s.getDrug().getId().equals(req.drugId()))
            .toList();

        if (!availableStocks.isEmpty()) {
            DrugStock stock = availableStocks.get(0);
            stock.setQuantity(Math.max(0, stock.getQuantity() - req.quantity()));
            if (stock.getQuantity() == 0) {
                stock.setStatus(StockStatus.DISPENSED);
            }
            stockRepo.save(stock);
            log.info("Décrémentation stock {} : -{} unités (tenant: {})", stock.getId(), req.quantity(), tenantId);
        } else {
            log.warn("Aucun stock AVAILABLE trouvé pour le médicament {} (tenant: {})", req.drugId(), tenantId);
        }

        // Création de la dispensation
        Dispensation d = new Dispensation();
        drugRepo.findById(req.drugId()).ifPresent(d::setDrug);
        d.setStudyId(req.studyId());
        d.setPatientCode(req.patientCode());
        d.setDispensationDate(req.dispensationDate() != null ? req.dispensationDate() : LocalDate.now());
        d.setPharmacistId(req.pharmacistId());
        d.setPrescriberId(req.prescriberId());
        d.setQuantity(req.quantity());
        d.setPrescription(req.prescription());
        d.setVisitNumber(req.visitNumber());
        d.setNotes(req.notes());
        d.setTenantId(tenantId);

        log.info("Dispensation créée pour patient {} (étude: {}, tenant: {})",
            req.patientCode(), req.studyId(), tenantId);
        return mapper.toResponse(dispensationRepo.save(d));
    }
}
