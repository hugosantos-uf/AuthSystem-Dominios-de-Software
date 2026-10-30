CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);
