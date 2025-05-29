CREATE SEQUENCE IF NOT EXISTS user_service.user_seq;

CREATE TABLE IF NOT EXISTS user_service.users
(
    id       BIGINT PRIMARY KEY DEFAULT nextval('user_service.user_seq'),
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
CREATE TABLE user_service.roles
(
    id   BIGINT PRIMARY KEY DEFAULT nextval('user_service.user_seq'),
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE user_service.user_roles
(
    user_id BIGINT REFERENCES users (id),
    role_id BIGINT REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

-- Insert the ADMIN role if not already present
INSERT INTO user_service.roles (name)
VALUES ('ADMIN')
ON CONFLICT (name) DO NOTHING;
-- Insert the USER role if not already present
INSERT INTO user_service.roles (name)
VALUES ('USER')
ON CONFLICT (name) DO NOTHING;


-- Insert the admin user if not already present
INSERT INTO user_service.users (username, password)
VALUES ('admin@admin.hr', '$2a$10$nJtt9SDTfFmPKqBuN6XSge.YyCxJWErhCl9b1L.Uu1v/4dhQdf7y.')
ON CONFLICT (username) DO NOTHING;
-- Password: "admin" (BCrypt-hashed using 10 rounds)

-- Link admin user to ADMIN role
INSERT INTO user_service.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM user_service.users u
         JOIN user_service.roles r ON r.name = 'ADMIN'
WHERE u.username = 'admin@admin.hr'
ON CONFLICT DO NOTHING;
