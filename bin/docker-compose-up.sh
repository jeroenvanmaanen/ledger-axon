#!/bin/bash

BIN="$(cd "$(dirname "$0")"; pwd)"
PROJECT="$(dirname "${BIN}")"

"${PROJECT}/ledger/docker-compose-up.sh" "$@"
