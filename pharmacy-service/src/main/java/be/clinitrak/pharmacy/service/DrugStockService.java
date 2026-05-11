package be.clinitrak.pharmacy.service;

import be.clinitrak.pharmacy.domain.entity.DrugStock;
import be.clinitrak.pharmacy.domain.entity.InvestigationalDrug;
import be.clinitrak.pharmacy.domain.enums.StockStatus;
import be.clinitrak.pharmacy.domain.repository.DrugStockRepository;
import be.clinitrak.pharmacy.domain.repository.InvestigationalDrugRepository;
import be.clinitrak.pharmacy.dto.StockImportResult;
import be.clinitrak.pharmacy.dto.StockReceiveRequest;
import be.clinitrak.pharmacy.dto.StockResponse;
import be.clinitrak.pharmacy.exception.PharmacyNotFoundException;
import be.clinitrak.pharmacy.mapper.PharmacyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour la gestion des stocks de médicaments.
 *
 * <p>Gère la réception des stocks (toujours en quarantaine), la mise à jour du statut,
 * et l'import depuis fichiers CSV/XLSX via Apache POI.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DrugStockService {

    private final DrugStockRepository stockRepo;
    private final InvestigationalDrugRepository drugRepo;
    private final PharmacyMapper mapper;

    /**
     * Retourne tous les stocks non supprimés du tenant.
     *
     * @param tenantId identifiant du tenant
     * @return liste des stocks actifs
     */
    @Transactional(readOnly = true)
    public List<StockResponse> getAll(String tenantId) {
        return stockRepo.findByTenantIdAndDeletedFalse(tenantId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    /**
     * Enregistre la réception d'un nouveau lot de médicament.
     *
     * <p>Le stock est automatiquement mis en quarantaine ({@link StockStatus#QUARANTINE})
     * à la réception, en attente de validation par le pharmacien.
     *
     * @param req      données de réception
     * @param tenantId identifiant du tenant
     * @return DTO du stock créé
     * @throws PharmacyNotFoundException si le médicament n'existe pas
     */
    public StockResponse receiveStock(StockReceiveRequest req, String tenantId) {
        InvestigationalDrug drug = drugRepo.findById(req.drugId())
            .orElseThrow(() -> new PharmacyNotFoundException("Drug not found"));
        DrugStock stock = new DrugStock();
        stock.setDrug(drug);
        stock.setStudyId(req.studyId() != null ? req.studyId() : drug.getStudyId());
        stock.setQuantity(req.quantity());
        stock.setUnit(req.unit());
        stock.setReceivedDate(req.receivedDate());
        stock.setExpiryDate(req.expiryDate());
        stock.setBatchNumber(req.batchNumber());
        stock.setLocation(req.location());
        stock.setStatus(StockStatus.QUARANTINE);
        stock.setTenantId(tenantId);
        log.info("Réception stock : {} unités de {} (lot: {}, tenant: {})",
            req.quantity(), drug.getDrugName(), req.batchNumber(), tenantId);
        return mapper.toResponse(stockRepo.save(stock));
    }

    /**
     * Met à jour le statut d'un stock (ex: QUARANTINE → AVAILABLE après contrôle qualité).
     *
     * @param id       identifiant UUID du stock
     * @param status   nouveau statut
     * @param tenantId identifiant du tenant
     * @return DTO du stock mis à jour
     * @throws PharmacyNotFoundException si le stock n'existe pas
     */
    public StockResponse updateStatus(UUID id, StockStatus status, String tenantId) {
        DrugStock stock = stockRepo.findById(id)
            .filter(s -> s.getTenantId().equals(tenantId) && !s.isDeleted())
            .orElseThrow(() -> new PharmacyNotFoundException("DrugStock", id));
        stock.setStatus(status);
        log.info("Mise à jour statut stock {} → {} (tenant: {})", id, status, tenantId);
        return mapper.toResponse(stockRepo.save(stock));
    }

    /**
     * Importe des stocks depuis un fichier CSV ou XLSX via Apache POI.
     *
     * <p>Format attendu des colonnes (ligne 0 = en-tête ignorée) :
     * <ol>
     *   <li>drugId (UUID)</li>
     *   <li>quantity (entier)</li>
     *   <li>unit</li>
     *   <li>receivedDate (ISO 8601 : YYYY-MM-DD)</li>
     *   <li>expiryDate (ISO 8601 : YYYY-MM-DD)</li>
     *   <li>batchNumber</li>
     *   <li>location</li>
     * </ol>
     *
     * @param file     fichier CSV ou XLSX
     * @param tenantId identifiant du tenant
     * @return résultat de l'import (lignes importées, échouées, erreurs)
     */
    public StockImportResult importFromCsv(MultipartFile file, String tenantId) {
        int imported = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // skip header
                try {
                    String drugIdStr = getCellValue(row, 0);
                    int quantity = (int) Double.parseDouble(getCellValue(row, 1));
                    String unit = getCellValue(row, 2);
                    LocalDate receivedDate = LocalDate.parse(getCellValue(row, 3));
                    LocalDate expiryDate = LocalDate.parse(getCellValue(row, 4));
                    String batchNumber = getCellValue(row, 5);
                    String location = getCellValue(row, 6);

                    UUID drugId = UUID.fromString(drugIdStr);
                    InvestigationalDrug drug = drugRepo.findById(drugId).orElseThrow(
                        () -> new PharmacyNotFoundException("Drug not found: " + drugIdStr)
                    );
                    DrugStock stock = new DrugStock();
                    stock.setDrug(drug);
                    stock.setStudyId(drug.getStudyId());
                    stock.setQuantity(quantity);
                    stock.setUnit(unit);
                    stock.setReceivedDate(receivedDate);
                    stock.setExpiryDate(expiryDate);
                    stock.setBatchNumber(batchNumber);
                    stock.setLocation(location);
                    stock.setStatus(StockStatus.QUARANTINE);
                    stock.setTenantId(tenantId);
                    stockRepo.save(stock);
                    imported++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Ligne " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Erreur lecture fichier: " + e.getMessage());
        }
        log.info("Import stock terminé : {} importés, {} échoués (tenant: {})", imported, failed, tenantId);
        return new StockImportResult(imported, failed, errors);
    }

    /**
     * Extrait la valeur textuelle d'une cellule quel que soit son type.
     *
     * @param row requête HTTP entrante
     * @param col index de la colonne
     * @return valeur de la cellule sous forme de String
     */
    private String getCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue().trim();
            default -> "";
        };
    }
}
