#!/bin/bash

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

source "${BIN}/verbose.sh"

docker run --rm -ti -v "${PROJECT}:${PROJECT}" -w "$(pwd)" -v /var/run/docker.sock:/var/run/docker.sock --privileged npalm/dind-java:latest "$@"