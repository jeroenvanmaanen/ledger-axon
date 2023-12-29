#!/usr/bin/env bash

BIN="$(cd "$(dirname "$0")" || exit ; pwd)"
PROJECT="$(dirname "${BIN}")"

docker run --rm -i -v "${PROJECT}:${PROJECT}" -w "${PROJECT}" -e 'NODE_OPTIONS=--openssl-legacy-provider' node:21 npm install