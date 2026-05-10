package be.clinitrak.ctc.repository;

import be.clinitrak.ctc.domain.entity.QualityEvent;
import be.clinitrak.ctc.domain.enums.EventStatus;
import be.clinitrak.ctc.domain.enums.EventType;
import be.clinitrak.ctc.domain.enums.Severity;
import be.clinitrak.ctc.domain.repository.QualityEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour {@link QualityEventRepository} avec PostgreSQL réel.
 *
 * <p>Utilise TestContainers pour démarrer une instance PostgreSQL de test.
 * Vérifie les requêtes de filtrage par tenant et par statut.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("QualityEventRepository — Tests d'intégration")
class QualityEventRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("clinitrak_ctc_test")
        .withUsername("clinitrak")
        .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private QualityEventRepository repository;

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String STUDY_ID = "study-001";

    @Test
    @DisplayName("save — devrait persister un événement qualité avec ses champs")
    void save_shouldPersistQualityEvent() {
        // given
        QualityEvent event = buildEvent(TENANT_A, EventStatus.OPEN, Severity.HIGH);

        // when
        QualityEvent saved = repository.save(event);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyId()).isEqualTo(STUDY_ID);
        assertThat(saved.getEventType()).isEqualTo(EventType.DEVIATION);
        assertThat(saved.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("findByTenantId — devrait retourner uniquement les événements du tenant")
    void findByTenantId_shouldReturnOnlyTenantEvents() {
        // given
        repository.save(buildEvent(TENANT_A, EventStatus.OPEN, Severity.MEDIUM));
        repository.save(buildEvent(TENANT_A, EventStatus.IN_PROGRESS, Severity.LOW));
        repository.save(buildEvent(TENANT_B, EventStatus.OPEN, Severity.HIGH));

        // when
        List<QualityEvent> tenantAEvents = repository.findByTenantIdAndDeletedFalse(TENANT_A);

        // then
        assertThat(tenantAEvents).hasSize(2);
        assertThat(tenantAEvents).allMatch(e -> TENANT_A.equals(e.getTenantId()));
    }

    @Test
    @DisplayName("findByStatus — devrait filtrer par statut correctement")
    void findByStatus_shouldFilterCorrectly() {
        // given
        repository.save(buildEvent(TENANT_A, EventStatus.OPEN, Severity.MEDIUM));
        repository.save(buildEvent(TENANT_A, EventStatus.OPEN, Severity.HIGH));
        repository.save(buildEvent(TENANT_A, EventStatus.CLOSED, Severity.LOW));

        // when
        List<QualityEvent> openEvents = repository.findByTenantIdAndStatusAndDeletedFalse(TENANT_A, EventStatus.OPEN);
        List<QualityEvent> closedEvents = repository.findByTenantIdAndStatusAndDeletedFalse(TENANT_A, EventStatus.CLOSED);

        // then
        assertThat(openEvents).hasSize(2);
        assertThat(openEvents).allMatch(e -> EventStatus.OPEN.equals(e.getStatus()));
        assertThat(closedEvents).hasSize(1);
        assertThat(closedEvents.get(0).getStatus()).isEqualTo(EventStatus.CLOSED);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private QualityEvent buildEvent(String tenantId, EventStatus status, Severity severity) {
        QualityEvent event = new QualityEvent();
        event.setStudyId(STUDY_ID);
        event.setEventType(EventType.DEVIATION);
        event.setEventDate(LocalDate.now());
        event.setSeverity(severity);
        event.setDescription("Déviation au protocole détectée lors de la visite");
        event.setStatus(status);
        event.setTenantId(tenantId);
        return event;
    }
}
