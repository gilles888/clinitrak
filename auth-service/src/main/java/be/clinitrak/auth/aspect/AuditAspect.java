package be.clinitrak.auth.aspect;

import be.clinitrak.auth.domain.entity.AuditLog;
import be.clinitrak.auth.domain.repository.AuditLogRepository;
import be.clinitrak.auth.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Aspect AOP qui trace automatiquement les appels aux méthodes des contrôleurs REST.
 *
 * <p>Pour chaque invocation de contrôleur, un {@link AuditLog} est persisté de façon
 * asynchrone afin de ne pas impacter la latence des endpoints.
 *
 * <p>La persistance est non-transactionnelle volontairement : on ne veut pas perdre
 * l'entrée d'audit en cas de rollback de la transaction métier.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    /** Coupe transversale : toutes les méthodes publiques des @RestController. */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {}

    /**
     * Intercepte tous les appels aux contrôleurs REST.
     *
     * <p>Persiste un log d'audit avec : utilisateur, tenant, action, résultat HTTP.
     *
     * @param joinPoint point de jointure AOP
     * @return résultat original de la méthode interceptée
     * @throws Throwable si la méthode interceptée lève une exception
     */
    @Around("restControllerMethods()")
    public Object auditControllerCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String action = joinPoint.getSignature().getDeclaringType().getSimpleName()
            + "." + joinPoint.getSignature().getName();

        AuditLog.Outcome outcome = AuditLog.Outcome.SUCCESS;
        Integer httpStatus = 200;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            outcome = AuditLog.Outcome.FAILURE;
            httpStatus = 500;
            throw e;
        } finally {
            persistAuditLog(action, outcome, httpStatus);
        }
    }

    /** Persiste le log de façon asynchrone pour éviter d'impacter la latence. */
    @Async
    public void persistAuditLog(String action, AuditLog.Outcome outcome, Integer httpStatus) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String tenantId = TenantContext.getTenantId();

            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .outcome(outcome)
                .httpStatus(httpStatus);

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String principal = auth.getName(); // "email:tenantId"
                if (principal.contains(":")) {
                    builder.userEmail(principal.split(":")[0]);
                }
            }

            if (tenantId != null) {
                try {
                    builder.tenantId(UUID.fromString(tenantId));
                } catch (IllegalArgumentException ignored) {}
            }

            // Extraction IP depuis la requête courante
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                builder.ipAddress(forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
                builder.userAgent(request.getHeader("User-Agent"));
            }

            auditLogRepository.save(builder.build());
        } catch (Exception e) {
            // Ne jamais laisser l'audit bloquer l'application
            log.error("Erreur lors de la persistance de l'audit log : {}", e.getMessage());
        }
    }
}
