#!/bin/bash

set -e
set -u

function create_user_and_database() {
  local database=$1
  echo "  Creating user and database '$database'"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
      CREATE USER $database WITH PASSWORD 'changeme';
      CREATE DATABASE $database;
      GRANT ALL PRIVILEGES ON DATABASE $database TO $database;
      \c $database;
      CREATE SCHEMA $database;
      GRANT ALL ON SCHEMA $database TO $database;
      GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA $database TO $database;
      GRANT ALL ON ALL SEQUENCES IN SCHEMA $database TO $database;
      GRANT ALL ON ALL FUNCTIONS IN SCHEMA $database TO $database;
EOSQL
}

function create_and_fill_catalog_tables() {
  echo "Creating tables in catalog"
  psql -v ON_ERROR_STOP=1 --username "postgres" -d catalog <<-EOSQL
      CREATE SCHEMA IF NOT EXISTS catalog;
      CREATE TABLE IF NOT EXISTS catalog.products (
          id SERIAL PRIMARY KEY,
          name VARCHAR(500) NOT NULL,
          category VARCHAR(500) NOT NULL,
          discounted_price DECIMAL(10,2),
          actual_price DECIMAL(10,2) NOT NULL,
          discount_percentage DECIMAL(5,2),
          rating DECIMAL(3,2),
          rating_count INT,
          about_product TEXT,
          img_link TEXT,
          product_link TEXT,
          specific_category TEXT,
          combined_text TEXT,
          image_name TEXT,
          image_path TEXT
      );

      ALTER TABLE catalog.products OWNER TO catalog;

      COPY catalog.products(
                            name, category, discounted_price, actual_price,
                            discount_percentage, rating, rating_count, about_product,
                            img_link, product_link, specific_category, combined_text,
                            image_name, image_path
          ) FROM '/tmp/amazon_for_db.csv' DELIMITER ',' CSV HEADER;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
  echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
  for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
    create_user_and_database $db
  done
  echo "Multiple databases created"
  create_and_fill_catalog_tables
  echo "created tables"
fi
