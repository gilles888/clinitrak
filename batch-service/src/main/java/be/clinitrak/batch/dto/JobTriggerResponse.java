package be.clinitrak.batch.dto;

import java.time.LocalDateTime;

/**
 * Réponse au déclenchement manuel d'un job Spring Batch.
 *
 * @param jobName         nom du job déclenché
 * @param jobExecutionId  identifiant de l'exécution Spring Batch
 * @param status          statut de l'exécution (STARTED, COMPLETED, FAILED, etc.)
 * @param startTime       heure de démarrage de l'exécution
 * @param message         message descriptif du résultat
 */
public record JobTriggerResponse(
        String jobName,
        Long jobExecutionId,
        String status,
        LocalDateTime startTime,
        String message
) {}
