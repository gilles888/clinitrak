package be.clinitrak.ethics.service;

import be.clinitrak.ethics.domain.entity.EthicsReview;
import be.clinitrak.ethics.domain.enums.ReviewDecision;
import be.clinitrak.ethics.domain.enums.ReviewType;
import be.clinitrak.ethics.domain.repository.EthicsReviewRepository;
import be.clinitrak.ethics.dto.DecisionUpdateRequest;
import be.clinitrak.ethics.dto.EthicsReviewCreateRequest;
import be.clinitrak.ethics.dto.EthicsReviewResponse;
import be.clinitrak.ethics.exception.EthicsException;
import be.clinitrak.ethics.exception.EthicsNotFoundException;
import be.clinitrak.ethics.mapper.EthicsMapper;
import be.clinitrak.ethics.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link EthicsReviewService}.
 *
 * <p>Utilise Mockito pour isoler le service de ses dépendances.
 * Le {@link TenantContext} est mocké statiquement pour simuler un tenant résolu.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EthicsReviewService — Tests unitaires")
class EthicsReviewServiceTest {

    @Mock
    private EthicsReviewRepository reviewRepository;

    @Mock
    private SequenceGeneratorService sequenceGeneratorService;

    @Mock
    private EthicsMapper ethicsMapper;

    @InjectMocks
    private EthicsReviewService reviewService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();
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
    // createReview
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createReview — devrait créer un avis avec un numéro CE généré")
    void createReview_shouldCreateReviewWithGeneratedNumber() {
        // given
        EthicsReviewCreateRequest request = new EthicsReviewCreateRequest(
            STUDY_ID, ReviewType.INITIAL, LocalDate.now(), "Dr. Dupont", null
        );

        EthicsReview review = buildMockReview();
        EthicsReviewResponse expectedResponse = buildMockResponse(review);

        when(ethicsMapper.fromCreateRequest(request)).thenReturn(review);
        when(sequenceGeneratorService.generateEthicsNumber(TENANT_ID)).thenReturn("2026/0001");
        when(reviewRepository.save(any(EthicsReview.class))).thenReturn(review);
        when(ethicsMapper.toResponse(review)).thenReturn(expectedResponse);

        // when
        EthicsReviewResponse result = reviewService.createReview(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.ethicsNumber()).isEqualTo("2026/0001");
        assertThat(result.decision()).isEqualTo(ReviewDecision.PENDING);
        verify(sequenceGeneratorService).generateEthicsNumber(TENANT_ID);
        verify(reviewRepository).save(any(EthicsReview.class));
    }

    // ----------------------------------------------------------------
    // getReview
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getReview — devrait retourner l'avis quand il existe")
    void getReview_shouldReturnReview_whenFound() {
        // given
        EthicsReview review = buildMockReview();
        EthicsReviewResponse expected = buildMockResponse(review);

        when(reviewRepository.findByIdAndTenantIdAndDeletedFalse(REVIEW_ID, TENANT_ID))
            .thenReturn(Optional.of(review));
        when(ethicsMapper.toResponse(review)).thenReturn(expected);

        // when
        EthicsReviewResponse result = reviewService.getReview(REVIEW_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(REVIEW_ID);
        verify(reviewRepository).findByIdAndTenantIdAndDeletedFalse(REVIEW_ID, TENANT_ID);
    }

    @Test
    @DisplayName("getReview — devrait lever EthicsNotFoundException quand l'avis n'existe pas")
    void getReview_shouldThrowEthicsNotFoundException_whenNotFound() {
        // given
        when(reviewRepository.findByIdAndTenantIdAndDeletedFalse(REVIEW_ID, TENANT_ID))
            .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> reviewService.getReview(REVIEW_ID))
            .isInstanceOf(EthicsNotFoundException.class)
            .hasMessageContaining(REVIEW_ID.toString());
    }

    // ----------------------------------------------------------------
    // updateDecision
    // ----------------------------------------------------------------

