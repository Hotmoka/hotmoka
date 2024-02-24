#!/bin/bash

# This Maven invocation will compile and install all Hotmoka and Takamaka
# jars in your local Maven repository. Remove -DskipTests if you
# what to run the tests as well. Modules get compiled and
# distributed inside the modules/ directory.
# Docker images are created at the end and pushed to DockerHub
# (you must have the right to do that)

if [ -z $1 ]
then
    VERSION=latest
else
    VERSION=$1
fi;

mvn clean install -DskipTests

DOCKER_IMAGE_HOTMOKA=hotmoka/tendermint-node:${VERSION}
DOCKER_IMAGE_BLUEKNOT=veroforchain/tendermint-node:${VERSION}
DOCKER_FILE_HOTMOKA=dockerfiles/tendermint-node/tendermint-node
DOCKER_FILE_BLUEKNOT=dockerfiles/tendermint-node/blueknot-tendermint-node
docker buildx build --push --platform linux/amd64 -t ${DOCKER_IMAGE_HOTMOKA} -f ${DOCKER_FILE_HOTMOKA} .
#docker buildx build --push --platform linux/amd64 -t ${DOCKER_IMAGE_BLUEKNOT} -f ${DOCKER_FILE_BLUEKNOT} .

DOCKER_IMAGE_HOTMOKA=hotmoka/tendermint-node-arm64:${VERSION}
DOCKER_IMAGE_BLUEKNOT=veroforchain/tendermint-node-arm64:${VERSION}
DOCKER_FILE_HOTMOKA=dockerfiles/tendermint-node/tendermint-node-arm64
DOCKER_FILE_BLUEKNOT=dockerfiles/tendermint-node/blueknot-tendermint-node-arm64
docker buildx build --push --platform linux/arm64 -t ${DOCKER_IMAGE_HOTMOKA} -f ${DOCKER_FILE_HOTMOKA} .
#docker buildx build --push --platform linux/arm64 -t ${DOCKER_IMAGE_BLUEKNOT} -f ${DOCKER_FILE_BLUEKNOT} .
