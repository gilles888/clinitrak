package be.clinitrak.exchange.domain.repository;

import be.clinitrak.exchange.domain.entity.ExchangeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les documents attachés aux demandes d'échange.
 */
@Repository
public interface ExchangeDocumentRepository extends JpaRepository<ExchangeDocument, UUID> {

    /**
     * Retourne tous les documents d'une demande d'échange.
     *
     * @param requestId UUID de la demande d'échange
     * @return liste des documents
     */
    List<ExchangeDocument> findByRequest_IdAndDeletedFalse(UUID requestId);
}
