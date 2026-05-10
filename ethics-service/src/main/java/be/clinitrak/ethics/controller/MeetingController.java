package be.clinitrak.ethics.controller;

import be.clinitrak.ethics.domain.enums.MeetingStatus;
import be.clinitrak.ethics.dto.AgendaItemRequest;
import be.clinitrak.ethics.dto.AgendaItemResponse;
import be.clinitrak.ethics.dto.MeetingCreateRequest;
import be.clinitrak.ethics.dto.MeetingResponse;
import be.clinitrak.ethics.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des réunions du Comité d'Éthique.
 *
 * <p>Fournit les endpoints pour créer, modifier et consulter les réunions
 * ainsi que leurs ordres du jour.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ethics/meetings")
@RequiredArgsConstructor
@Tag(name = "Réunions CE", description = "Gestion des réunions du Comité d'Éthique")
public class MeetingController {

    private final MeetingService meetingService;

    /**
     * Retourne les réunions dans une plage de dates.
     *
     * @param from date de début (ISO, obligatoire)
     * @param to   date de fin (ISO, obligatoire)
     * @return 200 OK avec la liste des réunions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Lister les réunions", description = "Retourne les réunions CE dans une plage de dates")
    public ResponseEntity<List<MeetingResponse>> getMeetings(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(meetingService.getMeetingsByDateRange(from, to));
    }

    /**
     * Crée une nouvelle réunion CE.
     *
     * @param request données de création
     * @return 201 Created avec la réunion créée
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Créer une réunion", description = "Crée une nouvelle réunion du Comité d'Éthique")
    public ResponseEntity<MeetingResponse> createMeeting(@Valid @RequestBody MeetingCreateRequest request) {
        MeetingResponse response = meetingService.createMeeting(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Met à jour une réunion CE existante.
     *
     * @param id      identifiant de la réunion
     * @param request nouvelles données
     * @return 200 OK avec la réunion mise à jour
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Modifier une réunion", description = "Met à jour les informations d'une réunion CE")
    public ResponseEntity<MeetingResponse> updateMeeting(
        @PathVariable UUID id,
        @Valid @RequestBody MeetingCreateRequest request
    ) {
        return ResponseEntity.ok(meetingService.updateMeeting(id, request));
    }

    /**
     * Retourne l'ordre du jour d'une réunion.
     *
     * @param id identifiant de la réunion
     * @return 200 OK avec la liste des items de l'ordre du jour
     */
    @GetMapping("/{id}/agenda")
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Ordre du jour", description = "Retourne les items de l'ordre du jour d'une réunion")
    public ResponseEntity<List<AgendaItemResponse>> getAgendaItems(@PathVariable UUID id) {
        return ResponseEntity.ok(meetingService.getAgendaItems(id));
    }

    /**
     * Ajoute un item à l'ordre du jour d'une réunion.
     *
     * @param id      identifiant de la réunion
     * @param request données de l'item
     * @return 201 Created avec l'item créé
     */
    @PostMapping("/{id}/agenda")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CE_SECRETARY', 'CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Ajouter à l'ordre du jour", description = "Ajoute un item à l'ordre du jour d'une réunion")
    public ResponseEntity<AgendaItemResponse> addAgendaItem(
        @PathVariable UUID id,
        @Valid @RequestBody AgendaItemRequest request
    ) {
        AgendaItemResponse response = meetingService.addAgendaItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Met à jour le statut d'une réunion.
     *
     * @param id     identifiant de la réunion
     * @param status nouveau statut
     * @return 200 OK
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CE_COORDINATOR', 'ADMIN_TENANT', 'SUPER_ADMIN')")
    @Operation(summary = "Changer le statut", description = "Met à jour le statut d'une réunion CE")
    public ResponseEntity<Void> updateStatus(
        @PathVariable UUID id,
        @RequestParam MeetingStatus status
    ) {
        meetingService.updateMeetingStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
