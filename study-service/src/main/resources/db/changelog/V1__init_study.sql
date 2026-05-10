-- ============================================================
-- CliniTrak — V1 : Schéma initial study-service
-- ============================================================
-- Auteur   : CliniTrak Team
-- Date     : 2026-05-09
-- Note     : Toutes les tables utilisent UUID comme PK (gen_random_uuid())
--            Multi-tenant via colonne tenant_id sur chaque table
--            Soft delete via colonne deleted
--            Optimistic locking via colonne version
-- ============================================================

-- ----------------------------------------------------------
-- SÉQUENCE : Pour la numérotation des études (ST-YYYY-NNNNN)
-- ----------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS clinitrak_study_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;

COMMENT ON SEQUENCE clinitrak_study_seq IS 'Séquence pour la génération des numéros d''études ST-YYYY-NNNNN';

-- ----------------------------------------------------------
-- FONCTION : Mise à jour automatique du champ updated_at
-- ----------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function pour mettre à jour automatiquement updated_at';

-- ----------------------------------------------------------
-- TABLE : clinical_studies
-- ----------------------------------------------------------
CREATE TABLE clinical_studies (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id               UUID        NOT NULL,
    study_number            VARCHAR(30),
    ethics_number           VARCHAR(100),
    eudract_number          VARCHAR(20),
    ctis_number             VARCHAR(30),
    title                   VARCHAR(500) NOT NULL,
    acronym                 VARCHAR(50),
    study_type              VARCHAR(30) NOT NULL,
    sponsor_type            VARCHAR(30) NOT NULL,
    sponsor                 VARCHAR(255),
    principal_investigator  VARCHAR(255),
    therapeutic_area        VARCHAR(100),
    phase                   VARCHAR(20),
    current_status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    start_date              DATE,
    end_date                DATE,
    approval_date           DATE,
    target_enrollment       INT,
    current_enrollment      INT         NOT NULL DEFAULT 0,
    is_sponsor_cusl         BOOLEAN     NOT NULL DEFAULT FALSE,
    description             TEXT,
    -- Champ JSONB pour futures extensions (métadonnées spécifiques au tenant)
    metadata                JSONB,
    -- Champs d'audit (BaseEntity)
    deleted                 BOOLEAN     NOT NULL DEFAULT FALSE,
    version                 BIGINT      NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),

    CONSTRAINT pk_clinical_studies      PRIMARY KEY (id),
    CONSTRAINT uk_study_number_tenant   UNIQUE (study_number, tenant_id),
    CONSTRAINT chk_study_type           CHECK (study_type IN ('INTERVENTIONAL', 'OBSERVATIONAL', 'EXPANDED_ACCESS')),
    CONSTRAINT chk_sponsor_type         CHECK (sponsor_type IN ('ACADEMIC', 'COMMERCIAL', 'INSTITUTIONAL')),
    CONSTRAINT chk_current_status       CHECK (current_status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ONGOING', 'SUSPENDED', 'CLOSED', 'WITHDRAWN')),
    CONSTRAINT chk_phase                CHECK (phase IS NULL OR phase IN ('PHASE_1', 'PHASE_2', 'PHASE_3', 'PHASE_4', 'NA')),
    CONSTRAINT chk_enrollment_positive  CHECK (current_enrollment >= 0)
);

COMMENT ON TABLE  clinical_studies                  IS 'Études cliniques (protocoles de recherche) par tenant';
COMMENT ON COLUMN clinical_studies.study_number     IS 'Numéro interne unique au format ST-YYYY-NNNNN';
COMMENT ON COLUMN clinical_studies.ethics_number    IS 'Numéro d''approbation du Comité d''Éthique';
COMMENT ON COLUMN clinical_studies.eudract_number   IS 'Numéro EudraCT au format YYYY-NNNNNN-CC';
COMMENT ON COLUMN clinical_studies.ctis_number      IS 'Numéro CTIS (EU Clinical Trials Information System)';
COMMENT ON COLUMN clinical_studies.is_sponsor_cusl  IS 'TRUE si les CUSL (Saint-Luc) sont le promoteur';
COMMENT ON COLUMN clinical_studies.metadata         IS 'Données JSON extensibles spécifiques au tenant';

