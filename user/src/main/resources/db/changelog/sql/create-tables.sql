CREATE SEQUENCE IF NOT EXISTS user_service.user_seq;

CREATE TABLE IF NOT EXISTS user_service.users
(
    id       BIGINT PRIMARY KEY DEFAULT nextval('user_service.user_seq'),
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
--todo add categories