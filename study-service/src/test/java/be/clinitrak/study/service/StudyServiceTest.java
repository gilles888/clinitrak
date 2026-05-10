package be.clinitrak.study.service;

import be.clinitrak.study.domain.entity.ClinicalStudy;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.enums.StudyType;
import be.clinitrak.study.domain.enums.SponsorType;
import be.clinitrak.study.domain.repository.ClinicalStudyRepository;
import be.clinitrak.study.domain.repository.PatientRepository;
import be.clinitrak.study.domain.repository.StudyContactRepository;
import be.clinitrak.study.domain.repository.StudyStatusHistoryRepository;
import be.clinitrak.study.domain.repository.SubmissionRepository;
import be.clinitrak.study.dto.*;
import be.clinitrak.study.exception.StudyException;
import be.clinitrak.study.exception.StudyNotFoundException;
import be.clinitrak.study.mapper.StudyMapper;
import be.clinitrak.study.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link StudyService}.
 *
 * <p>Utilise Mockito pour isoler le service de ses dépendances.
 * Le {@link TenantContext} est mocké statiquement pour simuler un tenant résolu.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudyService — Tests unitaires")
class StudyServiceTest {

    @Mock
    private ClinicalStudyRepository studyRepository;

    @Mock
    private StudyStatusHistoryRepository statusHistoryRepository;

    @Mock
    private StudyContactRepository contactRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private StudyMapper studyMapper;

    @InjectMocks
    private StudyService studyService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDY_ID = UUID.randomUUID();

    private MockedStatic<TenantContext> mockedTenantContext;

    @BeforeEach
    void setUp() {
        mockedTenantContext = mockStatic(TenantContext.class);
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(TENANT_ID.toString());
    }

    @AfterEach
    void tearDown() {
        mockedTenantContext.close();
    }

    // ----------------------------------------------------------------
    // createStudy
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createStudy — devrait créer une étude avec un numéro généré")
    void createStudy_shouldCreateStudyWithGeneratedNumber() {
        // given
        StudyCreateRequest request = new StudyCreateRequest(
            "Étude test oncologie",
            "ONCO-TEST",
            StudyType.INTERVENTIONAL,
            SponsorType.ACADEMIC,
            "CUSL",
            "Dr. Dupont",
            "Oncologie",
            null,
            null,
            null,
            100,
            true,
            "Description test",
            null,
            null,
            null
        );

        ClinicalStudy study = buildMockStudy();
        StudyResponse expectedResponse = buildMockStudyResponse(study);

        when(studyMapper.fromCreateRequest(request)).thenReturn(study);
        when(studyRepository.existsByEthicsNumberAndTenantId(any(), eq(TENANT_ID))).thenReturn(false);
        when(studyRepository.countByTenantIdAndYear(eq(TENANT_ID), anyInt())).thenReturn(0L);
        when(studyRepository.existsByStudyNumberAndTenantId(anyString(), eq(TENANT_ID))).thenReturn(false);
        when(studyRepository.save(any(ClinicalStudy.class))).thenReturn(study);
        when(statusHistoryRepository.save(any())).thenReturn(null);
        when(studyMapper.toResponse(study)).thenReturn(expectedResponse);

        // when
        StudyResponse result = studyService.createStudy(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(STUDY_ID);
        assertThat(result.studyNumber()).startsWith("ST-");
        verify(studyRepository).save(any(ClinicalStudy.class));
        verify(statusHistoryRepository).save(any());
    }

    @Test
    @DisplayName("createStudy — devrait lever StudyException si numéro éthique dupliqué")
    void createStudy_shouldThrowStudyException_whenEthicsNumberDuplicated() {
        // given
        StudyCreateRequest request = new StudyCreateRequest(
            "Étude dupliquée",
            null,
            StudyType.OBSERVATIONAL,
            SponsorType.COMMERCIAL,
            "Pharma SA",
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            "CE-2026-001",
            null,
            null
        );

        when(studyRepository.existsByEthicsNumberAndTenantId("CE-2026-001", TENANT_ID)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> studyService.createStudy(request))
            .isInstanceOf(StudyException.class)
            .hasMessageContaining("CE-2026-001");
    }

    // ----------------------------------------------------------------
    // getStudy
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getStudy — devrait retourner l'étude quand elle existe")
    void getStudy_shouldReturnStudy_whenFound() {
        // given
        ClinicalStudy study = buildMockStudy();
        StudyResponse expected = buildMockStudyResponse(study);

        when(studyRepository.findByIdAndTenantIdAndDeletedFalse(STUDY_ID, TENANT_ID))
            .thenReturn(Optional.of(study));
        when(studyMapper.toResponse(study)).thenReturn(expected);

        // when
        StudyResponse result = studyService.getStudy(STUDY_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(STUDY_ID);
        verify(studyRepository).findByIdAndTenantIdAndDeletedFalse(STUDY_ID, TENANT_ID);
    }

    @Test
    @DisplayName("getStudy — devrait lever StudyNotFoundException quand l'étude n'existe pas")
    void getStudy_shouldThrowStudyNotFoundException_whenNotFound() {
        // given
        when(studyRepository.findByIdAndTenantIdAndDeletedFalse(STUDY_ID, TENANT_ID))
            .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> studyService.getStudy(STUDY_ID))
            .isInstanceOf(StudyNotFoundException.class)
            .hasMessageContaining(STUDY_ID.toString());
    }

    // ----------------------------------------------------------------
    // searchStudies
    // ----------------------------------------------------------------

    @Test
    @DisplayName("searchStudies — devrait retourner une page de résumés")
    @SuppressWarnings("unchecked")
    void searchStudies_shouldReturnPageOfSummaries() {
        // given
        StudySearchCriteria criteria = new StudySearchCriteria(
            null, null, null, null, "onco", null, null, "Oncologie", null, null, null, null
        );
        Pageable pageable = Pageable.ofSize(20);

        ClinicalStudy study = buildMockStudy();
        StudySummaryResponse summary = new StudySummaryResponse(
            STUDY_ID, "ST-2026-00001", "Étude oncologie", "ONCO",
            StudyType.INTERVENTIONAL, StudyStatus.DRAFT, "Brouillon",
            SponsorType.ACADEMIC, "CUSL", "Dr. Dupont", null, null, null, 100, 0, true
        );

        Page<ClinicalStudy> studyPage = new PageImpl<>(List.of(study));
        when(studyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(studyPage);
        when(studyMapper.toSummary(study)).thenReturn(summary);

        // when
        Page<StudySummaryResponse> result = studyService.searchStudies(criteria, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).studyNumber()).isEqualTo("ST-2026-00001");
    }

    // ----------------------------------------------------------------
    // Tenant non résolu
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getStudy — devrait lever StudyException si tenant non résolu")
    void getStudy_shouldThrowStudyException_whenTenantNotResolved() {
        // given
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> studyService.getStudy(STUDY_ID))
            .isInstanceOf(StudyException.class)
            .hasMessageContaining("Tenant non résolu");
    }

