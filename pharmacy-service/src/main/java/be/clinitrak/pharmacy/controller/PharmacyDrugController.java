package be.clinitrak.pharmacy.controller;

import be.clinitrak.pharmacy.dto.DrugCreateRequest;
import be.clinitrak.pharmacy.dto.DrugResponse;
import be.clinitrak.pharmacy.service.DrugService;
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

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des médicaments expérimentaux.
 *
 * <p>Endpoints disponibles :
 * <ul>
 *   <li>GET /api/v1/pharmacy/drugs — liste les médicaments du tenant</li>
 *   <li>GET /api/v1/pharmacy/drugs/{id} — détail d'un médicament</li>
 *   <li>POST /api/v1/pharmacy/drugs — crée un nouveau médicament</li>
 *   <li>DELETE /api/v1/pharmacy/drugs/{id} — soft-delete d'un médicament</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pharmacy/drugs")
@RequiredArgsConstructor
@Tag(name = "Médicaments", description = "Gestion des médicaments expérimentaux (IMP/NIMP/Placebo)")
public class PharmacyDrugController {

    private final DrugService drugService;

    /**
     * Retourne la liste des médicaments du tenant courant.
     *
     * @return liste des médicaments
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Lister les médicaments", description = "Retourne tous les médicaments non supprimés du tenant")
    public ResponseEntity<List<DrugResponse>> getAll() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(drugService.getAll(tenantId));
    }

    /**
     * Retourne le détail d'un médicament par son identifiant.
     *
     * @param id identifiant UUID du médicament
     * @return détail du médicament
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_PHARMACIST','ROLE_CTC_MANAGER','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Détail d'un médicament")
    public ResponseEntity<DrugResponse> getById(@PathVariable UUID id) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(drugService.getById(id, tenantId));
    }

    /**
     * Crée un nouveau médicament expérimental.
     *
     * @param request données de création du médicament
     * @return médicament créé avec statut 201
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_PHARMACIST')")
    @Operation(summary = "Créer un médicament", description = "Crée un médicament expérimental avec statut réglementaire PENDING")
    public ResponseEntity<DrugResponse> create(@Valid @RequestBody DrugCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        DrugResponse response = drugService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Supprime logiquement un médicament (soft-delete).
     *
     * @param id identifiant UUID du médicament
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PHARMACIST')")
    @Operation(summary = "Supprimer un médicament (soft-delete)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        String tenantId = TenantContext.getTenantId();
        drugService.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
