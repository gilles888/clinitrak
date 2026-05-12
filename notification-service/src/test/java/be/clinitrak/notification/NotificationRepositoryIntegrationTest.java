package be.clinitrak.notification;

import be.clinitrak.notification.domain.entity.Notification;
import be.clinitrak.notification.domain.enums.NotificationType;
import be.clinitrak.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration du repository de notifications avec TestContainers PostgreSQL.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinitrak_notification_test")
            .withUsername("clinitrak")
            .withPassword("clinitrak");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Vérifie que la persistance d'une notification fonctionne correctement.
     */
    @Test
    void shouldPersistNotification() {
        UUID userId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .recipientUserId(userId)
                .tenantId(UUID.randomUUID())
                .type(NotificationType.STUDY_STATUS_CHANGE)
                .subject("L'étude PROTO-001 a changé de statut")
                .message("L'étude PROTO-001 a changé de statut")
                .templateName("study-status-change")
                .build();

        Notification saved = notificationRepository.save(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.isEmailSent()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    /**
     * Vérifie la recherche paginée des notifications d'un utilisateur.
     */
    @Test
    void shouldFindNotificationsByUser() {
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            Notification n = Notification.builder()
                    .recipientUserId(userId)
                    .type(NotificationType.STOCK_ALERT)
                    .subject("Alerte stock " + i)
                    .message("Message " + i)
                    .build();
            notificationRepository.save(n);
        }

        // Notification d'un autre utilisateur (ne doit pas apparaître)
        notificationRepository.save(Notification.builder()
                .recipientUserId(UUID.randomUUID())
                .type(NotificationType.SYSTEM_ALERT)
                .subject("Autre utilisateur")
                .message("Autre")
                .build());

        Page<Notification> page = notificationRepository.findByRecipientUserId(
                userId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    /**
     * Vérifie le comptage des notifications non lues.
     */
    @Test
    void shouldCountUnreadNotifications() {
        UUID userId = UUID.randomUUID();

        // 2 non lues
        for (int i = 0; i < 2; i++) {
            notificationRepository.save(Notification.builder()
                    .recipientUserId(userId)
                    .type(NotificationType.MEETING_REMINDER)
                    .subject("Réunion " + i)
                    .message("Message")
                    .build());
        }

        // 1 lue
        Notification read = Notification.builder()
                .recipientUserId(userId)
                .type(NotificationType.TASK_ASSIGNED)
                .subject("Tâche assignée")
                .message("Message")
                .read(true)
                .build();
        notificationRepository.save(read);

        long unreadCount = notificationRepository
                .countByRecipientUserIdAndReadFalseAndDeletedAtIsNull(userId);

        assertThat(unreadCount).isEqualTo(2);
    }
}
