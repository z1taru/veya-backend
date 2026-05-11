-- ============================================================
--  Veya Backend — V1 initial schema
-- ============================================================

-- ── Extensions ───────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── ENUMS ────────────────────────────────────────────────────
CREATE TYPE user_status    AS ENUM ('ACTIVE', 'INACTIVE', 'BANNED');

CREATE TYPE member_role    AS ENUM ('OWNER', 'PARENT', 'MEMBER');

CREATE TYPE member_status  AS ENUM ('ACTIVE', 'INVITED', 'REMOVED');

CREATE TYPE invite_status  AS ENUM ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED');

CREATE TYPE task_status    AS ENUM ('PENDING', 'ACCEPTED', 'DONE', 'DECLINED', 'LATER');

CREATE TYPE task_priority  AS ENUM ('LOW', 'MEDIUM', 'HIGH');

CREATE TYPE repeat_type    AS ENUM ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY');

CREATE TYPE ai_parsed_type AS ENUM ('TASK', 'SHOPPING', 'REMINDER', 'UNKNOWN');

CREATE TYPE ai_cmd_status  AS ENUM ('PREVIEW', 'CREATED', 'FAILED');

CREATE TYPE notif_type     AS ENUM ('TASK_ASSIGNED', 'TASK_STATUS_CHANGED',
                                    'SHOPPING_ITEM_ADDED', 'REMINDER_TRIGGERED',
                                    'FAMILY_INVITE', 'SYSTEM');

-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email ON users (email);

-- ── REFRESH TOKENS ───────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- ── FAMILIES ─────────────────────────────────────────────────
CREATE TABLE families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    name VARCHAR(120) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── FAMILY MEMBERS ───────────────────────────────────────────
CREATE TABLE family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role member_role NOT NULL DEFAULT 'MEMBER',
    status member_status NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (family_id, user_id)
);

CREATE INDEX idx_family_members_family_id ON family_members (family_id);

CREATE INDEX idx_family_members_user_id ON family_members (user_id);

-- ── FAMILY INVITES ───────────────────────────────────────────
CREATE TABLE family_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    email VARCHAR(254) NOT NULL,
    invited_by UUID NOT NULL REFERENCES users (id),
    role member_role NOT NULL DEFAULT 'MEMBER',
    token VARCHAR(255) NOT NULL,
    status invite_status NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_family_invites_token ON family_invites (token);

CREATE INDEX idx_family_invites_family_id ON family_invites (family_id);

-- ── TASKS ────────────────────────────────────────────────────
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id UUID NOT NULL REFERENCES users (id),
    assignee_id UUID REFERENCES users (id),
    status task_status NOT NULL DEFAULT 'PENDING',
    priority task_priority NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    due_time TIME,
    repeat_type repeat_type NOT NULL DEFAULT 'NONE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_family_id ON tasks (family_id);

CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);

CREATE INDEX idx_tasks_status ON tasks (status);

CREATE INDEX idx_tasks_due_date ON tasks (due_date);

-- ── TASK STATUS HISTORY ──────────────────────────────────────
CREATE TABLE task_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    changed_by UUID NOT NULL REFERENCES users (id),
    old_status task_status,
    new_status task_status NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_history_task_id ON task_status_history (task_id);

-- ── REMINDERS ────────────────────────────────────────────────
CREATE TABLE reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id UUID NOT NULL REFERENCES users (id),
    assignee_id UUID REFERENCES users (id),
    reminder_date DATE,
    reminder_time TIME,
    repeat_type repeat_type NOT NULL DEFAULT 'NONE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_family_id ON reminders (family_id);

-- ── SHOPPING ITEMS ───────────────────────────────────────────
CREATE TABLE shopping_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    quantity VARCHAR(100),
    category VARCHAR(100),
    added_by UUID NOT NULL REFERENCES users (id),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shopping_items_family_id ON shopping_items (family_id);

-- ── AI COMMANDS ──────────────────────────────────────────────
CREATE TABLE ai_commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    family_id UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id),
    raw_text TEXT NOT NULL,
    parsed_type ai_parsed_type NOT NULL DEFAULT 'UNKNOWN',
    parsed_payload JSONB,
    status ai_cmd_status NOT NULL DEFAULT 'PREVIEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_commands_family_id ON ai_commands (family_id);

CREATE INDEX idx_ai_commands_user_id ON ai_commands (user_id);

-- ── NOTIFICATIONS ────────────────────────────────────────────
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id UUID REFERENCES families (id) ON DELETE CASCADE,
    type notif_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);

CREATE INDEX idx_notifications_read ON notifications (user_id, read);