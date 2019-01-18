# Ledger

## Introduction

Af few years ago somebody suggested that we should take a look at the [Jars System of Financial Management](6jars.com).
After some thinking and adjustments we opened some extra savings accounts at [Triodos Bank](triodos.nl) (at no extra
cost). The disadvantage of savings accounts is that we could not pay bills directly from those accounts. Therefore
we decided to fill the jars from the account that receives our income (INC). The bank account that corresponds to the
Necessity Account (NEC) is an ordinary bank account. That is also the account to which we can make withdrawals of the
savings accounts. So the money flows from Employer to INC to JAR to NEC to Others. The exception is the Play Account
(PLY) that is also a normal bank account. When we go on holiday we transfer the holiday budget from SAV to PLY.

So we sometimes have to compensate payments that we do from NEC from one of the savings accounts. Of course we
sometimes forget or we forget that we already did it and do it a second time. So we would make lists and check them
again and again. Now and then we lose a list, and still we have discussions and mistakes. I thought it would be
practical if we could download the bank statements, dump them in a database and have a web application that lets us
annotate the transactions to replace the lists.

## Architecture

The data is stored in MongoDB.

There is a generic REST interface that provides access to the data.

All the logic is in the front-end.

The front-end is a static web application written in ReactJS that is deployed on Nginx.

All three components (MongoDB, REST API, and Nginx) run in separate Docker containers.

## Setup

1. Clone the repository
2. Run: `bin/create-local-settings.sh`
3. Edit `data/*-local.*`
4. Run: `npm run build`
5. Run: `bin/docker-run.sh`
