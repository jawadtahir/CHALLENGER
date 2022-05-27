# CHALLENGER

Challenger service was used in ACM DEBS 21 [1] and ACM DEBS 22 [2] to disseminate data and benchmark solutions of the Grand Challenge participants.

## Software prereq

- Gradle
- PostgreSQL
  - You need to create the database and the valid users first.
  - Run the following commands
```
# Login Database

sudo -iu postgres
psql

# Create a database

create database bandency;
create user bandency with encrypted password 'bandency';
grant all privileges on database bandency to bandency;

# Run in query tool

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

## How to run CHALLENGER

+ Download the dataset from Zonodo[] and place the zip file in resources folder.
+ Run [Main](src/main/java/de/tum/i13/Main.java)
+ gRPC server now runs at port 52923
