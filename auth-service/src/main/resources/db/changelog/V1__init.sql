-- ============================================================
-- CliniTrak — V1 : Schéma initial auth-service
-- ============================================================
-- Auteur   : CliniTrak Team
-- Date     : 2025-05-09
-- Note     : Toutes les tables utilisent UUID comme PK (gen_random_uuid())
-- ============================================================

-- ----------------------------------------------------------
-- TABLE : tenants
-- ----------------------------------------------------------
CREATE TABLE tenants (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    slug            VARCHAR(63) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(255),
    db_schema       VARCHAR(63),
    contact_email   VARCHAR(255),
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    settings        JSONB,
    deleted         BOOLEAN     NOT NULL DEFAULT FALSE,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),

    CONSTRAINT pk_tenants           PRIMARY KEY (id),
    CONSTRAINT uk_tenant_slug       UNIQUE (slug),
    CONSTRAINT chk_tenant_slug_fmt  CHECK (slug ~ '^[a-z0-9][a-z0-9\-]{1,61}[a-z0-9]$')
);

COMMENT ON TABLE  tenants         IS 'Institutions hospitalières (tenants du système multi-tenant)';
COMMENT ON COLUMN tenants.slug    IS 'Identifiant court unique utilisé dans les headers X-Tenant-ID';
COMMENT ON COLUMN tenants.settings IS 'Paramètres JSON spécifiques au tenant (timezone, locale, modules actifs)';

-- ----------------------------------------------------------
-- TABLE : permissions
-- ----------------------------------------------------------
CREATE TABLE permissions (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    module      VARCHAR(50),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    CONSTRAINT pk_permissions       PRIMARY KEY (id),
    CONSTRAINT uk_permission_name   UNIQUE (name),
    CONSTRAINT chk_perm_name_fmt    CHECK (name ~ '^[A-Z_:]+$')
);

COMMENT ON TABLE  permissions       IS 'Permissions atomiques (RESOURCE:ACTION)';
COMMENT ON COLUMN permissions.name  IS 'Ex: STUDY:READ, ETHICS:SUBMIT, PHARMACY:DISPENSE';

-- ----------------------------------------------------------
-- TABLE : roles
-- ----------------------------------------------------------
CREATE TABLE roles (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    tenant_id   UUID,               -- NULL = rôle global
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    CONSTRAINT pk_roles                 PRIMARY KEY (id),
    CONSTRAINT uk_role_name_tenant      UNIQUE (name, tenant_id),
    CONSTRAINT fk_role_tenant           FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

COMMENT ON TABLE  roles           IS 'Rôles RBAC regroupant des permissions. tenant_id=NULL = rôle global.';
COMMENT ON COLUMN roles.tenant_id IS 'NULL pour les rôles système, UUID pour les rôles spécifiques à un tenant';

-- ----------------------------------------------------------
-- TABLE : role_permissions (N:N)
-- ----------------------------------------------------------
CREATE TABLE role_permissions (
    role_id         UUID NOT NULL,
    permission_id   UUID NOT NULL,

    CONSTRAINT pk_role_permissions  PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role           FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission     FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- ----------------------------------------------------------
-- TABLE : users
-- ----------------------------------------------------------
CREATE TABLE users (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    email                   VARCHAR(255) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    tenant_id               UUID        NOT NULL,
    enabled                 BOOLEAN     NOT NULL DEFAULT TRUE,
    account_locked          BOOLEAN     NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMPTZ,
    failed_login_attempts   INT         NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    deleted                 BOOLEAN     NOT NULL DEFAULT FALSE,
    version                 BIGINT      NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),

    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uk_user_email_tenant UNIQUE (email, tenant_id),
    CONSTRAINT fk_user_tenant       FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT chk_failed_attempts  CHECK (failed_login_attempts >= 0)
);

COMMENT ON TABLE  users                 IS 'Utilisateurs de la plateforme, rattachés à un tenant';
COMMENT ON COLUMN users.email           IS 'Email unique par tenant (insensible à la casse à l''application)';
COMMENT ON COLUMN users.password_hash   IS 'BCrypt hash du mot de passe (force 12)';

CREATE INDEX idx_user_email  ON users(email);
CREATE INDEX idx_user_tenant ON users(tenant_id);

-- ----------------------------------------------------------
-- TABLE : user_roles (N:N)
-- ----------------------------------------------------------
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    CONSTRAINT pk_user_roles    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role       FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- ----------------------------------------------------------
-- TABLE : refresh_tokens
-- ----------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    token       VARCHAR(512) NOT NULL,
    user_id     UUID        NOT NULL,
    tenant_id   UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    user_agent  VARCHAR(512),
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_refresh_tokens    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token     UNIQUE (token),
    CONSTRAINT fk_rt_user           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_value ON refresh_tokens(token);
CREATE INDEX idx_refresh_token_user  ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_exp   ON refresh_tokens(expires_at) WHERE revoked = FALSE;

-- ----------------------------------------------------------
-- TABLE : audit_logs (insert-only)
-- ----------------------------------------------------------
CREATE TABLE audit_logs (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID,
    user_email      VARCHAR(255),
    tenant_id       UUID,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100),
    resource_id     VARCHAR(255),
    details         JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    http_status     INT,
    outcome         VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs        PRIMARY KEY (id),
    CONSTRAINT chk_audit_outcome    CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_user       ON audit_logs(user_id);
CREATE INDEX idx_audit_tenant     ON audit_logs(tenant_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_action     ON audit_logs(action);

COMMENT ON TABLE  audit_logs IS 'Logs d''audit immuables — aucune UPDATE autorisée sur cette table';

-- ----------------------------------------------------------
-- TRIGGER : updated_at automatique
-- ----------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
