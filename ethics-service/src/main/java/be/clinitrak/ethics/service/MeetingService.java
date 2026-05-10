package be.clinitrak.ethics.service;

import be.clinitrak.ethics.domain.entity.Meeting;
import be.clinitrak.ethics.domain.entity.MeetingAgendaItem;
import be.clinitrak.ethics.domain.enums.MeetingStatus;
import be.clinitrak.ethics.domain.repository.MeetingAgendaItemRepository;
import be.clinitrak.ethics.domain.repository.MeetingRepository;
import be.clinitrak.ethics.dto.AgendaItemRequest;
import be.clinitrak.ethics.dto.AgendaItemResponse;
import be.clinitrak.ethics.dto.MeetingCreateRequest;
import be.clinitrak.ethics.dto.MeetingResponse;
import be.clinitrak.ethics.exception.EthicsException;
import be.clinitrak.ethics.exception.EthicsNotFoundException;
import be.clinitrak.ethics.mapper.EthicsMapper;
import be.clinitrak.ethics.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des réunions du Comité d'Éthique.
 *
 * <p>Toutes les méthodes opèrent dans le contexte du tenant courant récupéré
 * via {@link TenantContext#getTenantId()}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingAgendaItemRepository agendaItemRepository;
    private final EthicsMapper ethicsMapper;

    /**
     * Retourne les réunions planifiées dans les 30 prochains jours pour le tenant courant.
     *
     * @return liste des réunions à venir, triée par date croissante
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public List<MeetingResponse> getUpcomingMeetings() {
        UUID tenantId = resolveTenantId();
        LocalDate today = LocalDate.now();
        return meetingRepository
            .findByTenantIdAndMeetingDateBetweenOrderByMeetingDate(tenantId, today, today.plusDays(30))
            .stream()
            .map(this::toResponseWithAgendaCount)
            .collect(Collectors.toList());
    }

    /**
     * Retourne les réunions dans une plage de dates pour le tenant courant.
     *
     * @param from date de début (inclusive)
     * @param to   date de fin (inclusive)
     * @return liste des réunions dans la plage, triée par date croissante
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    public List<MeetingResponse> getMeetingsByDateRange(LocalDate from, LocalDate to) {
        UUID tenantId = resolveTenantId();
        return meetingRepository
            .findByTenantIdAndMeetingDateBetweenOrderByMeetingDate(tenantId, from, to)
            .stream()
            .map(this::toResponseWithAgendaCount)
            .collect(Collectors.toList());
    }

    /**
     * Retourne une réunion par son identifiant.
     *
     * @param id identifiant UUID de la réunion
     * @return réponse complète de la réunion
     * @throws EthicsNotFoundException si la réunion n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    public MeetingResponse getMeeting(UUID id) {
        UUID tenantId = resolveTenantId();
        Meeting meeting = findMeetingOrThrow(id, tenantId);
        return toResponseWithAgendaCount(meeting);
    }

    /**
     * Crée une nouvelle réunion CE.
     *
     * @param request données de création
     * @return réponse complète de la réunion créée
     * @throws EthicsException si le tenant courant n'est pas résolu
     */
    @Transactional
    public MeetingResponse createMeeting(MeetingCreateRequest request) {
        UUID tenantId = resolveTenantId();

        Meeting meeting = new Meeting();
        meeting.setTenantId(tenantId);
        meeting.setMeetingDate(request.meetingDate());
        meeting.setMeetingTime(request.meetingTime());
        meeting.setMeetingType(request.meetingType());
        meeting.setLocation(request.location());
        meeting.setNotes(request.notes());
        meeting.setStatus(MeetingStatus.PLANNED);

        Meeting saved = meetingRepository.save(meeting);
        log.info("Réunion CE créée : {} le {} (tenant: {})",
            saved.getId(), saved.getMeetingDate(), tenantId);

        return toResponseWithAgendaCount(saved);
    }

    /**
     * Met à jour une réunion CE existante.
     *
     * @param id      identifiant de la réunion à mettre à jour
     * @param request nouvelles données de la réunion
     * @return réponse complète de la réunion mise à jour
     * @throws EthicsNotFoundException si la réunion n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    @Transactional
    public MeetingResponse updateMeeting(UUID id, MeetingCreateRequest request) {
        UUID tenantId = resolveTenantId();
        Meeting meeting = findMeetingOrThrow(id, tenantId);

        meeting.setMeetingDate(request.meetingDate());
        meeting.setMeetingTime(request.meetingTime());
        meeting.setMeetingType(request.meetingType());
        meeting.setLocation(request.location());
        meeting.setNotes(request.notes());

        Meeting saved = meetingRepository.save(meeting);
        log.info("Réunion CE mise à jour : {} (tenant: {})", saved.getId(), tenantId);

        return toResponseWithAgendaCount(saved);
    }

    /**
     * Retourne les items de l'ordre du jour d'une réunion.
     *
     * @param meetingId identifiant de la réunion
     * @return liste des items triée par ordre croissant
     * @throws EthicsNotFoundException si la réunion n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    public List<AgendaItemResponse> getAgendaItems(UUID meetingId) {
        UUID tenantId = resolveTenantId();
        findMeetingOrThrow(meetingId, tenantId);
        return agendaItemRepository
            .findByMeetingIdAndTenantIdOrderByItemOrder(meetingId, tenantId)
            .stream()
            .map(ethicsMapper::toAgendaItemResponse)
            .collect(Collectors.toList());
    }

    /**
     * Ajoute un item à l'ordre du jour d'une réunion.
     *
     * @param meetingId identifiant de la réunion
     * @param request   données de l'item à ajouter
     * @return réponse de l'item créé
     * @throws EthicsNotFoundException si la réunion n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    @Transactional
    public AgendaItemResponse addAgendaItem(UUID meetingId, AgendaItemRequest request) {
        UUID tenantId = resolveTenantId();
        Meeting meeting = findMeetingOrThrow(meetingId, tenantId);

        MeetingAgendaItem item = new MeetingAgendaItem();
        item.setTenantId(tenantId);
        item.setMeeting(meeting);
        item.setStudyId(request.studyId());
        item.setItemOrder(request.itemOrder());
        item.setItemType(request.itemType());
        item.setDurationMinutes(request.durationMinutes());
        item.setComments(request.comments());

        MeetingAgendaItem saved = agendaItemRepository.save(item);
        log.debug("Item ajouté à l'ordre du jour de la réunion {} : ordre={}", meetingId, item.getItemOrder());

        return ethicsMapper.toAgendaItemResponse(saved);
    }

    /**
     * Met à jour le statut d'une réunion.
     *
     * @param id     identifiant de la réunion
     * @param status nouveau statut
     * @throws EthicsNotFoundException si la réunion n'existe pas dans le tenant courant
     * @throws EthicsException         si le tenant courant n'est pas résolu
     */
    @Transactional
    public void updateMeetingStatus(UUID id, MeetingStatus status) {
        UUID tenantId = resolveTenantId();
        Meeting meeting = findMeetingOrThrow(id, tenantId);
        MeetingStatus previous = meeting.getStatus();
        meeting.setStatus(status);
        meetingRepository.save(meeting);
        log.info("Statut de la réunion {} : {} → {} (tenant: {})", id, previous, status, tenantId);
    }

    // ----------------------------------------------------------------
    // Méthodes utilitaires privées
    // ----------------------------------------------------------------

    /**
     * Convertit une réunion en réponse en incluant le nombre d'items à l'ordre du jour.
     *
     * @param meeting entité Meeting
     * @return DTO MeetingResponse avec agendaItemCount calculé
     */
    private MeetingResponse toResponseWithAgendaCount(Meeting meeting) {
        int count = agendaItemRepository
            .findByMeetingIdAndTenantIdOrderByItemOrder(meeting.getId(), meeting.getTenantId())
            .size();
        MeetingResponse base = ethicsMapper.toMeetingResponse(meeting);
        // Reconstruire avec le bon count (MapStruct met 0 par défaut)
        return new MeetingResponse(
            base.id(), base.tenantId(), base.meetingDate(), base.meetingTime(),
            base.meetingType(), base.meetingTypeLabel(), base.location(),
            base.status(), base.statusLabel(), base.notes(), count, base.createdAt()
        );
    }

    /**
     * Résout l'UUID du tenant courant depuis le {@link TenantContext}.
     *
     * @return UUID du tenant
     * @throws EthicsException si le tenant n'est pas résolu
     */
    private UUID resolveTenantId() {
        String tenantStr = TenantContext.getTenantId();
        if (tenantStr == null || tenantStr.isBlank()) {
            throw new EthicsException("Tenant non résolu — vérifiez le header X-Tenant-ID ou le token JWT");
        }
        try {
            return UUID.fromString(tenantStr);
        } catch (IllegalArgumentException e) {
            throw new EthicsException("Identifiant de tenant invalide : " + tenantStr);
        }
    }

    /**
     * Recherche une réunion par ID et tenant, ou lève une exception si introuvable.
     *
     * @param id       identifiant de la réunion
     * @param tenantId identifiant du tenant
     * @return entité Meeting trouvée
     * @throws EthicsNotFoundException si la réunion n'existe pas
     */
    private Meeting findMeetingOrThrow(UUID id, UUID tenantId) {
        return meetingRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new EthicsNotFoundException("Réunion", id));
    }
}
