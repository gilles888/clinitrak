package be.clinitrak.exchange.domain.repository;

import be.clinitrak.exchange.domain.entity.ExchangeRequest;
import be.clinitrak.exchange.domain.enums.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les demandes d'échange.
 */
@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, UUID> {

    /**
     * Retourne toutes les demandes d'un utilisateur externe pour un tenant donné.
     *
     * @param externalUserId UUID de l'utilisateur externe
     * @param tenantId       identifiant du tenant
     * @return liste des demandes
     */
    List<ExchangeRequest> findByExternalUser_IdAndTenantIdAndDeletedFalse(UUID externalUserId, String tenantId);

    /**
     * Retourne toutes les demandes d'un tenant donné (vue interne).
     *
     * @param tenantId identifiant du tenant
     * @return liste de toutes les demandes du tenant
     */
    List<ExchangeRequest> findByTenantIdAndDeletedFalse(String tenantId);

    /**
     * Recherche une demande par ID en vérifiant l'appartenance au tenant.
     *
     * @param id       UUID de la demande
     * @param tenantId identifiant du tenant
     * @return demande ou empty
     */
    Optional<ExchangeRequest> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    /**
     * Retourne les demandes par statut pour un tenant.
     *
     * @param status   statut à filtrer
     * @param tenantId identifiant du tenant
     * @return liste des demandes au statut donné
     */
    List<ExchangeRequest> findByStatusAndTenantIdAndDeletedFalse(ExchangeStatus status, String tenantId);
}
