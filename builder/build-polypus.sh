#!/bin/bash
rm -rf target
mvn package
cp target/twitter-crawler-*-jar-with-dependencies.jar crawler.jar
