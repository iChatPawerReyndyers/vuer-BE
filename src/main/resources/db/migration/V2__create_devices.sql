CREATE TABLE devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    nickname VARCHAR(255),
    model VARCHAR(255),
    device_token VARCHAR(255) NOT NULL UNIQUE,
    platform VARCHAR(50) NOT NULL,
    fcm_token VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at TIMESTAMP NOT NULL
);