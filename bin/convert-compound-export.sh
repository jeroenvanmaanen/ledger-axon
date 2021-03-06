#!/usr/bin/env bash

# Convert an export of the compound collection of the old React-only implementation of Ledger that was made
# with NoSQLBooster for MongoDB using the MongoShell BSON format, to the YAML format that can be imported
# in the new Axon-based back-end for Ledger.

SED_EXT=-r
case "$(uname)" in
Darwin*)
        SED_EXT=-E
esac
export SED_EXT

TAB="$(echo -en '\011')"

cat "$@" \
    | sed "${SED_EXT}" \
        -e '/^[}],/a\
---' \
        -e '/^[}],/s/,//' \
        -e "s/^(${TAB}*)${TAB}/\\1        /" \
        -e "s/^(${TAB}*)${TAB}/\\1        /" \
        -e "s/^(${TAB}*)${TAB}/\\1        /" \
        -e "s/^(${TAB}*)${TAB}/\\1        /" \
        -e "/^        \"_id\"/s/_id/key/" \
        -e '/\"_id\"/d' \
        -e '/\"date\"/d' \
        -e 's/NumberInt[(](-?[0-9]*)[)]/\1/g' \
        -e 's/ObjectId[(]("[^"]*")[)]/\1/g' \
        -e 's/("amount" *: *)"(-?[0-9]*)"/\1\2/' \
        -e 's/"transactions"/"members"/' \
        -e 's/"amount"/"amountCents"/' \
        -e "/^        \"/s/^/|/" \
        -e 's/[[]$/{/' \
        -e 's/[]]$/}/' \
        -e '/[{]/s/^/|/' \
    | tr '|\012' '\012|' \
    | egrep  '^([{]|.*"(key|members|intendedJar)")' \
    | sed "${SED_EXT}" -e 's/[{](.*"key" *: *)([^,]*)/\2 : {\1\2/' \
    | tr '|\012' '\012|' \
    | sed -e 's/^[|]//'
