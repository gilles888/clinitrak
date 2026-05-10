package be.clinitrak.ethics.service;

import be.clinitrak.ethics.domain.entity.EthicsReview;
import be.clinitrak.ethics.domain.enums.ReviewDecision;
import be.clinitrak.ethics.domain.repository.EthicsReviewRepository;
import be.clinitrak.ethics.dto.DecisionUpdateRequest;
import be.clinitrak.ethics.dto.EthicsReviewCreateRequest;
import be.clinitrak.ethics.dto.EthicsReviewResponse;
import be.clinitrak.ethics.exception.EthicsException;
import be.clinitrak.ethics.exception.EthicsNotFoundException;
import be.clinitrak.ethics.mapper.EthicsMapper;
import be.clinitrak.ethics.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des avis éthiques.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link EthicsException} est levée immédiatement.
 *
 * <p>La numérotation des avis suit le format {@code AAAA/NNNN} (ex: 2026/0042)
 * et est générée via {@link SequenceGeneratorService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EthicsReviewService {

    private final EthicsReviewRepository reviewRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final EthicsMapper ethicsMapper;

    /**
     * Retourne les avis éthiques d'un tenant paginés, triés du plus récent au plus ancien.
     *
     * @param pageable paramètres de pagination et tri
     * @return page de réponses d'avis
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public Page<EthicsReviewResponse> getReviews(Pageable pageable) {
        UUID tenantId = resolveTenantId();
        return reviewRepository
            .findByTenantIdAndDeletedFalseOrderBySubmissionDateDesc(tenantId, pageable)
            .map(ethicsMapper::toResponse);
    }

    /**
     * Retourne un avis éthique complet par son identifiant.
     *
     * @param id identifiant UUID de l'avis
     * @return réponse complète de l'avis
     * @throws EthicsNotFoundException si l'avis n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    public EthicsReviewResponse getReview(UUID id) {
        UUID tenantId = resolveTenantId();
        EthicsReview review = findReviewOrThrow(id, tenantId);
        return ethicsMapper.toResponse(review);
    }

    /**
     * Retourne tous les avis d'une étude pour le tenant courant.
     *
     * @param studyId identifiant de l'étude
     * @return liste de réponses d'avis pour cette étude
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public List<EthicsReviewResponse> getReviewsByStudy(UUID studyId) {
        UUID tenantId = resolveTenantId();
        return reviewRepository.findByStudyIdAndTenantIdAndDeletedFalse(studyId, tenantId)
            .stream()
            .map(ethicsMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Crée un nouvel avis éthique.
     *
     * <p>Le numéro CE est généré automatiquement via {@link SequenceGeneratorService}.
     * La décision initiale est positionnée à {@link ReviewDecision#PENDING}.
     *
     * @param request données de création de l'avis
     * @return réponse complète de l'avis créé
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    @Transactional
    public EthicsReviewResponse createReview(EthicsReviewCreateRequest request) {
        UUID tenantId = resolveTenantId();

        EthicsReview review = ethicsMapper.fromCreateRequest(request);
        review.setTenantId(tenantId);
        review.setDecision(ReviewDecision.PENDING);

        // Génération du numéro CE avec verrou pessimiste
        String ethicsNumber = sequenceGeneratorService.generateEthicsNumber(tenantId);
        review.setEthicsNumber(ethicsNumber);

        EthicsReview saved = reviewRepository.save(review);
        log.info("Avis CE créé : {} pour étude {} (tenant: {})",
            saved.getEthicsNumber(), saved.getStudyId(), tenantId);

        return ethicsMapper.toResponse(saved);
    }

    /**
     * Met à jour la décision d'un avis éthique.
     *
     * <p>Valide que la transition est autorisée : seul un avis PENDING peut recevoir
     * une décision. Un avis déjà WITHDRAWN ou APPROVED ne peut pas être modifié.
     *
     * @param reviewId identifiant de l'avis à mettre à jour
     * @param request  nouvelle décision avec date et commentaires
     * @return réponse complète de l'avis avec la nouvelle décision
     * @throws EthicsNotFoundException si l'avis n'existe pas dans le tenant courant
     * @throws EthicsException         si la transition de statut est invalide ou tenant non résolu
     */
    @Transactional
    public EthicsReviewResponse updateDecision(UUID reviewId, DecisionUpdateRequest request) {
        UUID tenantId = resolveTenantId();
        EthicsReview review = findReviewOrThrow(reviewId, tenantId);

        // Valider la transition : on ne peut pas modifier un avis final (WITHDRAWN)
        if (review.getDecision() == ReviewDecision.WITHDRAWN) {
            throw new EthicsException("Impossible de modifier un avis retiré : " + reviewId);
        }
        // La décision ne peut pas être PENDING lors d'une mise à jour
        if (request.decision() == ReviewDecision.PENDING) {
            throw new EthicsException("La décision PENDING n'est pas valide lors d'une mise à jour");
        }

        ReviewDecision previousDecision = review.getDecision();
        review.setDecision(request.decision());
        review.setDecisionDate(request.decisionDate());

        if (request.reviewDate() != null) {
            review.setReviewDate(request.reviewDate());
        }
        if (request.nextReviewDate() != null) {
            review.setNextReviewDate(request.nextReviewDate());
        }
        if (request.comments() != null) {
            review.setComments(request.comments());
        }

        EthicsReview saved = reviewRepository.save(review);
        log.info("Décision mise à jour pour avis {} : {} → {} (tenant: {})",
            saved.getEthicsNumber(), previousDecision, request.decision(), tenantId);

        return ethicsMapper.toResponse(saved);
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Résout l'UUID du tenant courant depuis le {@link TenantContext}.
     *
     * @return UUID du tenant
     * @throws EthicsException si le tenant n'est pas résolu
     */
    private UUID resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new EthicsException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        try {
            return UUID.fromString(tenantStr);
        } catch (IllegalArgumentException e) {
            throw new EthicsException("Identifiant de tenant invalide : " + tenantStr);
        }
    }

    /**
     * Recherche un avis par ID et tenant, ou lève une exception si introuvable.
     *
     * @param id       identifiant de l'avis
     * @param tenantId identifiant du tenant
     * @return entité EthicsReview trouvée
     * @throws EthicsNotFoundException si l'avis n'existe pas ou est supprimé
     */
    private EthicsReview findReviewOrThrow(UUID id, UUID tenantId) {
        return reviewRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
            .orElseThrow(() -> new EthicsNotFoundException("Avis CE", id));
    }
}
