CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    device_id UUID NOT NULL REFERENCES devices(id),
    sender_address VARCHAR(255) NOT NULL,
    body_encrypted TEXT NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    original_timestamp TIMESTAMP NOT NULL,
    ingested_at TIMESTAMP NOT NULL
);