    @Test
    @DisplayName("updateDecision — PENDING→APPROVED devrait réussir")
    void updateDecision_shouldSucceed_whenTransitionFromPendingToApproved() {
        // given
        EthicsReview review = buildMockReview();
        review.setDecision(ReviewDecision.PENDING);

        DecisionUpdateRequest request = new DecisionUpdateRequest(
            ReviewDecision.APPROVED,
            LocalDate.now(),
            LocalDate.now(),
            LocalDate.now().plusYears(1),
            "Approuvé sans conditions"
        );

        EthicsReviewResponse expectedResponse = buildMockResponse(review);
        expectedResponse = new EthicsReviewResponse(
            expectedResponse.id(), expectedResponse.tenantId(), expectedResponse.studyId(),
            expectedResponse.ethicsNumber(), expectedResponse.reviewType(), expectedResponse.reviewTypeLabel(),
            expectedResponse.submissionDate(), request.reviewDate(),
            ReviewDecision.APPROVED, "Approuvé",
            request.decisionDate(), request.comments(),
            request.nextReviewDate(), expectedResponse.rapporteurName(),
            expectedResponse.reminderSent(), expectedResponse.createdAt(), expectedResponse.updatedAt()
        );

        when(reviewRepository.findByIdAndTenantIdAndDeletedFalse(REVIEW_ID, TENANT_ID))
            .thenReturn(Optional.of(review));
        when(reviewRepository.save(any(EthicsReview.class))).thenReturn(review);
        when(ethicsMapper.toResponse(review)).thenReturn(expectedResponse);

        // when
        EthicsReviewResponse result = reviewService.updateDecision(REVIEW_ID, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.decision()).isEqualTo(ReviewDecision.APPROVED);
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("updateDecision — devrait lever une exception si décision est PENDING")
    void updateDecision_shouldThrow_whenDecisionIsPending() {
        // given
        EthicsReview review = buildMockReview();
        review.setDecision(ReviewDecision.PENDING);

        DecisionUpdateRequest request = new DecisionUpdateRequest(
            ReviewDecision.PENDING, LocalDate.now(), null, null, null
        );

        when(reviewRepository.findByIdAndTenantIdAndDeletedFalse(REVIEW_ID, TENANT_ID))
            .thenReturn(Optional.of(review));

        // when / then
        assertThatThrownBy(() -> reviewService.updateDecision(REVIEW_ID, request))
            .isInstanceOf(EthicsException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("updateDecision — devrait lever une exception si l'avis est WITHDRAWN")
    void updateDecision_shouldThrow_whenReviewIsWithdrawn() {
        // given
        EthicsReview review = buildMockReview();
        review.setDecision(ReviewDecision.WITHDRAWN);

        DecisionUpdateRequest request = new DecisionUpdateRequest(
            ReviewDecision.APPROVED, LocalDate.now(), null, null, null
        );

        when(reviewRepository.findByIdAndTenantIdAndDeletedFalse(REVIEW_ID, TENANT_ID))
            .thenReturn(Optional.of(review));

        // when / then
        assertThatThrownBy(() -> reviewService.updateDecision(REVIEW_ID, request))
            .isInstanceOf(EthicsException.class)
            .hasMessageContaining("retiré");
    }

    // ----------------------------------------------------------------
    // getReviewsByStudy
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getReviewsByStudy — devrait retourner les avis de l'étude")
    void getReviewsByStudy_shouldReturnReviewsForStudy() {
        // given
        EthicsReview review = buildMockReview();
        EthicsReviewResponse response = buildMockResponse(review);

        when(reviewRepository.findByStudyIdAndTenantIdAndDeletedFalse(STUDY_ID, TENANT_ID))
            .thenReturn(List.of(review));
        when(ethicsMapper.toResponse(review)).thenReturn(response);

        // when
        List<EthicsReviewResponse> result = reviewService.getReviewsByStudy(STUDY_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studyId()).isEqualTo(STUDY_ID);
    }

    // ----------------------------------------------------------------
    // Tenant non résolu
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getReview — devrait lever EthicsException si tenant non résolu")
    void getReview_shouldThrow_whenTenantNotResolved() {
        // given
        mockedTenantContext.when(TenantContext::getTenantId).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> reviewService.getReview(REVIEW_ID))
            .isInstanceOf(EthicsException.class)
            .hasMessageContaining("Tenant non résolu");
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private EthicsReview buildMockReview() {
        EthicsReview review = new EthicsReview();
        review.setId(REVIEW_ID);
        review.setTenantId(TENANT_ID);
        review.setStudyId(STUDY_ID);
        review.setEthicsNumber("2026/0001");
        review.setReviewType(ReviewType.INITIAL);
        review.setSubmissionDate(LocalDate.now());
        review.setDecision(ReviewDecision.PENDING);
        review.setReminderSent(false);
        return review;
    }

    private EthicsReviewResponse buildMockResponse(EthicsReview review) {
        return new EthicsReviewResponse(
            review.getId(),
            review.getTenantId(),
            review.getStudyId(),
            review.getEthicsNumber(),
            review.getReviewType(),
            review.getReviewType() != null ? review.getReviewType().getLabel() : null,
            review.getSubmissionDate(),
            review.getReviewDate(),
            review.getDecision(),
            review.getDecision() != null ? review.getDecision().getLabel() : null,
            review.getDecisionDate(),
            review.getComments(),
            review.getNextReviewDate(),
            review.getRapporteurName(),
            review.isReminderSent(),
            Instant.now(),
            Instant.now()
        );
    }
}
