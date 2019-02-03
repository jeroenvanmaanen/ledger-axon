#!/bin/bash

set -e

SED_EXT=-r
case $(uname) in
Darwin*)
        SED_EXT=-E
esac
export SED_EXT

COMPOSE="$(cd "$(dirname "$0")" ; pwd)"
PROJECT="$(dirname "${COMPOSE}")"
PRESENT="${PROJECT}/present"
DOCKER_REPOSITORY='ledger'

: ${SILENT:=true}
. "${PROJECT}/bin/verbose.sh"

: ${MONGO_SERVER_PORT:=27017}
: ${EXTRA_VOLUMES:=}
source "${COMPOSE}/etc/settings-local.sh"

VOLUMES=''
if [[ -n "${EXTRA_VOLUMES}" ]]
then
    VOLUMES="
    volumes:${EXTRA_VOLUMES}"
fi

MONGO_PORTS=' Do not expose Mongo DB port'
PRESENT_SUFFIX=''
PRESENT_VOLUMES=' No volumes'
if [[ ".$1" = '.--dev' ]]
then
    shift
    MONGO_PORTS="
    ports:
    -
      target: 27017
      published: ${MONGO_SERVER_PORT}"
    PRESENT_SUFFIX='-dev'
    PRESENT_VOLUMES=" Mount local volume for development
    volumes:
    -
      type: bind
      source: ${PRESENT}
      target: ${PRESENT}
    working_dir: ${PRESENT}"
fi

BASE="${COMPOSE}/docker-compose"
TEMPLATE="${BASE}-template.yml"
TARGET="${BASE}.yml"
VARIABLES="$(tr '$\012' '\012$' < "${TEMPLATE}" | sed -e '/^[{][A-Za-z_][A-Za-z0-9_]*[}]/!d' -e 's/^[{]//' -e 's/[}].*//')"

function re-protect() {
    sed "${SED_EXT}" -e 's/([[]|[]]|[|*?^$()/])/\\\1/g' -e 's/$/\\/g' -e '$s/\\$//'
}

function substitute() {
    local VARIABLE="$1"
    local VALUE="$(eval "echo \"\${${VARIABLE}}\"" | re-protect)"
    log "VALUE=[${VALUE}]"
    if [[ -n "$(eval "echo \"\${${VARIABLE}+true}\"")" ]]
    then
        sed "${SED_EXT}" -e "s/[\$][{]${VARIABLE}[}]/${VALUE}/g" "${TARGET}" > "${TARGET}~"
        mv "${TARGET}~" "${TARGET}"
    fi
}

cp "${TEMPLATE}" "${TARGET}"
for VARIABLE in ${VARIABLES}
do
    log "VARIABLE=[${VARIABLE}]"
    substitute "${VARIABLE}"
done
"${SILENT}" || diff -u "${TEMPLATE}" "${TARGET}" || true

(
    cd "${COMPOSE}"
    docker-compose up
)