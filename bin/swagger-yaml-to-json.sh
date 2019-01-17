#!/usr/bin/env bash

BIN="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${BIN}")"

if [[ ".$1" = '.-v' ]]
then
    docker run --rm -v "${PROJECT}:${PROJECT}" -w "${PROJECT}" simplealpine/yaml2json:latest core/src/main/resources/web/etc/swagger.yaml \
        | tee core/src/main/resources/web/etc/swagger.json
else
    docker run --rm -v "${PROJECT}:${PROJECT}" -w "${PROJECT}" simplealpine/yaml2json:latest core/src/main/resources/web/etc/swagger.yaml \
        | docker run --rm -i stedolan/jq . \
        > core/src/main/resources/web/etc/swagger.json
fi
