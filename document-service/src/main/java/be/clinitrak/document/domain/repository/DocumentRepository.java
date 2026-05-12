package be.clinitrak.document.domain.repository;

import be.clinitrak.document.domain.entity.Document;
import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.domain.enums.ModuleSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité {@link Document}.
 * Fournit les requêtes de recherche multi-critères et de versioning documentaire.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Recherche paginée de documents non supprimés par étude et type.
     *
     * @param studyId identifiant de l'étude (peut être null)
     * @param type    type de document (peut être null)
     * @param pageable paramètres de pagination
     * @return page de documents correspondant aux critères
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.deletedAt IS NULL
            AND (:studyId IS NULL OR d.studyId = :studyId)
            AND (:type IS NULL OR d.documentType = :type)
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findAllByCriteria(
            @Param("studyId") UUID studyId,
            @Param("type") DocumentType type,
            Pageable pageable);

    /**
     * Recherche paginée de documents non supprimés par module source.
     *
     * @param moduleSource module fonctionnel source
     * @param pageable     paramètres de pagination
     * @return page de documents du module donné
     */
    Page<Document> findByModuleSourceAndDeletedAtIsNull(ModuleSource moduleSource, Pageable pageable);

    /**
     * Recherche un document non supprimé par son identifiant.
     *
     * @param id identifiant du document
     * @return Optional contenant le document s'il existe et n'est pas supprimé
     */
    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Document> findActiveById(@Param("id") UUID id);

    /**
     * Récupère l'historique complet des versions d'un document.
     * Remonte la chaîne depuis le document racine jusqu'à la version actuelle.
     *
     * @param rootId identifiant du document racine (version 1) ou de n'importe quelle version
     * @return liste de toutes les versions triées par numéro de version croissant
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.id = :rootId
               OR d.parentDocumentId = :rootId
            ORDER BY d.version ASC
            """)
    List<Document> findVersionHistory(@Param("rootId") UUID rootId);

    /**
     * Trouve la version la plus récente d'un document à partir d'un document parent.
     *
     * @param parentDocumentId identifiant du document parent
     * @return Optional contenant le document le plus récent
     */
    Optional<Document> findTopByParentDocumentIdOrderByVersionDesc(UUID parentDocumentId);

    /**
     * Compte le nombre de documents actifs associés à une étude.
     *
     * @param studyId identifiant de l'étude
     * @return nombre de documents actifs
     */
    long countByStudyIdAndDeletedAtIsNull(UUID studyId);
}
