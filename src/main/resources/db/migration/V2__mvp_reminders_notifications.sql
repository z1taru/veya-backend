ALTER TABLE reminders
    ADD COLUMN status task_status NOT NULL DEFAULT 'PENDING';

CREATE INDEX idx_reminders_status ON reminders (status);
CREATE INDEX idx_reminders_assignee_id ON reminders (assignee_id);

ALTER TABLE notifications
    RENAME COLUMN read TO is_read;
