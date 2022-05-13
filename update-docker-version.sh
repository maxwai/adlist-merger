#!/bin/bash

docker build --no-cache -t "maxwai/adlist-merger:$1" .
docker build -t maxwai/adlist-merger:latest .
docker push "maxwai/adlist-merger:$1"
docker push maxwai/adlist-merger:latest