    // ----------------------------------------------------------------
    // deleteStudy
    // ----------------------------------------------------------------

    @Test
    @DisplayName("deleteStudy — devrait marquer l'étude comme supprimée")
    void deleteStudy_shouldMarkStudyAsDeleted() {
        // given
        ClinicalStudy study = buildMockStudy();
        when(studyRepository.findByIdAndTenantIdAndDeletedFalse(STUDY_ID, TENANT_ID))
            .thenReturn(Optional.of(study));
        when(studyRepository.save(study)).thenReturn(study);

        // when
        studyService.deleteStudy(STUDY_ID);

        // then
        assertThat(study.isDeleted()).isTrue();
        verify(studyRepository).save(study);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ClinicalStudy buildMockStudy() {
        ClinicalStudy study = new ClinicalStudy();
        study.setId(STUDY_ID);
        study.setTenantId(TENANT_ID);
        study.setStudyNumber("ST-2026-00001");
        study.setTitle("Étude oncologie test");
        study.setStudyType(StudyType.INTERVENTIONAL);
        study.setSponsorType(SponsorType.ACADEMIC);
        study.setCurrentStatus(StudyStatus.DRAFT);
        study.setCurrentEnrollment(0);
        study.setSponsorCusl(true);
        return study;
    }

    private StudyResponse buildMockStudyResponse(ClinicalStudy study) {
        return new StudyResponse(
            study.getId(),
            study.getTenantId(),
            study.getStudyNumber(),
            null, null, null,
            study.getTitle(),
            null,
            study.getStudyType(),
            study.getStudyType().getLabel(),
            study.getSponsorType(),
            study.getSponsorType().getLabel(),
            null, null, null, null, null,
            study.getCurrentStatus(),
            study.getCurrentStatus().getLabel(),
            null, null, null,
            study.getTargetEnrollment(),
            study.getCurrentEnrollment(),
            study.isSponsorCusl(),
            null,
            Instant.now(),
            Instant.now(),
            "test@clinitrak.be"
        );
    }
}
