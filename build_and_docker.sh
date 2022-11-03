#!/bin/bash

# This Maven invocation will compile and install all Hotmoka and Takamaka
# jars in your local Maven repository. Remove -DskipTests if you
# what to run the tests as well. Modules get compiled and
# distributed inside the modules/ directory.
# Docker images are created at the end and pushed to DockerHub
# (you must have the right to do that)

mvn clean install -DskipTests
docker build -t hotmoka/tendermint-node:latest -f dockerfiles/tendermint-node/tendermint-node .
docker push hotmoka/tendermint-node:latest
docker build -t veroforchain/tendermint-node:latest -f dockerfiles/tendermint-node/blueknot-tendermint-node .
docker push veroforchain/tendermint-node:latest

