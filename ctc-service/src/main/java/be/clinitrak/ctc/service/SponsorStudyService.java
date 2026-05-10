package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.SponsorCUSLStudy;
import be.clinitrak.ctc.domain.repository.SponsorCUSLStudyRepository;
import be.clinitrak.ctc.dto.SponsorStudyCreateRequest;
import be.clinitrak.ctc.dto.SponsorStudyResponse;
import be.clinitrak.ctc.exception.CtcException;
import be.clinitrak.ctc.mapper.CtcMapper;
import be.clinitrak.ctc.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des études sponsor CUSL.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link CtcException} est levée immédiatement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SponsorStudyService {

    private final SponsorCUSLStudyRepository repository;
    private final CtcMapper mapper;

    /**
     * Retourne toutes les études sponsor actives du tenant courant.
     *
     * @param tenantId identifiant du tenant courant
     * @return liste des réponses d'études sponsor
     */
    public List<SponsorStudyResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle entrée d'étude sponsor CUSL.
     *
     * <p>Le budget dépensé est initialisé à zéro et le statut réglementaire
     * à IN_PREPARATION par défaut.
     *
     * @param request  DTO de création de l'étude sponsor
     * @param tenantId identifiant du tenant courant
     * @return réponse complète de l'étude sponsor créée
     */
    @Transactional
    public SponsorStudyResponse create(SponsorStudyCreateRequest request, String tenantId) {
        SponsorCUSLStudy entity = new SponsorCUSLStudy();
        entity.setStudyId(request.studyId());
        entity.setProjectManagerId(request.projectManagerId());
        entity.setBudgetTotal(request.budgetTotal());
        entity.setBudgetSpent(BigDecimal.ZERO);
        entity.setTenantId(tenantId);

        SponsorCUSLStudy saved = repository.save(entity);
        log.info("Étude sponsor créée : {} pour studyId {} (tenant: {})",
            saved.getId(), saved.getStudyId(), tenantId);
        return mapper.toResponse(saved);
    }
}
