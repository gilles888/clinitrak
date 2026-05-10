package be.clinitrak.ethics.service;

import be.clinitrak.ethics.client.dto.StudyBasicInfo;
import be.clinitrak.ethics.domain.entity.CorrespondenceTemplate;
import be.clinitrak.ethics.domain.entity.EthicsReview;
import be.clinitrak.ethics.domain.repository.CorrespondenceTemplateRepository;
import be.clinitrak.ethics.exception.EthicsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de traitement des templates de correspondance Thymeleaf stockés en base de données.
 *
 * <p>Utilise un {@link TemplateEngine} dédié configuré avec un {@code StringTemplateResolver}
 * pour traiter les templates HTML directement depuis leur contenu en base,
 * sans résolution de fichier sur disque.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateEngineService {

    @Qualifier("dbTemplateEngine")
    private final TemplateEngine templateEngine;

    private final CorrespondenceTemplateRepository templateRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Traite un template Thymeleaf stocké en base avec les variables fournies.
     *
     * <p>Les variables disponibles dans les templates :
     * {@code studyTitle}, {@code ethicsNumber}, {@code investigatorName},
     * {@code meetingDate}, {@code decision}, {@code hospitalName}, {@code recipientName}, etc.
     *
     * @param templateCode code du template à utiliser (ex: "APPROVAL_FR")
     * @param variables    map des variables de substitution
     * @return contenu HTML généré avec les variables substituées
     * @throws EthicsException si le template est introuvable ou si le traitement échoue
     */
    public String processTemplate(String templateCode, Map<String, Object> variables) {
        CorrespondenceTemplate template = templateRepository
            .findByTemplateCodeAndIsActiveTrue(templateCode)
            .orElseThrow(() -> new EthicsException("Template introuvable : " + templateCode));

        Context context = new Context();
        context.setVariables(variables);

        try {
            return templateEngine.process(template.getContent(), context);
        } catch (Exception e) {
            log.error("Erreur traitement template {} : {}", templateCode, e.getMessage());
            throw new EthicsException("Erreur traitement template : " + e.getMessage());
        }
    }

    /**
     * Retourne le sujet d'un template actif par son code.
     *
     * @param templateCode code du template
     * @return sujet du template, ou chaîne vide si non trouvé
     */
    public String getSubject(String templateCode) {
        return templateRepository.findByTemplateCodeAndIsActiveTrue(templateCode)
            .map(CorrespondenceTemplate::getSubject)
            .orElse("");
    }

    /**
     * Construit la map des variables standard depuis les données d'étude et d'avis CE.
     *
     * <p>Variables toujours présentes : {@code studyTitle}, {@code studyNumber},
     * {@code acronym}, {@code investigatorName}, {@code sponsor},
     * {@code recipientName}, {@code generatedDate}.
     *
     * <p>Variables supplémentaires si un avis CE est fourni : {@code ethicsNumber},
     * {@code submissionDate}, {@code decision}, {@code comments}.
     *
     * @param study         informations de l'étude (depuis study-service)
     * @param review        avis CE associé (peut être null)
     * @param recipientName nom du destinataire
     * @return map des variables prête à être passée à Thymeleaf
     */
    public Map<String, Object> buildVariables(StudyBasicInfo study, EthicsReview review, String recipientName) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("studyTitle", study.title());
        vars.put("studyNumber", study.studyNumber());
        vars.put("acronym", study.acronym());
        vars.put("investigatorName", study.principalInvestigator());
        vars.put("sponsor", study.sponsor());
        vars.put("recipientName", recipientName);
        vars.put("generatedDate", LocalDate.now().format(DATE_FORMATTER));

        if (review != null) {
            vars.put("ethicsNumber", review.getEthicsNumber());
            vars.put("submissionDate", review.getSubmissionDate() != null
                ? review.getSubmissionDate().format(DATE_FORMATTER) : "");
            vars.put("decision", review.getDecision() != null ? review.getDecision().getLabel() : "");
            vars.put("comments", review.getComments() != null ? review.getComments() : "");
        }

        return vars;
    }
}
