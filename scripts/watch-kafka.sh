#!/bin/bash
docker exec -ti tc_kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic new_texts
