package be.clinitrak.ctc.service;

import be.clinitrak.ctc.domain.entity.FinancialContract;
import be.clinitrak.ctc.domain.enums.ContractStatus;
import be.clinitrak.ctc.domain.enums.Currency;
import be.clinitrak.ctc.domain.repository.FinancialContractRepository;
import be.clinitrak.ctc.dto.FinancialContractCreateRequest;
import be.clinitrak.ctc.dto.FinancialContractResponse;
import be.clinitrak.ctc.exception.CtcException;
import be.clinitrak.ctc.mapper.CtcMapper;
import be.clinitrak.ctc.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des contrats financiers CTC.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link CtcException} est levée immédiatement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialContractService {

    private final FinancialContractRepository repository;
    private final CtcMapper mapper;

    /**
     * Retourne tous les contrats financiers actifs du tenant courant.
     *
     * @param tenantId identifiant du tenant courant
     * @return liste des réponses de contrats financiers
     */
    public List<FinancialContractResponse> getAll(String tenantId) {
        return repository.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée un nouveau contrat financier avec statut DRAFT.
     *
     * <p>Si aucune devise n'est fournie, EUR est utilisée par défaut.
     *
     * @param request  DTO de création du contrat
     * @param tenantId identifiant du tenant courant
     * @return réponse complète du contrat créé
     */
    @Transactional
    public FinancialContractResponse create(FinancialContractCreateRequest request, String tenantId) {
        FinancialContract entity = new FinancialContract();
        entity.setStudyId(request.studyId());
        entity.setContractType(request.contractType());
        entity.setContractDate(request.contractDate());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency() != null ? request.currency() : Currency.EUR);
        entity.setBillingSchedule(request.billingSchedule());
        entity.setPaymentTerms(request.paymentTerms());
        entity.setStatus(ContractStatus.DRAFT);
        entity.setTenantId(tenantId);

        FinancialContract saved = repository.save(entity);
        log.info("Contrat financier créé : {} pour étude {} (tenant: {})",
            saved.getId(), saved.getStudyId(), tenantId);
        return mapper.toResponse(saved);
    }
}
