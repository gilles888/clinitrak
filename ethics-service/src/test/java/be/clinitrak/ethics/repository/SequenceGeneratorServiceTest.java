package be.clinitrak.ethics.repository;

import be.clinitrak.ethics.domain.repository.EthicsSequenceRepository;
import be.clinitrak.ethics.service.SequenceGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour {@link SequenceGeneratorService} avec PostgreSQL réel.
 *
 * <p>Utilise TestContainers pour démarrer une instance PostgreSQL de test.
 * Vérifie l'unicité et le format des numéros CE générés.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SequenceGeneratorService.class)
@DisplayName("SequenceGeneratorService — Tests d'intégration")
class SequenceGeneratorServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("clinitrak_ethics_test")
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
    private SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private EthicsSequenceRepository sequenceRepository;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    @DisplayName("Première séquence — devrait créer la séquence et retourner /0001")
    void generateEthicsNumber_firstCall_shouldReturn0001() {
        // when
        String number = sequenceGeneratorService.generateEthicsNumber(TENANT_ID);

        // then
        assertThat(number).matches("\\d{4}/0001");
        assertThat(sequenceRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deuxième séquence — devrait incrémenter à /0002")
    void generateEthicsNumber_secondCall_shouldReturn0002() {
        // given
        UUID tenant2 = UUID.randomUUID(); // Tenant différent pour isolation

        // when
        String first  = sequenceGeneratorService.generateEthicsNumber(tenant2);
        String second = sequenceGeneratorService.generateEthicsNumber(tenant2);

        // then
        assertThat(first).endsWith("/0001");
        assertThat(second).endsWith("/0002");
    }

    @Test
    @DisplayName("Séquences différentes par tenant — chaque tenant a son propre compteur")
    void generateEthicsNumber_differentTenants_shouldHaveSeparateCounters() {
        // given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        // when
        String number1 = sequenceGeneratorService.generateEthicsNumber(tenant1);
        String number2 = sequenceGeneratorService.generateEthicsNumber(tenant2);
        String number3 = sequenceGeneratorService.generateEthicsNumber(tenant1);

        // then
        assertThat(number1).endsWith("/0001");
        assertThat(number2).endsWith("/0001"); // Tenant 2 commence à 0001
        assertThat(number3).endsWith("/0002"); // Tenant 1 continue à 0002
    }

    @Test
    @DisplayName("Format correct — doit correspondre à AAAA/NNNN (4 chiffres)")
    void generateEthicsNumber_shouldHaveCorrectFormat() {
        // given
        UUID tenant = UUID.randomUUID();

        // when
        String number = sequenceGeneratorService.generateEthicsNumber(tenant);

        // then
        assertThat(number).matches("\\d{4}/\\d{4}");
        String[] parts = number.split("/");
        assertThat(parts).hasSize(2);
        assertThat(parts[1]).hasSize(4); // Exactement 4 chiffres
    }
}
