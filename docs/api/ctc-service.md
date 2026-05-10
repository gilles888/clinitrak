# CliniTrak — API CTC Service

*Port: 8084 | Base URL: /api/v1/ctc*

## Authentification
Bearer JWT + X-Tenant-ID header (identique aux autres services)

## Endpoints

### Guichet (Desk Requests)

#### GET /api/v1/ctc/desk-requests
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR, ROLE_SUPER_ADMIN
Réponse 200: array de TrialDeskRequestResponse

#### POST /api/v1/ctc/desk-requests
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR
Body: { studyId, deskType, requestorName, requestorEmail, requestorOrganization, requestType, priority, deadline, notes }
Réponse 201: TrialDeskRequestResponse

#### PATCH /api/v1/ctc/desk-requests/{id}/assign
Rôle: ROLE_CTC_MANAGER
Body: { assignedTo, priority, deadline }
Réponse 200: TrialDeskRequestResponse

#### PATCH /api/v1/ctc/desk-requests/{id}/status
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR
Body: { status, notes }
Réponse 200: TrialDeskRequestResponse

### Monitoring

#### GET /api/v1/ctc/monitoring-visits
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR, ROLE_SUPER_ADMIN
Réponse 200: array de MonitoringVisitResponse

#### POST /api/v1/ctc/monitoring-visits
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR
Body: { studyId, visitDate, visitType, monitorName, correctionDeadline }
Réponse 201: MonitoringVisitResponse

### Contrats financiers

#### GET /api/v1/ctc/financial-contracts
Rôles: ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN
Réponse 200: array de FinancialContractResponse

#### POST /api/v1/ctc/financial-contracts
Rôle: ROLE_CTC_MANAGER
Body: { studyId, contractType, contractDate, amount, currency, billingSchedule, paymentTerms }
Réponse 201: FinancialContractResponse

### Statistiques

#### GET /api/v1/ctc/statistics-requests
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR, ROLE_SUPER_ADMIN
Réponse 200: array de StatisticsRequestResponse

#### POST /api/v1/ctc/statistics-requests
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR
Body: { studyId, requestorName, deadline, analysisType, dataFormat }
Réponse 201: StatisticsRequestResponse

### Qualité

#### GET /api/v1/ctc/quality-events
Rôles: ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN, ROLE_QUALITY_MANAGER
Réponse 200: array de QualityEventResponse

#### POST /api/v1/ctc/quality-events
Rôles: ROLE_CTC_MANAGER, ROLE_QUALITY_MANAGER
Body: { studyId, eventType, eventDate, severity, description }
Réponse 201: QualityEventResponse

### Études promoteur CUSL

#### GET /api/v1/ctc/sponsor-studies
Rôles: ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN
Réponse 200: array de SponsorStudyResponse

#### POST /api/v1/ctc/sponsor-studies
Rôle: ROLE_CTC_MANAGER
Body: { studyId, projectManagerId, budgetTotal }
Réponse 201: SponsorStudyResponse

### Dashboard & Timeline

#### GET /api/v1/ctc/dashboard
Rôles: ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN
Réponse 200: CTCDashboardResponse { totalDeskRequests, pendingRequests, plannedVisits, openQualityEvents, criticalEvents, activeContracts, pendingStatistics, sponsorStudies, recentRequests[] }

#### GET /api/v1/ctc/timeline/{studyId}
Rôles: ROLE_CTC_MANAGER, ROLE_STUDY_COORDINATOR, ROLE_SUPER_ADMIN
Réponse 200: StudyTimelineResponse { studyId, events: TimelineEvent[] }
TimelineEvent: { studyId, eventType, title, description, eventDate, severity }

## Enums

### DeskType: ACADEMIC(Académique) | COMMERCIAL(Commercial)
### RequestType: NEW_STUDY | AMENDMENT | EXTENSION | CLOSURE
### RequestStatus: PENDING | ASSIGNED | IN_PROGRESS | COMPLETED | REJECTED
### Priority: LOW | MEDIUM | HIGH | URGENT
### VisitType: INITIATION | ROUTINE | CLOSE_OUT
### VisitStatus: PLANNED | COMPLETED | CANCELLED
### ContractType: CONVENTION | AMENDMENT | SPONSOR
### ContractStatus: DRAFT | ACTIVE | COMPLETED | CANCELLED
### EventType: DEVIATION | SAE | CAPA | AUDIT
### Severity: LOW | MEDIUM | HIGH | CRITICAL
### EventStatus: OPEN | IN_PROGRESS | CLOSED | CANCELLED
### AnalysisType: DESCRIPTIVE | INFERENTIAL | SURVIVAL | LONGITUDINAL
### RegulatoryStatus: IN_PREPARATION | SUBMITTED | APPROVED | ONGOING | COMPLETED
