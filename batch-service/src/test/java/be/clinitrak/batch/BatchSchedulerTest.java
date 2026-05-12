package be.clinitrak.batch;

import be.clinitrak.batch.job.NightlyReminderJob;
import be.clinitrak.batch.job.WeeklyReportJob;
import be.clinitrak.batch.job.MonthlyBillingJob;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration du batch-service avec TestContainers PostgreSQL.
 * Vérifie que les beans Spring Batch sont correctement configurés.
 */
@SpringBatchTest
@SpringBootTest
@Testcontainers
class BatchSchedulerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinitrak_batch_test")
            .withUsername("clinitrak")
            .withPassword("clinitrak");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("clinitrak.services.notification-service.url", () -> "http://localhost:9999");
        registry.add("clinitrak.services.pharmacy-service.url", () -> "http://localhost:9998");
    }

    @Autowired
    @Qualifier(NightlyReminderJob.JOB_NAME)
    private Job nightlyReminderJob;

    @Autowired
    @Qualifier(WeeklyReportJob.JOB_NAME)
    private Job weeklyReportJob;

    @Autowired
    @Qualifier(MonthlyBillingJob.JOB_NAME)
    private Job monthlyBillingJob;

    /**
     * Vérifie que le job de rappel nocturne est correctement configuré.
     */
    @Test
    void nightlyReminderJobShouldBeConfigured() {
        assertThat(nightlyReminderJob).isNotNull();
        assertThat(nightlyReminderJob.getName()).isEqualTo(NightlyReminderJob.JOB_NAME);
    }

    /**
     * Vérifie que le job de rapport hebdomadaire est correctement configuré.
     */
    @Test
    void weeklyReportJobShouldBeConfigured() {
        assertThat(weeklyReportJob).isNotNull();
        assertThat(weeklyReportJob.getName()).isEqualTo(WeeklyReportJob.JOB_NAME);
    }

    /**
     * Vérifie que le job de facturation mensuelle est correctement configuré.
     */
    @Test
    void monthlyBillingJobShouldBeConfigured() {
        assertThat(monthlyBillingJob).isNotNull();
        assertThat(monthlyBillingJob.getName()).isEqualTo(MonthlyBillingJob.JOB_NAME);
    }
}
