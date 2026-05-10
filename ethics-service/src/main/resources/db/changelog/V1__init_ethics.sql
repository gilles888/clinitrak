-- ============================================================
-- V1 : Schéma initial du ethics-service
-- Comité d'Éthique CliniTrak
-- ============================================================

-- Trigger function pour mise à jour automatique de updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- ============================================================
-- Table : ethics_sequences
-- Séquences de numérotation CE par année et tenant
-- ============================================================
CREATE TABLE ethics_sequences (
    id          BIGSERIAL PRIMARY KEY,
    year        INTEGER     NOT NULL,
    tenant_id   UUID        NOT NULL,
    last_value  BIGINT      NOT NULL DEFAULT 0,
    version     BIGINT,
    CONSTRAINT uk_seq_year_tenant UNIQUE (year, tenant_id)
);

COMMENT ON TABLE  ethics_sequences IS 'Séquences de numérotation des avis CE par année et tenant';
COMMENT ON COLUMN ethics_sequences.last_value IS 'Dernier numéro de séquence utilisé (commence à 0)';

-- ============================================================
-- Table : ethics_reviews
-- Avis éthiques soumis au Comité
-- ============================================================
CREATE TABLE ethics_reviews (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID        NOT NULL,
    study_id         UUID        NOT NULL,
    ethics_number    VARCHAR(20) UNIQUE,
    review_type      VARCHAR(50) NOT NULL,
    submission_date  DATE        NOT NULL,
    review_date      DATE,
    decision         VARCHAR(50) DEFAULT 'PENDING',
    decision_date    DATE,
    comments         TEXT,
    next_review_date DATE,
    rapporteur_name  VARCHAR(255),
    reminder_sent    BOOLEAN     NOT NULL DEFAULT FALSE,
    -- BaseEntity columns
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    version          BIGINT
);

CREATE INDEX idx_er_tenant_id      ON ethics_reviews (tenant_id);
CREATE INDEX idx_er_study_id       ON ethics_reviews (study_id);
CREATE INDEX idx_er_decision       ON ethics_reviews (decision);
CREATE INDEX idx_er_submission_date ON ethics_reviews (submission_date);

CREATE TRIGGER update_ethics_reviews_updated_at
    BEFORE UPDATE ON ethics_reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  ethics_reviews IS 'Avis éthiques soumis au Comité d''Éthique';
COMMENT ON COLUMN ethics_reviews.ethics_number IS 'Numéro CE unique au format AAAA/NNNN';

-- ============================================================
-- Table : meetings
-- Réunions du Comité d'Éthique
-- ============================================================
CREATE TABLE meetings (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID        NOT NULL,
    meeting_date     DATE        NOT NULL,
    meeting_time     VARCHAR(10),
    meeting_type     VARCHAR(30) DEFAULT 'ORDINARY',
    location         VARCHAR(255),
    agenda           TEXT,
    minutes_document TEXT,
    attendees        TEXT,
    status           VARCHAR(30) DEFAULT 'PLANNED',
    notes            TEXT,
    -- BaseEntity columns
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    version          BIGINT
);

CREATE INDEX idx_mtg_tenant_id   ON meetings (tenant_id);
CREATE INDEX idx_mtg_meeting_date ON meetings (meeting_date);
CREATE INDEX idx_mtg_status       ON meetings (status);

CREATE TRIGGER update_meetings_updated_at
    BEFORE UPDATE ON meetings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE meetings IS 'Réunions du Comité d''Éthique (ordinaires et extraordinaires)';

-- ============================================================
-- Table : meeting_agenda_items
-- Items de l'ordre du jour des réunions
-- ============================================================
CREATE TABLE meeting_agenda_items (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID        NOT NULL,
    meeting_id       UUID        NOT NULL,
    study_id         UUID,
    item_order       INTEGER     NOT NULL,
    item_type        VARCHAR(50),
    duration_minutes INTEGER     DEFAULT 0,
    decision         VARCHAR(50),
    comments         TEXT,
    -- BaseEntity columns
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    version          BIGINT,
    CONSTRAINT fk_mai_meeting FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);

CREATE INDEX idx_mai_meeting_id ON meeting_agenda_items (meeting_id);
CREATE INDEX idx_mai_tenant_id  ON meeting_agenda_items (tenant_id);

