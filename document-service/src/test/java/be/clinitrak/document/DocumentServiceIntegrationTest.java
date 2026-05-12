package be.clinitrak.document;

import be.clinitrak.document.domain.entity.Document;
import be.clinitrak.document.domain.enums.DocumentType;
import be.clinitrak.document.domain.enums.ModuleSource;
import be.clinitrak.document.domain.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration du document-service avec TestContainers PostgreSQL.
 * Vérifie les opérations JPA : persistance, recherche et soft-delete.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinitrak_document_test")
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
    private DocumentRepository documentRepository;

    /**
     * Vérifie que la persistance d'un document fonctionne correctement.
     */
    @Test
    void shouldPersistDocument() {
        Document doc = Document.builder()
                .fileName("abc123.pdf")
                .originalFileName("rapport-annuel.pdf")
                .contentType("application/pdf")
                .fileSize(102400L)
                .bucketName("clinitrak-documents")
                .objectKey("study-001/PDF/abc123.pdf")
                .documentType(DocumentType.PDF)
                .moduleSource(ModuleSource.STUDY)
                .studyId(UUID.randomUUID())
                .version(1)
                .uploadedBy("investigator@hopital.be")
                .description("Rapport annuel de l'étude")
                .build();

        Document saved = documentRepository.save(doc);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getBucketName()).isEqualTo("clinitrak-documents");
    }

    /**
     * Vérifie que le soft-delete masque le document des recherches actives.
     */
    @Test
    void shouldSoftDeleteDocument() {
        Document doc = Document.builder()
                .fileName("test.pdf")
                .originalFileName("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .bucketName("clinitrak-documents")
                .objectKey("global/PDF/test.pdf")
                .documentType(DocumentType.PDF)
                .moduleSource(ModuleSource.ETHICS)
                .version(1)
                .uploadedBy("admin@hopital.be")
                .build();

        Document saved = documentRepository.save(doc);
        assertThat(documentRepository.findActiveById(saved.getId())).isPresent();

        saved.softDelete();
        documentRepository.save(saved);

        assertThat(documentRepository.findActiveById(saved.getId())).isEmpty();
    }

    /**
     * Vérifie la recherche multi-critères (studyId + type).
     */
    @Test
    void shouldFindDocumentsByCriteria() {
        UUID studyId = UUID.randomUUID();

        Document doc1 = Document.builder()
                .fileName("d1.pdf").originalFileName("d1.pdf")
                .contentType("application/pdf").fileSize(100L)
                .bucketName("clinitrak-documents").objectKey("s/PDF/d1.pdf")
                .documentType(DocumentType.PDF).moduleSource(ModuleSource.STUDY)
                .studyId(studyId).version(1).uploadedBy("user@test.be").build();

        Document doc2 = Document.builder()
                .fileName("d2.xlsx").originalFileName("d2.xlsx")
                .contentType("application/vnd.ms-excel").fileSize(200L)
                .bucketName("clinitrak-documents").objectKey("s/XLSX/d2.xlsx")
                .documentType(DocumentType.XLSX).moduleSource(ModuleSource.STUDY)
                .studyId(studyId).version(1).uploadedBy("user@test.be").build();

        documentRepository.saveAll(List.of(doc1, doc2));

        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var pdfDocs = documentRepository.findAllByCriteria(studyId, DocumentType.PDF, pageable);
        assertThat(pdfDocs.getTotalElements()).isEqualTo(1);
        assertThat(pdfDocs.getContent().get(0).getFileName()).isEqualTo("d1.pdf");

        var allDocs = documentRepository.findAllByCriteria(studyId, null, pageable);
        assertThat(allDocs.getTotalElements()).isEqualTo(2);
    }
}
