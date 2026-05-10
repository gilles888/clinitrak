package be.clinitrak.study.repository;

import be.clinitrak.study.domain.entity.ClinicalStudy;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.StudyType;
import be.clinitrak.study.domain.enums.SponsorType;
import be.clinitrak.study.domain.repository.ClinicalStudyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour {@link ClinicalStudyRepository}.
 *
 * <p>Utilise TestContainers pour démarrer un PostgreSQL réel.
 * Spring Boot @DataJpaTest configure uniquement la couche JPA.
 */
@DataJpaTest
@Testcontainers
@DisplayName("ClinicalStudyRepository — Tests d'intégration")
class StudyRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("clinitrak_study_test")
        .withUsername("clinitrak_test")
        .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Désactive Liquibase pour les tests DataJpa (utilise ddl-auto=create)
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ClinicalStudyRepository studyRepository;

    @Test
    @DisplayName("save + findById — devrait persister et retrouver une étude")
    void saveAndFindById_shouldPersistAndReturnStudy() {
        // given
        UUID tenantId = UUID.randomUUID();
        ClinicalStudy study = buildStudy(tenantId, "ST-2026-00001");

        // when
        ClinicalStudy saved = studyRepository.save(study);
        Optional<ClinicalStudy> found = studyRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStudyNumber()).isEqualTo("ST-2026-00001");
        assertThat(found.get().getTenantId()).isEqualTo(tenantId);
        assertThat(found.get().getCurrentStatus()).isEqualTo(StudyStatus.DRAFT);
    }

    @Test
    @DisplayName("findByIdAndTenantIdAndDeletedFalse — devrait isoler par tenant")
    void findByIdAndTenantId_shouldIsolateByTenant() {
        // given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        ClinicalStudy study1 = buildStudy(tenant1, "ST-2026-00001");
        ClinicalStudy study2 = buildStudy(tenant2, "ST-2026-00001");

        ClinicalStudy saved1 = studyRepository.save(study1);
        studyRepository.save(study2);

        // when - recherche avec le bon tenant
        Optional<ClinicalStudy> found = studyRepository.findByIdAndTenantIdAndDeletedFalse(saved1.getId(), tenant1);
        // when - recherche avec le mauvais tenant
        Optional<ClinicalStudy> notFound = studyRepository.findByIdAndTenantIdAndDeletedFalse(saved1.getId(), tenant2);

        // then
        assertThat(found).isPresent();
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("soft delete — devrait masquer les études supprimées")
    void softDelete_shouldHideDeletedStudies() {
        // given
        UUID tenantId = UUID.randomUUID();
        ClinicalStudy study = buildStudy(tenantId, "ST-2026-00002");
        ClinicalStudy saved = studyRepository.save(study);

        // when - vérification avant suppression
        Optional<ClinicalStudy> beforeDelete =
            studyRepository.findByIdAndTenantIdAndDeletedFalse(saved.getId(), tenantId);
        assertThat(beforeDelete).isPresent();

        // Soft delete
        saved.setDeleted(true);
        studyRepository.save(saved);

        // when - vérification après suppression
        Optional<ClinicalStudy> afterDelete =
            studyRepository.findByIdAndTenantIdAndDeletedFalse(saved.getId(), tenantId);

        // then
        assertThat(afterDelete).isEmpty();
        // Mais l'entité existe toujours en base
        Optional<ClinicalStudy> stillInDb = studyRepository.findById(saved.getId());
        assertThat(stillInDb).isPresent();
        assertThat(stillInDb.get().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("countByTenantIdAndCurrentStatusAndDeletedFalse — devrait compter par statut")
    void countByStatus_shouldCountActiveStudiesByStatus() {
        // given
        UUID tenantId = UUID.randomUUID();

        ClinicalStudy draft1 = buildStudy(tenantId, "ST-2026-00003");
        ClinicalStudy draft2 = buildStudy(tenantId, "ST-2026-00004");
        ClinicalStudy ongoing = buildStudy(tenantId, "ST-2026-00005");
        ongoing.setCurrentStatus(StudyStatus.ONGOING);

        studyRepository.save(draft1);
        studyRepository.save(draft2);
        studyRepository.save(ongoing);

        // when
        long draftCount = studyRepository.countByTenantIdAndCurrentStatusAndDeletedFalse(tenantId, StudyStatus.DRAFT);
        long ongoingCount = studyRepository.countByTenantIdAndCurrentStatusAndDeletedFalse(tenantId, StudyStatus.ONGOING);

        // then
        assertThat(draftCount).isEqualTo(2);
        assertThat(ongoingCount).isEqualTo(1);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private ClinicalStudy buildStudy(UUID tenantId, String studyNumber) {
        ClinicalStudy study = new ClinicalStudy();
        study.setTenantId(tenantId);
        study.setStudyNumber(studyNumber);
        study.setTitle("Étude test " + studyNumber);
        study.setStudyType(StudyType.INTERVENTIONAL);
        study.setSponsorType(SponsorType.ACADEMIC);
        study.setCurrentStatus(StudyStatus.DRAFT);
        study.setCurrentEnrollment(0);
        study.setSponsorCusl(false);
        return study;
    }
}