CREATE INDEX idx_study_tenant         ON clinical_studies(tenant_id);
CREATE INDEX idx_study_number         ON clinical_studies(study_number);
CREATE INDEX idx_study_ethics         ON clinical_studies(ethics_number) WHERE ethics_number IS NOT NULL;
CREATE INDEX idx_study_eudract        ON clinical_studies(eudract_number) WHERE eudract_number IS NOT NULL;
CREATE INDEX idx_study_ctis           ON clinical_studies(ctis_number) WHERE ctis_number IS NOT NULL;
CREATE INDEX idx_study_status         ON clinical_studies(current_status);
CREATE INDEX idx_study_tenant_status  ON clinical_studies(tenant_id, current_status) WHERE deleted = FALSE;
CREATE INDEX idx_study_therapeutic    ON clinical_studies(tenant_id, therapeutic_area) WHERE deleted = FALSE;
CREATE INDEX idx_study_sponsor_cusl   ON clinical_studies(tenant_id, is_sponsor_cusl) WHERE deleted = FALSE;

CREATE TRIGGER trg_clinical_studies_updated_at
    BEFORE UPDATE ON clinical_studies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ----------------------------------------------------------
-- TABLE : study_status_history
-- ----------------------------------------------------------
CREATE TABLE study_status_history (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    study_id    UUID        NOT NULL,
    tenant_id   UUID        NOT NULL,
    status      VARCHAR(30) NOT NULL,
    status_date DATE        NOT NULL,
    comment     VARCHAR(2000),
    changed_by  VARCHAR(255),
    -- Champs d'audit (BaseEntity)
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    CONSTRAINT pk_study_status_history  PRIMARY KEY (id),
    CONSTRAINT fk_ssh_study             FOREIGN KEY (study_id) REFERENCES clinical_studies(id) ON DELETE CASCADE,
    CONSTRAINT chk_ssh_status           CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ONGOING', 'SUSPENDED', 'CLOSED', 'WITHDRAWN'))
);

COMMENT ON TABLE  study_status_history          IS 'Historique immuable des changements de statut des études';
COMMENT ON COLUMN study_status_history.status   IS 'Nouveau statut après le changement';
COMMENT ON COLUMN study_status_history.changed_by IS 'Email de l''utilisateur ayant effectué le changement';

CREATE INDEX idx_status_history_study   ON study_status_history(study_id);
CREATE INDEX idx_status_history_tenant  ON study_status_history(tenant_id);
CREATE INDEX idx_status_history_date    ON study_status_history(status_date DESC);

CREATE TRIGGER trg_study_status_history_updated_at
    BEFORE UPDATE ON study_status_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ----------------------------------------------------------
-- TABLE : study_contacts
-- ----------------------------------------------------------
CREATE TABLE study_contacts (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    study_id     UUID        NOT NULL,
    tenant_id    UUID        NOT NULL,
    contact_type VARCHAR(30) NOT NULL,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(255),
    phone        VARCHAR(50),
    organization VARCHAR(255),
    is_primary   BOOLEAN     NOT NULL DEFAULT FALSE,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    -- Champs d'audit (BaseEntity)
    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),

    CONSTRAINT pk_study_contacts    PRIMARY KEY (id),
    CONSTRAINT fk_sc_study          FOREIGN KEY (study_id) REFERENCES clinical_studies(id) ON DELETE CASCADE,
    CONSTRAINT chk_contact_type     CHECK (contact_type IN ('INVESTIGATOR', 'CRA', 'COORDINATOR', 'SPONSOR'))
);

COMMENT ON TABLE  study_contacts                IS 'Contacts associés aux études (investigateurs, CRA, coordinateurs, promoteurs)';
COMMENT ON COLUMN study_contacts.is_primary     IS 'TRUE si contact principal pour son type';
COMMENT ON COLUMN study_contacts.active         IS 'FALSE si contact désactivé (soft delete fonctionnel)';

CREATE INDEX idx_contact_study   ON study_contacts(study_id);
CREATE INDEX idx_contact_tenant  ON study_contacts(tenant_id);
CREATE INDEX idx_contact_active  ON study_contacts(study_id, active) WHERE active = TRUE;

