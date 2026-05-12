package be.clinitrak.notification.service;

import be.clinitrak.notification.domain.entity.Notification;
import be.clinitrak.notification.domain.repository.NotificationRepository;
import be.clinitrak.notification.dto.NotificationResponse;
import be.clinitrak.notification.dto.SendNotificationRequest;
import be.clinitrak.notification.dto.SseEvent;
import be.clinitrak.notification.exception.NotificationNotFoundException;
import be.clinitrak.notification.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service métier du notification-service.
 * Orchestre l'envoi d'emails, la persistance des notifications en base
 * et la diffusion SSE en temps réel.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * Envoie une notification complète : email + persistance en base + push SSE.
     * L'envoi email est effectué de manière asynchrone pour ne pas bloquer la réponse HTTP.
     *
     * @param request paramètres de la notification à envoyer
     * @return réponse contenant les métadonnées de la notification créée
     */
    public NotificationResponse send(SendNotificationRequest request) {
        // Persistance en base
        Notification notification = Notification.builder()
                .recipientUserId(request.recipientUserId())
                .tenantId(request.tenantId())
                .type(request.type())
                .subject(request.subject())
                .message(request.subject()) // Message court = sujet par défaut
                .templateName(request.templateName())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Envoi email asynchrone
        sendEmailAsync(request);

        // Push SSE en temps réel
        SseEvent sseEvent = new SseEvent(
                saved.getId().toString(),
                request.type().name(),
                request.subject());
        sseEmitterRegistry.sendToUser(request.recipientUserId(), sseEvent);

        log.info("Notification envoyée : id={}, type={}, recipient={}",
                saved.getId(), request.type(), request.recipientUserId());

        return toResponse(saved);
    }

    /**
     * Récupère les notifications paginées de l'utilisateur connecté.
     *
     * @param userId   identifiant de l'utilisateur
     * @param pageable paramètres de pagination
     * @return page de notifications
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMyNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserId(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Marque une notification comme lue.
     *
     * @param id     identifiant de la notification
     * @param userId identifiant de l'utilisateur (vérification d'autorisation)
     * @return notification mise à jour
     * @throws NotificationNotFoundException si la notification n'existe pas ou n'appartient pas à l'utilisateur
     */
    public NotificationResponse markAsRead(UUID id, UUID userId) {
        Notification notification = notificationRepository.findActiveById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new NotificationNotFoundException(id);
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        Notification updated = notificationRepository.save(notification);
        log.debug("Notification {} marquée comme lue par {}", id, userId);
        return toResponse(updated);
    }

    /**
     * Compte les notifications non lues de l'utilisateur.
     *
     * @param userId identifiant de l'utilisateur
     * @return nombre de notifications non lues
     */
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalseAndDeletedAtIsNull(userId);
    }

    /**
     * Marque toutes les notifications de l'utilisateur comme lues.
     *
     * @param userId identifiant de l'utilisateur
     */
    public void markAllAsRead(UUID userId) {
        notificationRepository.findByRecipientUserId(userId, org.springframework.data.domain.Pageable.unpaged())
                .forEach(n -> {
                    if (!n.isRead()) {
                        n.setRead(true);
                        n.setReadAt(java.time.LocalDateTime.now());
                        notificationRepository.save(n);
                    }
                });
        log.debug("Toutes les notifications marquées comme lues pour l'utilisateur {}", userId);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    /**
     * Envoie l'email de manière asynchrone.
     */
    @Async
    protected void sendEmailAsync(SendNotificationRequest request) {
        try {
            String templateName = request.templateName() != null
                    ? request.templateName()
                    : request.type().name().toLowerCase().replace('_', '-');

            emailService.sendEmail(
                    request.recipientEmail(),
                    request.subject(),
                    templateName,
                    request.templateVars());

            // Mise à jour du statut email (dans une nouvelle transaction pour éviter le lazy)
            notificationRepository.findByRecipientUserId(
                    request.recipientUserId(), Pageable.unpaged())
                    .stream()
                    .filter(n -> n.getSubject().equals(request.subject()) && !n.isEmailSent())
                    .findFirst()
                    .ifPresent(n -> {
                        n.setEmailSent(true);
                        n.setEmailSentAt(LocalDateTime.now());
                        notificationRepository.save(n);
                    });

        } catch (Exception ex) {
            log.error("Échec de l'envoi email asynchrone vers {} : {}", request.recipientEmail(), ex.getMessage());
        }
    }

    /**
     * Convertit une entité Notification en DTO de réponse.
     */
    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getSubject(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
