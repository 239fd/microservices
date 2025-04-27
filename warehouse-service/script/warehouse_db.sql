CREATE SCHEMA IF NOT EXISTS warehouse;
SELECT current_database();

CREATE TABLE warehouse (
    id SERIAL PRIMARY KEY,
    name VARCHAR(45) NOT NULL,
    address VARCHAR(255) NOT NULL,
    organization_id INT NOT NULL
);

CREATE TABLE rack (
    id SERIAL PRIMARY KEY,
    capacity INT NOT NULL,
    warehouse_id INT NOT NULL
);

CREATE TABLE cell (
    id SERIAL PRIMARY KEY,
    length DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION,
    rack_id INT NOT NULL
);