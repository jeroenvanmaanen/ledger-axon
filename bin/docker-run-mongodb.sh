#!/bin/bash

docker run -d --name ledger-axon-mongodb -p 27717:27017 --hostname mongodb mongo:3.6
