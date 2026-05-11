package be.clinitrak.pharmacy.controller;

import be.clinitrak.pharmacy.dto.StockImportResult;
import be.clinitrak.pharmacy.dto.StockReceiveRequest;
import be.clinitrak.pharmacy.dto.StockResponse;
import be.clinitrak.pharmacy.dto.StockStatusUpdateRequest;
import be.clinitrak.pharmacy.service.DrugStockService;
import be.clinitrak.pharmacy.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des stocks de médicaments.
 *
 * <p>Endpoints disponibles :
 * <ul>
 *   <li>GET /api/v1/pharmacy/stocks — liste les stocks du tenant</li>
 *   <li>POST /api/v1/pharmacy/stocks/receive — réceptionne un lot</li>
 *   <li>PATCH /api/v1/pharmacy/stocks/{id}/status — met à jour le statut</li>
 *   <li>POST /api/v1/pharmacy/stocks/import — import CSV/XLSX</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pharmacy/stocks")
@RequiredArgsConstructor
@Tag(name = "Stocks", description = "Gestion des stocks de médicaments expérimentaux")
public class PharmacyStockController {

    private final DrugStockService stockService;

    /**
     * Retourne la liste des stocks du tenant courant.
     *
     * @return liste des stocks actifs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les stocks")
    public ResponseEntity<List<StockResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(stockService.getAll(tenantId));
    }

    /**
     * Réceptionne un nouveau lot de médicament.
     *
     * <p>Le stock est automatiquement placé en quarantaine à la réception.
     *
     * @param request données de réception du lot
     * @return stock créé avec statut 201
     */
    @PostMapping("/receive")
    @PreAuthorize("hasRole('ROLE_PHARMACIST')")
    @Operation(summary = "Réceptionner un lot", description = "Réceptionne un lot en quarantaine")
    public ResponseEntity<StockResponse> receive(@Valid @RequestBody StockReceiveRequest request) {
        String tenantId = TenantContext.getTenantId();
        StockResponse response = stockService.receiveStock(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Met à jour le statut d'un stock (ex: QUARANTINE → AVAILABLE).
     *
     * @param id      identifiant UUID du stock
     * @param request nouveau statut à appliquer
     * @return stock mis à jour
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ROLE_PHARMACIST')")
    @Operation(summary = "Mettre à jour le statut d'un stock")
    public ResponseEntity<StockResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody StockStatusUpdateRequest request
    ) {
        String tenantId = TenantContext.getTenantId();
        StockResponse response = stockService.updateStatus(id, request.status(), tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Importe des stocks depuis un fichier CSV ou XLSX.
     *
     * @param file fichier CSV ou XLSX contenant les données de stock
     * @return résultat de l'import (importés, échoués, erreurs)
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ROLE_PHARMACIST')")
    @Operation(summary = "Importer des stocks depuis CSV/XLSX")
    public ResponseEntity<StockImportResult> importStocks(
        @RequestParam("file") MultipartFile file
    ) {
        String tenantId = TenantContext.getTenantId();
        StockImportResult result = stockService.importFromCsv(file, tenantId);
        return ResponseEntity.ok(result);
    }
}
