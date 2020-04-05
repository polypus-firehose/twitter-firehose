#!/bin/bash
while(true); do docker exec -ti tc_aerospike aql -c "show sets"; sleep 1; clear; done
