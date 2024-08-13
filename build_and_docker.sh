#!/bin/bash

# This Maven invocation will compile and install all Hotmoka
# jars in your local Maven repository. Remove -DskipTests if you
# what to run the tests as well.
# Docker images are created at the end and pushed to DockerHub
# (you must have the right to do that)
# A moka_VERSION.tar.gz archive get created containing all modules.
# for running the moka script.


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

DOCKER_IMAGE_HOTMOKA=hotmoka/mokamint-node:${VERSION}
DOCKER_FILE_HOTMOKA=dockerfiles/mokamint-node/mokamint-node
docker buildx build --push --platform linux/amd64 -t ${DOCKER_IMAGE_HOTMOKA} -f ${DOCKER_FILE_HOTMOKA} .

tar -cvf moka_${VERSION}.tar --directory io-hotmoka-moka modules moka moka.bat; gzip moka_${VERSION}.tar
