package be.clinitrak.study.service;

import be.clinitrak.study.domain.entity.*;
import be.clinitrak.study.domain.enums.StudyStatus;
import be.clinitrak.study.domain.repository.*;
import be.clinitrak.study.dto.*;
import be.clinitrak.study.exception.StudyException;
import be.clinitrak.study.exception.StudyNotFoundException;
import be.clinitrak.study.mapper.StudyMapper;
import be.clinitrak.study.specification.ClinicalStudySpecification;
import be.clinitrak.study.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des études cliniques.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}. Si le tenant n'est pas résolu,
 * une {@link StudyException} est levée immédiatement.
 *
 * <p>La numérotation des études suit le format {@code ST-{YYYY}-{seq5}}
 * (ex: {@code ST-2026-00001}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private final ClinicalStudyRepository studyRepository;
    private final StudyStatusHistoryRepository statusHistoryRepository;
    private final StudyContactRepository contactRepository;
    private final SubmissionRepository submissionRepository;
    private final PatientRepository patientRepository;
    private final StudyMapper studyMapper;

    // ----------------------------------------------------------------
    // Études cliniques
    // ----------------------------------------------------------------

    /**
     * Recherche des études cliniques avec filtrage multicritères et pagination.
     *
     * @param criteria critères de recherche (tous optionnels)
     * @param pageable paramètres de pagination et tri
     * @return page de résumés d'études
     * @throws StudyException si le tenant courant n'est pas résolu
     */
    public Page<StudySummaryResponse> searchStudies(StudySearchCriteria criteria, Pageable pageable) {
        UUID tenantId = resolveTenantId();
        log.debug("Recherche d'études pour le tenant {} avec critères : {}", tenantId, criteria);

        var spec = ClinicalStudySpecification.buildFrom(criteria, tenantId);
        return studyRepository.findAll(spec, pageable)
            .map(studyMapper::toSummary);
    }

    /**
     * Récupère une étude clinique complète par son identifiant.
     *
     * @param studyId identifiant UUID de l'étude
     * @return réponse complète de l'étude
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant courant n'est pas résolu
     */
    public StudyResponse getStudy(UUID studyId) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);
        return studyMapper.toResponse(study);
    }

    /**
     * Crée une nouvelle étude clinique.
     *
     * <p>Le numéro d'étude est généré automatiquement au format {@code ST-{YYYY}-{seq5}}
     * si le titre ou le numéro n'est pas fourni.
     *
     * @param request données de création de l'étude
     * @return réponse complète de l'étude créée
     * @throws StudyException si le tenant n'est pas résolu ou si le numéro éthique est dupliqué
     */
    @Transactional
    public StudyResponse createStudy(StudyCreateRequest request) {
        UUID tenantId = resolveTenantId();

        // Vérifier la déduplication du numéro éthique si fourni
        if (request.ethicsNumber() != null && !request.ethicsNumber().isBlank()) {
            if (studyRepository.existsByEthicsNumberAndTenantId(request.ethicsNumber(), tenantId)) {
                throw new StudyException("Un numéro éthique identique existe déjà : " + request.ethicsNumber());
            }
        }

        ClinicalStudy study = studyMapper.fromCreateRequest(request);
        study.setTenantId(tenantId);
        study.setCurrentStatus(StudyStatus.DRAFT);
        study.setCurrentEnrollment(0);

        // Génération du numéro d'étude
        String studyNumber = generateStudyNumber(tenantId);
        study.setStudyNumber(studyNumber);

        ClinicalStudy saved = studyRepository.save(study);

        // Crée l'entrée initiale dans l'historique de statut
        createStatusHistoryEntry(saved, StudyStatus.DRAFT, LocalDate.now(), "Création de l'étude", tenantId);

        log.info("Étude créée : {} (tenant: {})", saved.getStudyNumber(), tenantId);
        return studyMapper.toResponse(saved);
    }

    /**
     * Met à jour les informations d'une étude clinique existante.
     *
     * @param studyId identifiant de l'étude à mettre à jour
     * @param request données de mise à jour
     * @return réponse complète de l'étude mise à jour
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    @Transactional
    public StudyResponse updateStudy(UUID studyId, StudyUpdateRequest request) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);

        studyMapper.updateFromRequest(request, study);

        ClinicalStudy saved = studyRepository.save(study);
        log.info("Étude mise à jour : {} (tenant: {})", saved.getStudyNumber(), tenantId);
        return studyMapper.toResponse(saved);
    }

    /**
     * Met à jour le statut réglementaire d'une étude et trace l'historique.
     *
     * @param studyId identifiant de l'étude
     * @param request nouveau statut avec date et commentaire
     * @return réponse complète de l'étude avec le nouveau statut
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    @Transactional
    public StudyResponse updateStatus(UUID studyId, StatusUpdateRequest request) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);

        StudyStatus previousStatus = study.getCurrentStatus();
        study.setCurrentStatus(request.status());

        // Mise à jour de la date d'approbation si applicable
        if (request.status() == StudyStatus.APPROVED) {
            study.setApprovalDate(request.statusDate());
        }

        ClinicalStudy saved = studyRepository.save(study);

        // Trace le changement dans l'historique
        String currentUser = getCurrentUsername();
        createStatusHistoryEntry(saved, request.status(), request.statusDate(), request.comment(), tenantId);

        log.info("Statut de l'étude {} : {} → {} (tenant: {})",
            saved.getStudyNumber(), previousStatus, request.status(), tenantId);
        return studyMapper.toResponse(saved);
    }

    /**
     * Supprime logiquement (soft delete) une étude clinique.
     *
     * @param studyId identifiant de l'étude à supprimer
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    @Transactional
    public void deleteStudy(UUID studyId) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);
        study.setDeleted(true);
        studyRepository.save(study);
        log.info("Étude supprimée (soft delete) : {} (tenant: {})", study.getStudyNumber(), tenantId);
    }

    // ----------------------------------------------------------------
    // Historique de statut
    // ----------------------------------------------------------------

    /**
     * Retourne l'historique complet des changements de statut d'une étude.
     *
     * @param studyId identifiant de l'étude
     * @return liste des entrées d'historique triée du plus récent au plus ancien
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    public List<StudyStatusHistory> getStatusHistory(UUID studyId) {
        UUID tenantId = resolveTenantId();
        // Vérifie que l'étude existe et appartient au tenant
        findStudyOrThrow(studyId, tenantId);
        return statusHistoryRepository.findByStudyIdAndTenantIdOrderByStatusDateDesc(studyId, tenantId);
    }

    // ----------------------------------------------------------------
    // Contacts
    // ----------------------------------------------------------------

    /**
     * Retourne la liste des contacts actifs d'une étude.
     *
     * @param studyId identifiant de l'étude
     * @return liste des contacts actifs
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    public List<ContactResponse> getContacts(UUID studyId) {
        UUID tenantId = resolveTenantId();
        findStudyOrThrow(studyId, tenantId);
        return contactRepository.findByStudyIdAndTenantIdAndActiveTrue(studyId, tenantId)
            .stream()
            .map(studyMapper::toContactResponse)
            .collect(Collectors.toList());
    }

    /**
     * Ajoute un contact à une étude clinique.
     *
     * @param studyId identifiant de l'étude
     * @param request données du contact à ajouter
     * @return réponse complète du contact créé
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    @Transactional
    public ContactResponse addContact(UUID studyId, ContactRequest request) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);

        StudyContact contact = studyMapper.fromContactRequest(request);
        contact.setStudy(study);
        contact.setTenantId(tenantId);
        contact.setActive(true);

        StudyContact saved = contactRepository.save(contact);
        log.debug("Contact ajouté à l'étude {} : {} {}", study.getStudyNumber(),
            contact.getFirstName(), contact.getLastName());
        return studyMapper.toContactResponse(saved);
    }

    /**
     * Désactive (suppression logique) un contact d'une étude.
     *
     * @param studyId   identifiant de l'étude
     * @param contactId identifiant du contact à supprimer
     * @throws StudyNotFoundException si l'étude ou le contact n'est pas trouvé
     * @throws StudyException         si le tenant n'est pas résolu
     */
    @Transactional
    public void removeContact(UUID studyId, UUID contactId) {
        UUID tenantId = resolveTenantId();
        findStudyOrThrow(studyId, tenantId);

        StudyContact contact = contactRepository.findByIdAndStudyIdAndTenantId(contactId, studyId, tenantId)
            .orElseThrow(() -> new StudyNotFoundException("Contact introuvable : " + contactId));

        contact.setActive(false);
        contactRepository.save(contact);
        log.debug("Contact désactivé : {} pour étude : {}", contactId, studyId);
    }

    // ----------------------------------------------------------------
    // Soumissions
    // ----------------------------------------------------------------

    /**
     * Retourne les soumissions d'une étude avec pagination.
     *
     * @param studyId  identifiant de l'étude
     * @param pageable paramètres de pagination et tri
     * @return page de soumissions
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    public Page<SubmissionResponse> getSubmissions(UUID studyId, Pageable pageable) {
        UUID tenantId = resolveTenantId();
        findStudyOrThrow(studyId, tenantId);
        return submissionRepository.findByStudyIdAndTenantId(studyId, tenantId, pageable)
            .map(studyMapper::toSubmissionResponse);
    }

    /**
     * Ajoute une soumission réglementaire à une étude.
     *
     * @param studyId identifiant de l'étude
     * @param request données de la soumission
     * @return réponse complète de la soumission créée
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    @Transactional
    public SubmissionResponse addSubmission(UUID studyId, SubmissionRequest request) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);

        Submission submission = studyMapper.fromSubmissionRequest(request);
        submission.setStudy(study);
        submission.setTenantId(tenantId);

        Submission saved = submissionRepository.save(submission);
        log.debug("Soumission {} ajoutée à l'étude {}", submission.getSubmissionType(), study.getStudyNumber());
        return studyMapper.toSubmissionResponse(saved);
    }

    // ----------------------------------------------------------------
    // Patients
    // ----------------------------------------------------------------

    /**
     * Retourne les patients d'une étude avec pagination.
     *
     * @param studyId  identifiant de l'étude
     * @param pageable paramètres de pagination et tri
     * @return page de patients
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le tenant n'est pas résolu
     */
    public Page<PatientResponse> getPatients(UUID studyId, Pageable pageable) {
        UUID tenantId = resolveTenantId();
        findStudyOrThrow(studyId, tenantId);
        return patientRepository.findByStudyIdAndTenantId(studyId, tenantId, pageable)
            .map(studyMapper::toPatientResponse);
    }

    /**
     * Ajoute un patient pseudonymisé à une étude clinique.
     *
     * <p>Le code patient doit être unique au sein de l'étude.
     * Le compteur d'inclusion ({@code currentEnrollment}) est mis à jour automatiquement.
     *
     * @param studyId identifiant de l'étude
     * @param request données du patient (code pseudonyme RGPD)
     * @return réponse complète du patient créé
     * @throws StudyNotFoundException si l'étude n'existe pas dans le tenant courant
     * @throws StudyException         si le code patient est dupliqué dans l'étude
     */
    @Transactional
    public PatientResponse addPatient(UUID studyId, PatientRequest request) {
        UUID tenantId = resolveTenantId();
        ClinicalStudy study = findStudyOrThrow(studyId, tenantId);

        // Vérifie l'unicité du code patient dans l'étude
        if (patientRepository.existsByPatientCodeAndStudyId(request.patientCode(), studyId)) {
            throw new StudyException("Le code patient '" + request.patientCode() + "' existe déjà dans cette étude");
        }

        Patient patient = studyMapper.fromPatientRequest(request);
        patient.setStudy(study);
        patient.setTenantId(tenantId);

        if (patient.getStatus() == null) {
            patient.setStatus(be.clinitrak.study.domain.enums.PatientStatus.SCREENED);
        }

        Patient saved = patientRepository.save(patient);

        // Met à jour le compteur d'inclusion de l'étude
        long patientCount = patientRepository.countByStudyIdAndTenantId(studyId, tenantId);
        study.setCurrentEnrollment((int) patientCount);
        studyRepository.save(study);

        log.debug("Patient {} ajouté à l'étude {} (total: {})",
            request.patientCode(), study.getStudyNumber(), patientCount);
        return studyMapper.toPatientResponse(saved);
    }

    // ----------------------------------------------------------------
    // Statistiques
    // ----------------------------------------------------------------

    /**
     * Calcule les statistiques agrégées des études pour le tenant courant.
     *
     * @return statistiques par statut, domaine thérapeutique et phase
     * @throws StudyException si le tenant courant n'est pas résolu
     */
    public StudyStatisticsResponse getStatistics() {
        UUID tenantId = resolveTenantId();

        long total = studyRepository.countByTenantIdAndDeletedFalse(tenantId);
        long draft = studyRepository.countByTenantIdAndCurrentStatusAndDeletedFalse(tenantId, StudyStatus.DRAFT);
        long ongoing = studyRepository.countByTenantIdAndCurrentStatusAndDeletedFalse(tenantId, StudyStatus.ONGOING);
        long approved = studyRepository.countByTenantIdAndCurrentStatusAndDeletedFalse(tenantId, StudyStatus.APPROVED);
        long closed = studyRepository.countByTenantIdAndCurrentStatusAndDeletedFalse(tenantId, StudyStatus.CLOSED);
        long cusl = studyRepository.countByTenantIdAndIsSponsorCuslAndDeletedFalse(tenantId, true);

        Map<String, Long> byArea = studyRepository.countByTherapeuticAreaAndTenantId(tenantId)
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1],
                (a, b) -> a,
                LinkedHashMap::new
            ));

        Map<String, Long> byPhase = studyRepository.countByPhaseAndTenantId(tenantId)
            .stream()
            .collect(Collectors.toMap(
                row -> row[0] != null ? row[0].toString() : "N/A",
                row -> (Long) row[1],
                (a, b) -> a,
                LinkedHashMap::new
            ));

        return new StudyStatisticsResponse(total, draft, ongoing, approved, closed, cusl, byArea, byPhase);
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Résout l'UUID du tenant courant depuis le {@link TenantContext}.
     *
     * @return UUID du tenant
     * @throws StudyException si le tenant n'est pas résolu
     */
    private UUID resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new StudyException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        try {
            return UUID.fromString(tenantStr);
        } catch (IllegalArgumentException e) {
            throw new StudyException("Identifiant de tenant invalide : " + tenantStr);
        }
    }

    /**
     * Recherche une étude par ID et tenant, ou lève une exception si introuvable.
     *
     * @param studyId  identifiant de l'étude
     * @param tenantId identifiant du tenant
     * @return entité ClinicalStudy trouvée
     * @throws StudyNotFoundException si l'étude n'existe pas ou est supprimée
     */
    private ClinicalStudy findStudyOrThrow(UUID studyId, UUID tenantId) {
        return studyRepository.findByIdAndTenantIdAndDeletedFalse(studyId, tenantId)
            .orElseThrow(() -> new StudyNotFoundException(studyId));
    }

    /**
     * Génère un numéro d'étude unique au format {@code ST-{YYYY}-{seq5}}.
     *
     * @param tenantId identifiant du tenant
     * @return numéro d'étude généré et unique
     */
    private String generateStudyNumber(UUID tenantId) {
        int year = Year.now().getValue();
        long seq = studyRepository.countByTenantIdAndYear(tenantId, year) + 1;
        String candidate = String.format("ST-%d-%05d", year, seq);

        // Boucle de sécurité pour garantir l'unicité en cas de concurrence
        int attempts = 0;
        while (studyRepository.existsByStudyNumberAndTenantId(candidate, tenantId) && attempts < 100) {
            seq++;
            candidate = String.format("ST-%d-%05d", year, seq);
            attempts++;
        }
        return candidate;
    }

    /**
     * Crée et persiste une entrée dans l'historique des statuts.
     *
     * @param study      étude concernée
     * @param status     nouveau statut
     * @param statusDate date du changement
     * @param comment    commentaire optionnel
     * @param tenantId   identifiant du tenant
     */
    private void createStatusHistoryEntry(
        ClinicalStudy study,
        StudyStatus status,
        LocalDate statusDate,
        String comment,
        UUID tenantId
    ) {
        StudyStatusHistory history = new StudyStatusHistory();
        history.setStudy(study);
        history.setTenantId(tenantId);
        history.setStatus(status);
        history.setStatusDate(statusDate);
        history.setComment(comment);
        history.setChangedBy(getCurrentUsername());
        statusHistoryRepository.save(history);
    }

    /**
     * Récupère le nom d'utilisateur courant depuis le SecurityContext.
     *
     * @return email de l'utilisateur ou "system" si non authentifié
     */
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "system";
    }
}
