#!/bin/bash

# An example of a script that runs a node of a blockchain
# that resumes an already existing node

# Source it as follows (to resume the node at panarea.hotmoka.io)
# bash <(curl -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/resume.sh) hotmoka panarea.hotmoka.io
# No keys will be generated. The node will be resumed as it was when it got stopped.

TYPE=${1:-hotmoka}

TYPE_CAPITALIZED=${TYPE^}
if [ $TYPE = hotmoka ];
then
    DOCKER_ID=hotmoka
    NETWORK_URL=${2:-panarea.hotmoka.io}
else
    DOCKER_ID=veroforchain
    NETWORK_URL=${2:-blueknot.vero4chain.it}
fi;

VERSION=$(curl --silent http://${NETWORK_URL}/get/nodeID| python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")

case $(uname -m) in
    arm64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    aarch64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    x86_64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node:${VERSION};;
esac

echo "Starting a node of the $TYPE_CAPITALIZED blockchain at $NETWORK_URL, version $VERSION:"
docker rm $TYPE 2>/dev/null >/dev/null

echo " * starting the docker container"
docker run -dit --name $TYPE -p 80:8080 -p 26656:26656 -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} resume >/dev/null

echo " * waiting for the node to complete its initialization"
sleep 10
echo "     waiting..."
while !(curl --silent localhost/get/nodeID 2>/dev/null >/dev/null)
do
    sleep 10
    echo "     waiting..."
done