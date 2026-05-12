package be.clinitrak.notification.sse;

import be.clinitrak.notification.dto.SseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre centralisant les connexions SSE (Server-Sent Events) actives
 * par identifiant utilisateur. Permet le push de notifications en temps réel
 * vers les clients connectés.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    /** Timeout par défaut des connexions SSE : 30 minutes. */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    /** Map des émetteurs SSE actifs, indexés par userId. */
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Enregistre une nouvelle connexion SSE pour un utilisateur.
     * Configure les callbacks de complétion et d'erreur pour nettoyage automatique.
     *
     * @param userId identifiant de l'utilisateur connecté
     * @return SseEmitter à retourner dans la réponse HTTP
     */
    public SseEmitter register(UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);
        log.debug("Nouvelle connexion SSE enregistrée pour l'utilisateur {}", userId);

        // Nettoyage automatique à la complétion/expiration
        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> {
            log.debug("Erreur SSE pour l'utilisateur {} : {}", userId, ex.getMessage());
            removeEmitter(userId, emitter);
        });

        // Événement de connexion initiale (heartbeat)
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("Connexion SSE établie pour l'utilisateur " + userId));
        } catch (IOException ex) {
            log.warn("Impossible d'envoyer l'événement de connexion à {} : {}", userId, ex.getMessage());
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    /**
     * Envoie un événement SSE à tous les émetteurs actifs d'un utilisateur.
     * Les émetteurs défaillants sont automatiquement supprimés du registre.
     *
     * @param userId identifiant de l'utilisateur destinataire
     * @param event  événement à transmettre
     */
    public void sendToUser(UUID userId, SseEvent event) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("Aucun émetteur SSE actif pour l'utilisateur {}", userId);
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.id())
                        .name(event.type())
                        .data(event.message()));
                log.debug("Événement SSE '{}' envoyé à l'utilisateur {}", event.type(), userId);
            } catch (IOException ex) {
                log.warn("Échec d'envoi SSE à {} : {}", userId, ex.getMessage());
                deadEmitters.add(emitter);
            }
        }

        // Suppression des émetteurs morts
        userEmitters.removeAll(deadEmitters);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }

    /**
     * Supprime périodiquement les émetteurs expirés ou fermés.
     * Exécuté toutes les 5 minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void cleanupExpired() {
        int beforeCount = emitters.values().stream().mapToInt(List::size).sum();
        emitters.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        int afterCount = emitters.values().stream().mapToInt(List::size).sum();
        if (beforeCount != afterCount) {
            log.info("Nettoyage SSE : {} émetteur(s) actif(s) restant(s)", afterCount);
        }
    }

    /**
     * Retourne le nombre de connexions SSE actives (pour monitoring).
     *
     * @return nombre total de SseEmitter actifs
     */
    public int getActiveConnectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Supprime un émetteur spécifique du registre.
     */
    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
