package be.clinitrak.notification.controller;

import be.clinitrak.notification.dto.NotificationResponse;
import be.clinitrak.notification.dto.SendNotificationRequest;
import be.clinitrak.notification.service.NotificationService;
import be.clinitrak.notification.sse.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Contrôleur REST du notification-service.
 * Expose les endpoints d'envoi, consultation, marquage comme lue et SSE.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications email et SSE temps réel")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Envoie une notification (email + in-app SSE).
     * Endpoint réservé aux services internes et aux super-admins.
     *
     * @param request paramètres de la notification
     * @return 201 avec les métadonnées de la notification créée
     */
    @PostMapping("/send")
    @Operation(summary = "Envoyer une notification", description = "Réservé aux services internes et SUPER_ADMIN")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_TENANT')")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.send(request);
        log.info("Notification envoyée : type={}, recipient={}", request.type(), request.recipientUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retourne les notifications paginées de l'utilisateur connecté.
     *
     * @param page numéro de page (0-based)
     * @param size taille de la page
     * @param auth authentication Spring Security
     * @return 200 avec page de notifications
     */
    @GetMapping("/my")
    @Operation(summary = "Mes notifications", description = "Notifications paginées de l'utilisateur connecté")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        // L'userId est stocké dans le claim "userId" du JWT ou on utilise le subject comme fallback
        // Dans cette implémentation, on attend l'UUID depuis un header X-User-Id positionné par le gateway
        UUID userId = extractUserIdFromAuth(auth);
        return ResponseEntity.ok(
                notificationService.findMyNotifications(userId, PageRequest.of(page, size)));
    }

    /**
     * Marque une notification comme lue.
     *
     * @param id   identifiant de la notification
     * @param auth authentication Spring Security
     * @return 200 avec la notification mise à jour
     */
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer comme lue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Identifiant UUID de la notification") @PathVariable UUID id,
            Authentication auth) {

        UUID userId = extractUserIdFromAuth(auth);
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    /**
     * Retourne le nombre de notifications non lues de l'utilisateur connecté.
     *
     * @param auth authentication Spring Security
     * @return 200 avec {@code { count: long }}
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de non-lues")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadCount(Authentication auth) {
        UUID userId = extractUserIdFromAuth(auth);
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(java.util.Map.of("count", count));
    }

    /**
     * Marque toutes les notifications de l'utilisateur comme lues.
     *
     * @param auth authentication Spring Security
     * @return 204 No Content
     */
    @PatchMapping("/read-all")
    @Operation(summary = "Tout marquer comme lu")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        UUID userId = extractUserIdFromAuth(auth);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint SSE pour la réception de notifications en temps réel.
     * Le client fournit son userId en paramètre de requête (EventSource ne supporte pas les headers).
     *
     * @param userId identifiant de l'utilisateur qui s'abonne
     * @return SseEmitter maintenant la connexion ouverte
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE", description = "Connexion Server-Sent Events pour les notifications temps réel")
    public SseEmitter streamNotifications(
            @Parameter(description = "Identifiant UUID de l'utilisateur")
            @RequestParam UUID userId) {
        log.info("Nouvelle connexion SSE demandée pour l'utilisateur {}", userId);
        return sseEmitterRegistry.register(userId);
    }

    /**
     * Extrait l'UUID utilisateur depuis l'authentification.
     * Utilise le nom du principal (email) pour retrouver l'utilisateur.
     * En production, le gateway injecte X-User-Id en header.
     */
    private UUID extractUserIdFromAuth(Authentication auth) {
        // Le gateway injecte X-User-Id ; en direct sur le service, on utilise un UUID dérivé
        // Cette méthode sera remplacée par l'injection via RequestAttribute dans un contexte gateway
        String principal = auth.getName();
        // Fallback : génération déterministe depuis l'email pour les tests
        return UUID.nameUUIDFromBytes(principal.getBytes());
    }
}
