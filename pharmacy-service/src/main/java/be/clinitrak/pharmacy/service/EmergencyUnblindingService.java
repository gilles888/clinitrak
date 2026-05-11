package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.EmergencyUnblinding;
import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import be.clinitrak.pharmacy.domain.repository.EmergencyUnblindingRepository;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.EmergencyUnblindingRequest;
import be.clinitrak.pharmacy.dto.EmergencyUnblindingResponse;
import be.clinitrak.pharmacy.exception.PharmacyNotFoundException;
import be.clinitrak.pharmacy.mapper.PharmacyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service métier pour les levées d'insu d'urgence.
 *
 * <p>Le traitement réel n'est révélé qu'après approbation explicite par un pharmacien.
 * Le code de randomisation est déchiffré via {@link EncryptionService}.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmergencyUnblindingService {

    private final EmergencyUnblindingRepository repo;
    private final InvestigationalDrugRepository drugRepo;
    private final EncryptionService encryptionService;
    private final PharmacyMapper mapper;

    /**
     * Retourne toutes les levées d'insu non supprimées du tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des levées d'insu actives
     */
    @Transactional(readOnly = true)
    public List<EmergencyUnblindingResponse> getAll(String tenantId) {
        return repo.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    /**
     * Crée une demande de levée d'insu d'urgence.
     *
     * <p>Le traitement n'est PAS révélé à ce stade — il faut une approbation
     * explicite via {@link #approve(UUID, String, String)}.
     *
     * @param req      données de la demande
     * @param tenantId identifiant du tenant
     * @return DTO de la levée d'insu créée (sans traitement)
     */
    public EmergencyUnblindingResponse request(EmergencyUnblindingRequest req, String tenantId) {
        EmergencyUnblinding u = new EmergencyUnblinding();
        u.setStudyId(req.studyId());
        u.setPatientCode(req.patientCode());
        u.setRequestDate(LocalDateTime.now());
        u.setRequestedBy(req.requestedBy());
        u.setReason(req.reason());
        // NE PAS révéler le treatment à ce stade
        u.setTenantId(tenantId);
        log.info("Demande levée d'insu : patient {} (étude: {}, demandeur: {}, tenant: {})",
            req.patientCode(), req.studyId(), req.requestedBy(), tenantId);
        return mapper.toResponse(repo.save(u));
    }

    /**
     * Approuve une levée d'insu et révèle le traitement réel du patient.
     *
     * <p>Le code de randomisation est déchiffré depuis le médicament de l'étude
     * correspondante. L'approbateur est enregistré avec la date et l'heure.
     *
     * @param id         identifiant UUID de la levée d'insu
     * @param approvedBy identifiant du pharmacien approbateur
     * @param tenantId   identifiant du tenant
     * @return DTO avec le traitement révélé
     * @throws PharmacyNotFoundException si la demande n'existe pas
     */
    public EmergencyUnblindingResponse approve(UUID id, String approvedBy, String tenantId) {
        EmergencyUnblinding u = repo.findById(id)
            .filter(e -> e.getTenantId().equals(tenantId) && !e.isDeleted())
            .orElseThrow(() -> new PharmacyNotFoundException("Unblinding not found"));

        // Déchiffrement du code de randomisation depuis les médicaments de l'étude
        List<InvestigationalDrug> drugs = drugRepo.findByStudyIdAndTenantIdAndDeletedFalse(u.getStudyId(), tenantId);
        String treatment = drugs.stream()
            .map(d -> encryptionService.decrypt(d.getRandomizationCodeEncrypted()))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("Code non disponible");

        u.setTreatment(treatment);
        u.setApprovedBy(approvedBy);
        u.setApprovedAt(LocalDateTime.now());
        log.warn("LEVÉE D'INSU APPROUVÉE — patient: {}, étude: {}, approuveur: {}, tenant: {}",
            u.getPatientCode(), u.getStudyId(), approvedBy, tenantId);
        return mapper.toResponse(repo.save(u));
    }
}