CREATE TRIGGER update_meeting_agenda_items_updated_at
    BEFORE UPDATE ON meeting_agenda_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE meeting_agenda_items IS 'Points de l''ordre du jour des réunions CE';
COMMENT ON COLUMN meeting_agenda_items.study_id IS 'Référence optionnelle vers une étude (null pour les points DIVERS)';

-- ============================================================
-- Table : annual_reports
-- Rapports annuels attendus par le Comité d'Éthique
-- ============================================================
CREATE TABLE annual_reports (
    id                UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id         UUID    NOT NULL,
    study_id          UUID    NOT NULL,
    report_year       INTEGER NOT NULL,
    due_date          DATE    NOT NULL,
    received_date     DATE,
    status            VARCHAR(30) DEFAULT 'PENDING',
    reminder_sent_60  BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_30  BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_0   BOOLEAN NOT NULL DEFAULT FALSE,
    last_reminder_date DATE,
    notes             TEXT,
    -- BaseEntity columns
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    deleted           BOOLEAN NOT NULL DEFAULT FALSE,
    version           BIGINT,
    CONSTRAINT uk_ar_study_year_tenant UNIQUE (study_id, report_year, tenant_id)
);

CREATE INDEX idx_ar_tenant_id ON annual_reports (tenant_id);
CREATE INDEX idx_ar_study_id  ON annual_reports (study_id);
CREATE INDEX idx_ar_due_date  ON annual_reports (due_date);
CREATE INDEX idx_ar_status    ON annual_reports (status);

CREATE TRIGGER update_annual_reports_updated_at
    BEFORE UPDATE ON annual_reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  annual_reports IS 'Rapports annuels attendus par le CE pour les études en cours';
COMMENT ON COLUMN annual_reports.reminder_sent_60 IS 'Rappel envoyé 60 jours avant l''échéance';
COMMENT ON COLUMN annual_reports.reminder_sent_30 IS 'Rappel envoyé 30 jours avant l''échéance';
COMMENT ON COLUMN annual_reports.reminder_sent_0  IS 'Rappel envoyé à l''échéance ou en retard';

-- ============================================================
-- Table : correspondence_templates
-- Modèles de correspondance Thymeleaf
-- ============================================================
CREATE TABLE correspondence_templates (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id     UUID,
    template_code VARCHAR(50) NOT NULL UNIQUE,
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    content       TEXT,
    language      VARCHAR(5)  DEFAULT 'fr',
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    subject       VARCHAR(255),
    -- BaseEntity columns
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    version       BIGINT
);

CREATE INDEX idx_ct_template_type ON correspondence_templates (template_type);
CREATE INDEX idx_ct_is_active     ON correspondence_templates (is_active);

CREATE TRIGGER update_correspondence_templates_updated_at
    BEFORE UPDATE ON correspondence_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  correspondence_templates IS 'Modèles Thymeleaf pour les correspondances CE';
COMMENT ON COLUMN correspondence_templates.tenant_id IS 'NULL = template global accessible à tous les tenants';
COMMENT ON COLUMN correspondence_templates.content IS 'Contenu HTML Thymeleaf avec variables [[${var}]]';

-- ============================================================
-- Table : correspondence
-- Correspondances générées par le CE
-- ============================================================
CREATE TABLE correspondence (
    id               UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id        UUID NOT NULL,
    study_id         UUID NOT NULL,
    review_id        UUID,
    template_id      UUID,
    generated_date   DATE NOT NULL,
    sent_date        DATE,
    recipient_email  VARCHAR(255),
    recipient_name   VARCHAR(255),
    subject          VARCHAR(255),
    content          TEXT,
    pdf_path         TEXT,
    sent             BOOLEAN NOT NULL DEFAULT FALSE,
    send_error       TEXT,
    -- BaseEntity columns
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version          BIGINT
);

CREATE INDEX idx_corr_study_id  ON correspondence (study_id);
CREATE INDEX idx_corr_tenant_id ON correspondence (tenant_id);
CREATE INDEX idx_corr_sent      ON correspondence (sent);

CREATE TRIGGER update_correspondence_updated_at
    BEFORE UPDATE ON correspondence
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE correspondence IS 'Correspondances CE générées depuis les templates Thymeleaf';
COMMENT ON COLUMN correspondence.pdf_path IS 'Chemin ou URL vers le PDF généré (null si pas encore généré)';
