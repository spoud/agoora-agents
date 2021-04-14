[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=spoud_agoora-agents&metric=alert_status)](https://sonarcloud.io/dashboard?id=spoud_agoora-agents)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=spoud_agoora-agents&metric=coverage)](https://sonarcloud.io/dashboard?id=spoud_agoora-agents)
# Agoora agents

This repository contains all the open source Agoora agents developed by SPOUD AG. An agent is a piece of software that
scrapes a dataset (a database, a streaming platform, an API, …) and reports insights into Agoora. There are two 
types of data: meta data and profile data. The meta data is everything but the actual data. The profiles can contain 
some data samples, so be sure to activate the profiling only if you want it.

The agents are meant to be used with the Agoora product from SPOUD. More info at https://agoora.com/.

All the agents in this repository are free to use/modify/redistribute under the MIT license. You can also use them as
an example if you want to develop a custom Agent for your needs. If you would like to publish a new agent in this
repository please create a ticket so we can discuss it.

They all use the gRPC api of Agoora available at https://github.com/spoud/sdm-proto-api

## How to use

A documentation with deployment examples can be found at https://docs.agoora.com/latest/integration/010-overview.html

## Technology

We decided to use Java together with Quarkus for your agents. We think it's the best fit the cloud and for 
where those agents will be used. 

Our agents are distributed via docker: https://hub.docker.com/u/spoud

## How to contribute

See [CONTRIBUTION.md](./CONTRIBUTION.md)
