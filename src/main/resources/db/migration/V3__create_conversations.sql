CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    sender_identity VARCHAR(255) NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(255),
    last_message_at TIMESTAMP,
    UNIQUE (user_id, sender_identity, channel_type)
);