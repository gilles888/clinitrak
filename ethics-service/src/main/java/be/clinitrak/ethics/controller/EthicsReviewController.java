package be.clinitrak.ethics.controller;

import be.clinitrak.ethics.dto.DecisionUpdateRequest;
import be.clinitrak.ethics.dto.EthicsReviewCreateRequest;
import be.clinitrak.ethics.dto.EthicsReviewResponse;
import be.clinitrak.ethics.service.EthicsReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des avis éthiques.
 *
 * <p>Fournit les endpoints CRUD pour les avis CE avec contrôle d'accès RBAC.
 * La création nécessite le rôle CE_SECRETARY, CE_COORDINATOR ou ADMIN_TENANT.
 * Les décisions sont réservées aux rôles CE_COORDINATOR et ADMIN_TENANT.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ethics/reviews")
@RequiredArgsConstructor
@Tag(name = "Avis Éthiques", description = "Gestion des avis soumis au Comité d'Éthique")
public class EthicsReviewController {

    private final EthicsReviewService reviewService;

    /**
     * Crée un nouvel avis éthique avec génération automatique du numéro CE.
     *
     * @param request données de création de l'avis
     * @return 201 Created avec l'avis créé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Créer un avis CE", description = "Crée un nouvel avis et génère automatiquement le numéro CE")
    public ResponseEntity<EthicsReviewResponse> createReview(
        @Valid @RequestBody EthicsReviewCreateRequest request
    ) {
        log.debug("POST /api/v1/ethics/reviews — création avis pour étude {}", request.studyId());
        EthicsReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne les avis éthiques du tenant courant avec pagination.
     *
     * @param pageable paramètres de pagination et tri
     * @return 200 OK avec la page d'avis
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Lister les avis CE", description = "Retourne les avis paginés du tenant courant")
    public ResponseEntity<Page<EthicsReviewResponse>> getReviews(Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviews(pageable));
    }

    /**
     * Retourne un avis éthique par son identifiant.
     *
     * @param id identifiant UUID de l'avis
     * @return 200 OK avec l'avis trouvé
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Récupérer un avis CE", description = "Retourne les détails complets d'un avis CE")
    public ResponseEntity<EthicsReviewResponse> getReview(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getReview(id));
    }

    /**
     * Retourne tous les avis CE d'une étude pour le tenant courant.
     *
     * @param studyId identifiant de l'étude
     * @return 200 OK avec la liste des avis de l'étude
     */
    @GetMapping("/by-study/{studyId}")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Avis par étude", description = "Retourne tous les avis CE d'une étude spécifique")
    public ResponseEntity<List<EthicsReviewResponse>> getReviewsByStudy(@PathVariable UUID studyId) {
        return ResponseEntity.ok(reviewService.getReviewsByStudy(studyId));
    }

    /**
     * Met à jour la décision d'un avis éthique.
     *
     * @param id      identifiant de l'avis
     * @param request nouvelle décision avec date et commentaires
     * @return 200 OK avec l'avis mis à jour
     */
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Enregistrer une décision", description = "Met à jour la décision CE d'un avis (réservé aux coordinateurs)")
    public ResponseEntity<EthicsReviewResponse> updateDecision(
        @PathVariable UUID id,
        @Valid @RequestBody DecisionUpdateRequest request
    ) {
        log.debug("PATCH /api/v1/ethics/reviews/{}/decision — décision: {}", id, request.decision());
        return ResponseEntity.ok(reviewService.updateDecision(id, request));
    }
}
