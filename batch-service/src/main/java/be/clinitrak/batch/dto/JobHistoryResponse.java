package be.clinitrak.batch.dto;

import java.time.LocalDateTime;

/**
 * Réponse contenant l'historique d'exécution d'un job Spring Batch.
 *
 * @param jobExecutionId  identifiant unique de l'exécution
 * @param jobName         nom du job
 * @param status          statut final de l'exécution (COMPLETED, FAILED, STOPPED, etc.)
 * @param startTime       heure de démarrage
 * @param endTime         heure de fin (null si en cours)
 * @param exitCode        code de sortie Spring Batch (COMPLETED, FAILED, etc.)
 * @param exitDescription description détaillée du résultat ou de l'erreur
 */
public record JobHistoryResponse(
        Long jobExecutionId,
        String jobName,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String exitCode,
        String exitDescription
) {}
