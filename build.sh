#!/bin/bash
set -e

cd builder && ./build.sh && cd ..
docker build -t brunneis/twitter-firehose .

cd json-adapter
docker pull catenae/link:2.0.0
docker build -t brunneis/twitter-firehose-json-adapter .
