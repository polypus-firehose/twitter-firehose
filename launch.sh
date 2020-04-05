#!/bin/bash
./build.sh
docker-compose down 2> /dev/null
docker-compose up -d