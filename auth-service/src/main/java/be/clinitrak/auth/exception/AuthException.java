package be.clinitrak.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception métier pour les erreurs d'authentification.
 *
 * <p>Mappée automatiquement sur HTTP 401 par le {@link GlobalExceptionHandler}.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
