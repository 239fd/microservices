CREATE SCHEMA IF NOT EXISTS product;
SELECT current_database();

CREATE TABLE product (
    id SERIAL PRIMARY KEY,
    amount INT NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    length DOUBLE PRECISION NOT NULL,
    name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('accepted', 'nonverified', 'writeoff')),
    unit VARCHAR(255) NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    weight DOUBLE PRECISION,
    bestbeforedate DATE
);

CREATE TABLE cell_has_product (
    cell_id INT NOT NULL,
    product_id INT NOT NULL,
    barcode_pdf VARCHAR(12),
    PRIMARY KEY (cell_id, product_id)
);