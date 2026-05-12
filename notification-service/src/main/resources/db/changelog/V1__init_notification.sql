-- ============================================================
-- CliniTrak — Notification Service — V1 : Schéma initial
-- ============================================================

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID            NOT NULL,
    tenant_id           UUID,
    type                VARCHAR(50)     NOT NULL,
    subject             VARCHAR(500),
    message             TEXT,
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP,
    email_sent          BOOLEAN         NOT NULL DEFAULT FALSE,
    email_sent_at       TIMESTAMP,
    template_name       VARCHAR(100),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP,
    version_lock        BIGINT          DEFAULT 0
);

CREATE INDEX idx_notifications_recipient  ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_tenant     ON notifications(tenant_id);
CREATE INDEX idx_notifications_read       ON notifications(is_read);
CREATE INDEX idx_notifications_type       ON notifications(type);
CREATE INDEX idx_notifications_deleted_at ON notifications(deleted_at);
