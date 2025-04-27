CREATE SCHEMA IF NOT EXISTS organization;
SELECT current_database();

CREATE TABLE organization (
    id SERIAL PRIMARY KEY,
    INN VARCHAR(9) NOT NULL UNIQUE,
    name VARCHAR(45) NOT NULL,
    address VARCHAR(255) NOT NULL
)
