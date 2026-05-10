package be.clinitrak.study.controller;

import be.clinitrak.study.domain.entity.StudyStatusHistory;
import be.clinitrak.study.dto.*;
import be.clinitrak.study.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des études cliniques.
 *
 * <p>Expose les endpoints CRUD pour les études cliniques, leurs contacts,
 * soumissions réglementaires, patients pseudonymisés et historiques de statut.
 *
 * <p>Tous les endpoints nécessitent une authentification JWT valide.
 * Certaines opérations d'écriture sont restreintes par rôle via {@code @PreAuthorize}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/studies")
@RequiredArgsConstructor
@Tag(name = "Studies", description = "Gestion des études cliniques")
@SecurityRequirement(name = "bearerAuth")
public class StudyController {

    private final StudyService studyService;

    // ----------------------------------------------------------------
    // Études cliniques — CRUD
    // ----------------------------------------------------------------

    /**
     * Recherche des études cliniques avec filtrage multicritères et pagination.
     *
     * @param criteria critères de recherche (tous optionnels, via query params)
     * @param pageable paramètres de pagination (page, size, sort)
     * @return page de résumés d'études
     */
    @GetMapping
    @Operation(summary = "Rechercher des études", description = "Recherche multicritères avec pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résultats de la recherche"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "400", description = "Paramètres invalides")
    })
    public ResponseEntity<PageResponse<StudySummaryResponse>> searchStudies(
        @ModelAttribute StudySearchCriteria criteria,
        @PageableDefault(size = 20, sort = "studyNumber") Pageable pageable
    ) {
        Page<StudySummaryResponse> page = studyService.searchStudies(criteria, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    /**
     * Récupère les statistiques agrégées des études du tenant courant.
     *
     * <p>Cet endpoint est placé avant {@code /{id}} pour éviter le conflit de route
     * avec "statistics" comme UUID.
     *
     * @return statistiques par statut, domaine thérapeutique et phase
     */
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques des études", description = "Statistiques agrégées pour le tableau de bord")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistiques calculées"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<StudyStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(studyService.getStatistics());
    }

    /**
     * Récupère une étude clinique complète par son identifiant.
     *
     * @param id identifiant UUID de l'étude
     * @return réponse complète de l'étude
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une étude", description = "Retourne les détails complets d'une étude")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Étude trouvée"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<StudyResponse> getStudy(@PathVariable UUID id) {
        return ResponseEntity.ok(studyService.getStudy(id));
    }

    /**
     * Crée une nouvelle étude clinique.
     *
     * @param request données de création de l'étude
     * @return 201 Created avec l'étude créée
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_CTC_PM','ROLE_CTC_CRA','ROLE_ADMIN_TENANT','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Créer une étude", description = "Crée une nouvelle étude clinique avec génération automatique du numéro")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Étude créée avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle insuffisant")
    })
    public ResponseEntity<StudyResponse> createStudy(@Valid @RequestBody StudyCreateRequest request) {
        StudyResponse created = studyService.createStudy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une étude clinique existante.
     *
     * @param id      identifiant de l'étude à mettre à jour
     * @param request données de mise à jour
     * @return étude mise à jour
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_CTC_PM','ROLE_CTC_CRA','ROLE_ADMIN_TENANT','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Mettre à jour une étude", description = "Mise à jour complète des informations d'une étude")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Étude mise à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle insuffisant")
    })
    public ResponseEntity<StudyResponse> updateStudy(
        @PathVariable UUID id,
        @Valid @RequestBody StudyUpdateRequest request
    ) {
        return ResponseEntity.ok(studyService.updateStudy(id, request));
    }

    /**
     * Met à jour le statut réglementaire d'une étude et trace l'historique.
     *
     * @param id      identifiant de l'étude
     * @param request nouveau statut avec date et commentaire
     * @return étude avec le nouveau statut
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'une étude", description = "Transition de statut avec traçabilité dans l'historique")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<StudyResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(studyService.updateStatus(id, request));
    }

    /**
     * Supprime logiquement (soft delete) une étude clinique.
     *
     * @param id identifiant de l'étude à supprimer
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN_TENANT','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Supprimer une étude", description = "Suppression logique (soft delete) — l'étude reste en base mais est masquée")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Étude supprimée"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle insuffisant")
    })
    public ResponseEntity<Void> deleteStudy(@PathVariable UUID id) {
        studyService.deleteStudy(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // Historique de statut
    // ----------------------------------------------------------------

    /**
     * Retourne l'historique complet des changements de statut d'une étude.
     *
     * @param id identifiant de l'étude
     * @return liste des entrées d'historique triée du plus récent au plus ancien
     */
    @GetMapping("/{id}/statuses")
    @Operation(summary = "Historique des statuts", description = "Retourne toutes les transitions de statut d'une étude")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique retourné"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<List<StudyStatusHistory>> getStatusHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(studyService.getStatusHistory(id));
    }

    /**
     * Ajoute une entrée de statut à l'historique (alias de PATCH /status).
     *
     * @param id      identifiant de l'étude
     * @param request nouveau statut avec date et commentaire
     * @return 201 Created avec l'étude mise à jour
     */
    @PostMapping("/{id}/statuses")
    @Operation(summary = "Ajouter un statut", description = "Ajoute une transition de statut et met à jour le statut courant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Statut ajouté"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<StudyResponse> addStatus(
        @PathVariable UUID id,
        @Valid @RequestBody StatusUpdateRequest request
    ) {
        StudyResponse updated = studyService.updateStatus(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    // ----------------------------------------------------------------
    // Contacts
    // ----------------------------------------------------------------

    /**
     * Retourne les contacts actifs d'une étude.
     *
     * @param id identifiant de l'étude
     * @return liste des contacts actifs
     */
    @GetMapping("/{id}/contacts")
    @Operation(summary = "Contacts d'une étude", description = "Retourne tous les contacts actifs de l'étude")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contacts retournés"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<List<ContactResponse>> getContacts(@PathVariable UUID id) {
        return ResponseEntity.ok(studyService.getContacts(id));
    }

    /**
     * Ajoute un contact à une étude clinique.
     *
     * @param id      identifiant de l'étude
     * @param request données du contact
     * @return 201 Created avec le contact créé
     */
    @PostMapping("/{id}/contacts")
    @Operation(summary = "Ajouter un contact", description = "Ajoute un contact (investigateur, CRA, coordinateur) à l'étude")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Contact ajouté"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<ContactResponse> addContact(
        @PathVariable UUID id,
        @Valid @RequestBody ContactRequest request
    ) {
        ContactResponse created = studyService.addContact(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Désactive un contact d'une étude (soft delete).
     *
     * @param id        identifiant de l'étude
     * @param contactId identifiant du contact à supprimer
     * @return 204 No Content
     */
    @DeleteMapping("/{id}/contacts/{contactId}")
    @Operation(summary = "Supprimer un contact", description = "Désactive un contact de l'étude (soft delete)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contact désactivé"),
        @ApiResponse(responseCode = "404", description = "Contact ou étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Void> removeContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        studyService.removeContact(id, contactId);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // Soumissions
    // ----------------------------------------------------------------

    /**
     * Retourne les soumissions d'une étude avec pagination.
     *
     * @param id       identifiant de l'étude
     * @param pageable paramètres de pagination
     * @return page de soumissions
     */
    @GetMapping("/{id}/submissions")
    @Operation(summary = "Soumissions d'une étude", description = "Retourne les soumissions réglementaires avec pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Soumissions retournées"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<PageResponse<SubmissionResponse>> getSubmissions(
        @PathVariable UUID id,
        @PageableDefault(size = 10, sort = "submissionDate") Pageable pageable
    ) {
        Page<SubmissionResponse> page = studyService.getSubmissions(id, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    /**
     * Ajoute une soumission réglementaire à une étude.
     *
     * @param id      identifiant de l'étude
     * @param request données de la soumission
     * @return 201 Created avec la soumission créée
     */
    @PostMapping("/{id}/submissions")
    @Operation(summary = "Ajouter une soumission", description = "Enregistre une soumission réglementaire (initiale, amendement, rapport)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Soumission ajoutée"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<SubmissionResponse> addSubmission(
        @PathVariable UUID id,
        @Valid @RequestBody SubmissionRequest request
    ) {
        SubmissionResponse created = studyService.addSubmission(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ----------------------------------------------------------------
    // Patients
    // ----------------------------------------------------------------

    /**
     * Retourne les patients pseudonymisés d'une étude avec pagination.
     *
     * @param id       identifiant de l'étude
     * @param pageable paramètres de pagination
     * @return page de patients
     */
    @GetMapping("/{id}/patients")
    @Operation(summary = "Patients d'une étude", description = "Retourne les patients pseudonymisés (RGPD) avec pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patients retournés"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<PageResponse<PatientResponse>> getPatients(
        @PathVariable UUID id,
        @PageableDefault(size = 20, sort = "patientCode") Pageable pageable
    ) {
        Page<PatientResponse> page = studyService.getPatients(id, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    /**
     * Ajoute un patient pseudonymisé à une étude.
     *
     * @param id      identifiant de l'étude
     * @param request données du patient (code pseudonyme RGPD uniquement)
     * @return 201 Created avec le patient créé
     */
    @PostMapping("/{id}/patients")
    @Operation(summary = "Ajouter un patient", description = "Enregistre un patient pseudonymisé (code RGPD, pas de données identifiantes)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient ajouté"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou code patient dupliqué"),
        @ApiResponse(responseCode = "404", description = "Étude introuvable"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<PatientResponse> addPatient(
        @PathVariable UUID id,
        @Valid @RequestBody PatientRequest request
    ) {
        PatientResponse created = studyService.addPatient(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
