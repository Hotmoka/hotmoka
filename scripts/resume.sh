#!/bin/bash

# An example of a script that runs a node of a blockchain
# that resumes an already existing node

# Source it as follows:
# bash <(curl -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/resume.sh) hotmoka 1.5.0
# No keys will be generated. The node will be resumed as it was when it got stopped.

TYPE=${1:-hotmoka}

TYPE_CAPITALIZED=${TYPE^}
if [ $TYPE = hotmoka ];
then
    DOCKER_ID=hotmoka
    VERSION=${2:-1.5.0}
else
    DOCKER_ID=veroforchain
    VERSION=${2:-1.5.0}
fi;

case $(uname -m) in
    arm64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    aarch64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    x86_64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node:${VERSION};;
esac

echo "Starting a node of the $TYPE_CAPITALIZED blockchain, version $VERSION:"
docker rm $TYPE 2>/dev/null >/dev/null

echo " * starting the docker container"
docker run -dit --name $TYPE -p 80:8001 -p 26656:26656 -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} resume >/dev/null

echo " * waiting for the node to complete its initialization"
sleep 10
echo "     waiting..."
while !(moka node info --uri ws://localhost:80 --timeout 2000 2>/dev/null >/dev/null)
do
    echo "     waiting..."
done
