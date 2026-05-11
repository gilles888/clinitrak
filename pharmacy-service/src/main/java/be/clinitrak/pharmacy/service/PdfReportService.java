package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.DrugStock;
import be.clinitrak.pharmacy.domain.repository.DrugStockRepository;
import be.clinitrak.pharmacy.exception.PharmacyException;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Service de génération de rapports PDF pour la pharmacie.
 *
 * <p>Utilise Flying Saucer (xhtmlrenderer) pour convertir du HTML/CSS en PDF.
 * Les rapports sont générés à la volée et retournés en mémoire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final DrugStockRepository stockRepo;

    /**
     * Génère un rapport d'inventaire PDF complet pour un tenant.
     *
     * @param tenantId identifiant du tenant
     * @return contenu PDF en tableau d'octets
     * @throws PharmacyException si la génération échoue
     */
    @Transactional(readOnly = true)
    public byte[] generateInventoryReport(String tenantId) {
        List<DrugStock> stocks = stockRepo.findByTenantIdAndDeletedFalse(tenantId);
        String html = buildInventoryHtml(stocks);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            log.info("Rapport inventaire PDF généré : {} lignes (tenant: {})", stocks.size(), tenantId);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération PDF : {}", e.getMessage(), e);
            throw new PharmacyException("Erreur génération PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Construit le HTML du rapport d'inventaire à partir des stocks.
     *
     * @param stocks liste des stocks à inclure dans le rapport
     * @return HTML complet du rapport
     */
    private String buildInventoryHtml(List<DrugStock> stocks) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
        sb.append("body{font-family:Arial,sans-serif;font-size:12px;}");
        sb.append("table{width:100%;border-collapse:collapse;}");
        sb.append("th,td{border:1px solid #ccc;padding:6px;text-align:left;}");
        sb.append("th{background:#3b82f6;color:white;}");
        sb.append("</style></head><body>");
        sb.append("<h1>Rapport d'inventaire Pharmacie</h1>");
        sb.append("<p>Généré le : ").append(LocalDate.now()).append("</p>");
        sb.append("<table><thead><tr>");
        sb.append("<th>Médicament</th><th>Étude</th><th>Lot</th>");
        sb.append("<th>Quantité</th><th>Unité</th><th>Emplacement</th>");
        sb.append("<th>Péremption</th><th>Statut</th>");
        sb.append("</tr></thead><tbody>");
        for (DrugStock s : stocks) {
            sb.append("<tr>")
              .append("<td>").append(s.getDrug() != null ? s.getDrug().getDrugName() : "").append("</td>")
              .append("<td>").append(s.getStudyId()).append("</td>")
              .append("<td>").append(s.getBatchNumber() != null ? s.getBatchNumber() : "").append("</td>")
              .append("<td>").append(s.getQuantity()).append("</td>")
              .append("<td>").append(s.getUnit() != null ? s.getUnit() : "").append("</td>")
              .append("<td>").append(s.getLocation() != null ? s.getLocation() : "").append("</td>")
              .append("<td>").append(s.getExpiryDate() != null ? s.getExpiryDate() : "").append("</td>")
              .append("<td>").append(s.getStatus() != null ? s.getStatus().getLabel() : "").append("</td>")
              .append("</tr>");
        }
        sb.append("</tbody></table></body></html>");
        return sb.toString();
    }
}
