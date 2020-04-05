#!/bin/bash
docker build -t maven .
dir=$(cd .. && pwd)
docker run -v $dir:/tmp/polypus -ti maven
