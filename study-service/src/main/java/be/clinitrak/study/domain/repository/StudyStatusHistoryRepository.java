package be.clinitrak.study.domain.repository;

import be.clinitrak.study.domain.entity.StudyStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour l'historique des statuts d'études cliniques.
 */
@Repository
public interface StudyStatusHistoryRepository extends JpaRepository<StudyStatusHistory, UUID> {

    /**
     * Retourne l'historique de statut d'une étude, trié du plus récent au plus ancien.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return liste des entrées d'historique triée par date décroissante
     */
    List<StudyStatusHistory> findByStudyIdAndTenantIdOrderByStatusDateDesc(UUID studyId, UUID tenantId);
}
