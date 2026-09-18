CREATE TABLE linked_email_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    imap_host VARCHAR(255) NOT NULL,
    imap_port INT NOT NULL DEFAULT 993,
    username VARCHAR(255) NOT NULL,
    encrypted_password VARCHAR(512) NOT NULL,
    display_label VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_linked_email_user_id ON linked_email_accounts(user_id);
CREATE INDEX idx_linked_email_active ON linked_email_accounts(is_active);
