# CHALLENGER

Challenger service was used in ACM DEBS 21 [1] and ACM DEBS 22 [2] to disseminate data and benchmark solutions of the Grand Challenge participants.

## Software prereq

- Python 3.8
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

# Create database

./web/upgradedb.sh
```

## How to run CHALLENGER

+ Download the dataset from Zonodo [3] and place the zip file in resources folder.
+ Run [Main](src/main/java/de/tum/i13/Main.java)
+ gRPC server now runs at port 52923

## How to run frontend
+ Install [dependencies](web/notes/notes.md)
+ Then run [startwebserver](web/startwebserver)
```shell
cd web
./startwebserver
```

## How to create groups and get API token

+ Run the [admin](web/frontend/admin.py) file as:
```shell
python frontend/admin.py --email someemail@mail.com --skipmail true --makevm false
```


## References 

1. [Jawad Tahir, Christoph Doblander, Ruben Mayer, Sebastian Frischbier, and Hans-Arno Jacobsen. 2021. The DEBS 2021 grand challenge: analyzing environmental impact of worldwide lockdowns. In <i>Proceedings of the 15th ACM International Conference on Distributed and Event-based Systems</i> (<i>DEBS '21</i>). Association for Computing Machinery, New York, NY, USA, 136–141.](https://doi.org/10.1145/3465480.3467836)


2. [Sebastian Frischbier, Jawad Tahir, Christoph Doblander, Arne Hormann, Ruben Mayer, and Hans-Arno Jacobsen. 2022. The DEBS 2022 Grand Challenge: Detecting Trading Trends in Financial Tick Data. In The 16th ACM International Conference on Distributed and Event-based Systems (DEBS ’22), June 27-July 1, 2022, Copenhagen,Denmark.ACM,New York,NY,USA, 6 pages.](https://doi.org/10.1145/3524860.3539645)


3. [Frischbier, Sebastian, Tahir, Jawad, Doblander, Christoph, Hormann, Arne, Mayer, Ruben, & Jacobsen, Hans-Arno. (2022). DEBS 2022 Grand Challenge Data Set: Trading Data [Data set]. The 16th ACM International Conference on Distributed and Event-based Systems (DEBS '22), Copenhagen. Zenodo. https://doi.org/10.5281/zenodo.6382482](https://zenodo.org/record/6382482#.YpPrV3VBxGo)4