CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_profiles (
                               user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                               username VARCHAR(50) UNIQUE NOT NULL,
                               age_range VARCHAR(20),
                               city VARCHAR(120),
                               phone VARCHAR(40),
                               updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