CREATE TRIGGER trg_study_contacts_updated_at
    BEFORE UPDATE ON study_contacts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ----------------------------------------------------------
-- TABLE : submissions
-- ----------------------------------------------------------
CREATE TABLE submissions (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    study_id         UUID        NOT NULL,
    tenant_id        UUID        NOT NULL,
    submission_type  VARCHAR(30) NOT NULL,
    submission_date  DATE        NOT NULL,
    due_date         DATE,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    submitted_by     VARCHAR(255),
    received_date    DATE,
    reference_number VARCHAR(100),
    comments         TEXT,
    -- Champs d'audit (BaseEntity)
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    version          BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),

    CONSTRAINT pk_submissions       PRIMARY KEY (id),
    CONSTRAINT fk_sub_study         FOREIGN KEY (study_id) REFERENCES clinical_studies(id) ON DELETE CASCADE,
    CONSTRAINT chk_submission_type  CHECK (submission_type IN ('INITIAL', 'AMENDMENT', 'ANNUAL_REPORT', 'SAFETY_REPORT', 'FINAL_REPORT')),
    CONSTRAINT chk_submission_status CHECK (status IN ('PENDING', 'SUBMITTED', 'ACKNOWLEDGED', 'APPROVED', 'REJECTED', 'WITHDRAWN'))
);

COMMENT ON TABLE  submissions                   IS 'Soumissions réglementaires aux autorités compétentes (CE, FAMHP, EMA)';
COMMENT ON COLUMN submissions.submission_type   IS 'Type : initiale, amendement, rapport annuel/sécurité/final';
COMMENT ON COLUMN submissions.reference_number  IS 'Numéro de référence attribué par les autorités à réception';

CREATE INDEX idx_submission_study   ON submissions(study_id);
CREATE INDEX idx_submission_tenant  ON submissions(tenant_id);
CREATE INDEX idx_submission_status  ON submissions(status);
CREATE INDEX idx_submission_date    ON submissions(submission_date DESC);

CREATE TRIGGER trg_submissions_updated_at
    BEFORE UPDATE ON submissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ----------------------------------------------------------
-- TABLE : study_patients
-- ----------------------------------------------------------
CREATE TABLE study_patients (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    study_id        UUID        NOT NULL,
    tenant_id       UUID        NOT NULL,
    patient_code    VARCHAR(50) NOT NULL,
    inclusion_date  DATE,
    exclusion_date  DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'SCREENED',
    site_code       VARCHAR(50),
    notes           TEXT,
    -- Champs d'audit (BaseEntity)
    deleted         BOOLEAN     NOT NULL DEFAULT FALSE,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),

    CONSTRAINT pk_study_patients        PRIMARY KEY (id),
    CONSTRAINT fk_sp_study              FOREIGN KEY (study_id) REFERENCES clinical_studies(id) ON DELETE CASCADE,
    CONSTRAINT uk_patient_code_study    UNIQUE (patient_code, study_id),
    CONSTRAINT chk_patient_status       CHECK (status IN ('SCREENED', 'ENROLLED', 'ONGOING', 'COMPLETED', 'WITHDRAWN', 'SCREEN_FAILED'))
);

COMMENT ON TABLE  study_patients                IS 'Patients pseudonymisés (RGPD) participant aux études cliniques';
COMMENT ON COLUMN study_patients.patient_code   IS 'Code pseudonyme unique dans l''étude — pas de données identifiantes';
COMMENT ON COLUMN study_patients.notes          IS 'Notes de suivi non identifiantes — pas de nom, prénom, DDN';
COMMENT ON COLUMN study_patients.site_code      IS 'Code du centre participant pour les études multicentriques';

CREATE INDEX idx_patient_study   ON study_patients(study_id);
CREATE INDEX idx_patient_tenant  ON study_patients(tenant_id);
CREATE INDEX idx_patient_status  ON study_patients(study_id, status);
CREATE INDEX idx_patient_site    ON study_patients(study_id, site_code);

CREATE TRIGGER trg_study_patients_updated_at
    BEFORE UPDATE ON study_patients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
