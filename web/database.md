
# Login Database

sudo -iu postgres
psql

# Create a database

create database bandency;
create user bandency with encrypted password 'bandency';
grant all privileges on database bandency to bandency;

# Run in query tool

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";