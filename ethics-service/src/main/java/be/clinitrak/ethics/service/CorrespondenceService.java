package be.clinitrak.ethics.service;

import be.clinitrak.ethics.client.StudyServiceClient;
import be.clinitrak.ethics.client.dto.StudyBasicInfo;
import be.clinitrak.ethics.domain.entity.Correspondence;
import be.clinitrak.ethics.domain.entity.EthicsReview;
import be.clinitrak.ethics.domain.repository.CorrespondenceRepository;
import be.clinitrak.ethics.domain.repository.CorrespondenceTemplateRepository;
import be.clinitrak.ethics.domain.repository.EthicsReviewRepository;
import be.clinitrak.ethics.dto.CorrespondenceGenerateRequest;
import be.clinitrak.ethics.dto.CorrespondenceResponse;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Service métier pour la génération et l'envoi de correspondances CE.
 *
 * <p>La génération d'une correspondance suit le processus :
 * <ol>
 *   <li>Chargement des données de l'étude via le study-service (Feign)</li>
 *   <li>Chargement de l'avis CE si {@code reviewId} est fourni</li>
 *   <li>Construction des variables Thymeleaf + variables supplémentaires</li>
 *   <li>Traitement du template HTML via {@link TemplateEngineService}</li>
 *   <li>Persistance de la correspondance en base</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorrespondenceService {

    private final CorrespondenceRepository correspondenceRepository;
    private final CorrespondenceTemplateRepository templateRepository;
    private final EthicsReviewRepository reviewRepository;
    private final StudyServiceClient studyServiceClient;
    private final TemplateEngineService templateEngineService;
    private final EthicsMapper ethicsMapper;

    /**
     * Génère une correspondance CE depuis un template Thymeleaf.
     *
     * <p>Charge les données de l'étude depuis le study-service via Feign,
     * traite le template HTML avec les variables substituées, et persiste
     * la correspondance en base. Le contenu HTML est également disponible
     * pour génération PDF ultérieure.
     *
     * @param request données de génération (étude, template, destinataire, variables)
     * @return réponse de la correspondance créée
     * @throws EthicsException si le template est introuvable ou si la génération échoue
     */
    @Transactional
    public CorrespondenceResponse generateCorrespondence(CorrespondenceGenerateRequest request) {
        UUID tenantId = resolveTenantId();

        // 1. Charger les données de l'étude via Feign
        StudyBasicInfo study;
        try {
            study = studyServiceClient.getStudy(request.studyId());
        } catch (Exception e) {
            log.error("Impossible de charger l'étude {} : {}", request.studyId(), e.getMessage());
            throw new EthicsException("Impossible d'accéder aux données de l'étude : " + e.getMessage());
        }

        // 2. Charger l'avis CE si reviewId est fourni
        EthicsReview review = null;
        if (request.reviewId() != null) {
            review = reviewRepository.findByIdAndTenantIdAndDeletedFalse(request.reviewId(), tenantId)
                .orElseThrow(() -> new EthicsNotFoundException("Avis CE", request.reviewId()));
        }

        // 3. Construire les variables Thymeleaf
        Map<String, Object> variables = templateEngineService.buildVariables(study, review, request.recipientName());
        // Ajouter les variables supplémentaires fournies par l'appelant
        if (request.additionalVariables() != null) {
            variables.putAll(request.additionalVariables());
        }

        // 4. Traiter le template
        String htmlContent = templateEngineService.processTemplate(request.templateCode(), variables);
        String subject = templateEngineService.getSubject(request.templateCode());

        // Résoudre l'ID du template
        UUID templateId = templateRepository.findByTemplateCodeAndIsActiveTrue(request.templateCode())
            .map(t -> t.getId())
            .orElse(null);

        // 5. Persister la correspondance
        Correspondence correspondence = new Correspondence();
        correspondence.setTenantId(tenantId);
        correspondence.setStudyId(request.studyId());
        correspondence.setReviewId(request.reviewId());
        correspondence.setTemplateId(templateId);
        correspondence.setGeneratedDate(LocalDate.now());
        correspondence.setRecipientEmail(request.recipientEmail());
        correspondence.setRecipientName(request.recipientName());
        correspondence.setSubject(subject);
        correspondence.setContent(htmlContent);
        correspondence.setSent(false);

        Correspondence saved = correspondenceRepository.save(correspondence);
        log.info("Correspondance générée : {} pour étude {} (template: {}, tenant: {})",
            saved.getId(), request.studyId(), request.templateCode(), tenantId);

        return ethicsMapper.toCorrespondenceResponse(saved);
    }

    /**
     * Marque une correspondance comme envoyée.
     *
     * <p>En production, cette méthode déclencherait l'envoi réel via le notification-service.
     * Actuellement, elle met à jour le statut d'envoi en base.
     *
     * @param correspondenceId identifiant de la correspondance à envoyer
     * @return réponse de la correspondance mise à jour
     * @throws EthicsNotFoundException si la correspondance n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    @Transactional
    public CorrespondenceResponse sendCorrespondence(UUID correspondenceId) {
        UUID tenantId = resolveTenantId();
        Correspondence correspondence = correspondenceRepository.findById(correspondenceId)
            .filter(c -> tenantId.equals(c.getTenantId()) && !c.isDeleted())
            .orElseThrow(() -> new EthicsNotFoundException("Correspondance", correspondenceId));

        try {
            // TODO production : router vers notification-service via Feign
            // notificationClient.sendEmail(correspondence.getRecipientEmail(),
            //     correspondence.getSubject(), correspondence.getContent());

            correspondence.setSent(true);
            correspondence.setSentDate(LocalDate.now());
            correspondence.setSendError(null);
            log.info("Correspondance {} marquée comme envoyée à {} (tenant: {})",
                correspondenceId, correspondence.getRecipientEmail(), tenantId);
        } catch (Exception e) {
            log.error("Erreur envoi correspondance {} : {}", correspondenceId, e.getMessage());
            correspondence.setSendError(e.getMessage());
        }

        Correspondence saved = correspondenceRepository.save(correspondence);
        return ethicsMapper.toCorrespondenceResponse(saved);
    }

    /**
     * Retourne les correspondances d'une étude pour le tenant courant.
     *
     * @param studyId  identifiant de l'étude
     * @param pageable paramètres de pagination
     * @return page de correspondances, triée de la plus récente à la plus ancienne
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public Page<CorrespondenceResponse> getCorrespondenceByStudy(UUID studyId, Pageable pageable) {
        UUID tenantId = resolveTenantId();
        return correspondenceRepository
            .findByStudyIdAndTenantIdOrderByGeneratedDateDesc(studyId, tenantId, pageable)
            .map(ethicsMapper::toCorrespondenceResponse);
    }

    /**
     * Retourne le contenu HTML d'une correspondance pour génération PDF.
     *
     * @param correspondenceId identifiant de la correspondance
     * @return contenu HTML de la correspondance
     * @throws EthicsNotFoundException si la correspondance n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu ou contenu vide
     */
    public String getCorrespondenceContent(UUID correspondenceId) {
        UUID tenantId = resolveTenantId();
        Correspondence correspondence = correspondenceRepository.findById(correspondenceId)
            .filter(c -> tenantId.equals(c.getTenantId()) && !c.isDeleted())
            .orElseThrow(() -> new EthicsNotFoundException("Correspondance", correspondenceId));

        if (correspondence.getContent() == null || correspondence.getContent().isBlank()) {
            throw new EthicsException("Aucun contenu disponible pour la correspondance : " + correspondenceId);
        }

        return correspondence.getContent();
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
}
