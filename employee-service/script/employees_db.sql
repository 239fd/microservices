CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS employee;
SELECT current_database();

CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    login VARCHAR(255) UNIQUE,
    first_name VARCHAR(255),
    password VARCHAR(255),
    phone VARCHAR(13),
    second_name VARCHAR(255),
    surname VARCHAR(255),
    title VARCHAR(20) NOT NULL CHECK (title IN ('director', 'accountant', 'worker')),
    organization_id INT,
    warehouse_id INT
)
