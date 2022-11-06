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

echo $VERSION

case $(uname -m) in
    arm64)
	DOCKER_IMAGE_HOTMOKA=hotmoka/tendermint-node-arm64:${VERSION}
    	DOCKER_IMAGE_BLUEKNOT=veroforchain/tendermint-node-arm64:${VERSION};;
    aarch64)
	DOCKER_IMAGE_HOTMOKA=hotmoka/tendermint-node-arm64:${VERSION}
	DOCKER_IMAGE_BLUEKNOT=veroforchain/tendermint-node-arm64:${VERSION};;
    x86_64)
	DOCKER_IMAGE_HOTMOKA=hotmoka/tendermint-node:${VERSION}
	DOCKER_IMAGE_BLUEKNOT=veroforchain/tendermint-node:${VERSION};;
esac

mvn clean install -DskipTests
docker build -t ${DOCKER_IMAGE_HOTMOKA} -f dockerfiles/tendermint-node/tendermint-node .
docker push ${DOCKER_IMAGE_HOTMOKA}
docker build -t ${DOCKER_IMAGE_BLUEKNOT} -f dockerfiles/tendermint-node/blueknot-tendermint-node .
docker push ${DOCKER_IMAGE_BLUEKNOT}

