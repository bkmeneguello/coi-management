#!/bin/bash

java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.2.9/hsqldb-2.2.9.jar org.hsqldb.Server --database.0 file:/home/bruno/coi-db/schema --dbname.0 coi
