package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import be.clinitrak.pharmacy.domain.enums.DrugRegulatoryStatus;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.DrugCreateRequest;
import be.clinitrak.pharmacy.dto.DrugResponse;
import be.clinitrak.pharmacy.exception.PharmacyNotFoundException;
import be.clinitrak.pharmacy.mapper.PharmacyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la gestion des médicaments expérimentaux.
 *
 * <p>Gère le cycle de vie des médicaments : création, consultation.
 * Le code de randomisation est chiffré avant persistance via {@link EncryptionService}.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DrugService {

    private final InvestigationalDrugRepository repository;
    private final PharmacyMapper mapper;
    private final EncryptionService encryptionService;

    /**
     * Retourne tous les médicaments non supprimés du tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des médicaments actifs
     */
    @Transactional(readOnly = true)
    public List<DrugResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    /**
     * Retourne un médicament par son identifiant.
     *
     * @param id       identifiant UUID du médicament
     * @param tenantId identifiant du tenant
     * @return DTO de réponse
     * @throws PharmacyNotFoundException si le médicament n'existe pas
     */
    @Transactional(readOnly = true)
    public DrugResponse getById(UUID id, String tenantId) {
        InvestigationalDrug drug = repository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId) && !d.isDeleted())
            .orElseThrow(() -> new PharmacyNotFoundException("Drug", id));
        return mapper.toResponse(drug);
    }

    /**
     * Crée un nouveau médicament expérimental.
     *
     * <p>Le statut réglementaire est initialisé à {@link DrugRegulatoryStatus#PENDING}.
     * Le code de randomisation est chiffré si fourni.
     *
     * @param req      données de création
     * @param tenantId identifiant du tenant
     * @return DTO du médicament créé
     */
    public DrugResponse create(DrugCreateRequest req, String tenantId) {
        InvestigationalDrug drug = new InvestigationalDrug();
        drug.setStudyId(req.studyId());
        drug.setDrugName(req.drugName());
        drug.setInn(req.inn());
        drug.setDosage(req.dosage());
        drug.setForm(req.form());
        drug.setManufacturer(req.manufacturer());
        drug.setBatchNumber(req.batchNumber());
        drug.setExpiryDate(req.expiryDate());
        drug.setStorageConditions(req.storageConditions());
        drug.setCategory(req.category());
        drug.setRegulatoryStatus(DrugRegulatoryStatus.PENDING);
        if (req.randomizationCode() != null) {
            drug.setRandomizationCodeEncrypted(encryptionService.encrypt(req.randomizationCode()));
        }
        drug.setTenantId(tenantId);
        log.info("Création médicament : {} pour étude {} (tenant: {})", req.drugName(), req.studyId(), tenantId);
        return mapper.toResponse(repository.save(drug));
    }

    /**
     * Supprime logiquement un médicament (soft-delete).
     *
     * @param id       identifiant UUID du médicament
     * @param tenantId identifiant du tenant
     * @throws PharmacyNotFoundException si le médicament n'existe pas
     */
    public void delete(UUID id, String tenantId) {
        InvestigationalDrug drug = repository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId) && !d.isDeleted())
            .orElseThrow(() -> new PharmacyNotFoundException("Drug", id));
        drug.setDeleted(true);
        repository.save(drug);
        log.info("Soft-delete médicament : {} (tenant: {})", id, tenantId);
    }
}
