package be.clinitrak.study.exception;

import java.util.UUID;

/**
 * Exception levée lorsqu'une étude clinique n'est pas trouvée dans le tenant courant.
 *
 * <p>Mappée sur un HTTP 404 Not Found par le {@link GlobalExceptionHandler}.
 */
public class StudyNotFoundException extends RuntimeException {

    /**
     * Crée une exception avec un message descriptif incluant l'identifiant.
     *
     * @param studyId identifiant de l'étude non trouvée
     */
    public StudyNotFoundException(UUID studyId) {
        super("Étude clinique introuvable : " + studyId);
    }

    /**
     * Crée une exception avec un message personnalisé.
     *
     * @param message message descriptif de l'erreur
     */
    public StudyNotFoundException(String message) {
        super(message);
    }
}
