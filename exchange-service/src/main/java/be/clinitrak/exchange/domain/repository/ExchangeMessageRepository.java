package be.clinitrak.exchange.domain.repository;

import be.clinitrak.exchange.domain.entity.ExchangeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les messages de demandes d'échange.
 */
@Repository
public interface ExchangeMessageRepository extends JpaRepository<ExchangeMessage, UUID> {

    /**
     * Retourne tous les messages d'une demande pour un tenant, triés par date d'envoi.
     *
     * @param requestId UUID de la demande d'échange
     * @param tenantId  identifiant du tenant
     * @return liste de messages triés par {@code sentAt} croissant
     */
    List<ExchangeMessage> findByRequest_IdAndTenantIdAndDeletedFalseOrderBySentAtAsc(UUID requestId, String tenantId);
}
