package be.clinitrak.ethics.service;

import be.clinitrak.ethics.exception.EthicsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Service de génération de PDF depuis un contenu HTML.
 *
 * <p>Utilise Flying Saucer ({@code xhtmlrenderer}) avec le backend OpenPDF (iText fork libre).
 * Le contenu HTML doit être du XHTML valide pour que Flying Saucer puisse le parser correctement.
 *
 * <p>Pour garantir la compatibilité XHTML, les templates Thymeleaf doivent utiliser
 * le mode {@code TemplateMode.HTML} et respecter les règles de fermeture des balises.
 */
@Slf4j
@Service
public class PdfGenerationService {

    /**
     * Génère un PDF depuis un contenu HTML en utilisant Flying Saucer + OpenPDF.
     *
     * <p>Le contenu HTML est transformé en XHTML par Flying Saucer avant rendu.
     * Les polices système sont utilisées par défaut ; pour des polices personnalisées,
     * elles doivent être enregistrées dans le {@code ITextRenderer}.
     *
     * @param htmlContent contenu HTML (doit être XHTML valide ou proche de XHTML)
     * @return tableau d'octets du PDF généré
     * @throws EthicsException si la génération du PDF échoue
     */
    public byte[] generatePdfFromHtml(String htmlContent) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.getSharedContext().setPrint(true);
            renderer.getSharedContext().setInteractive(false);
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(baos);
            log.debug("PDF généré avec succès ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération PDF : {}", e.getMessage(), e);
            throw new EthicsException("Impossible de générer le PDF : " + e.getMessage());
        }
    }
}
