#!/bin/bash

# An example of a script that runs a node of a blockchain
# that resumes an already existing node

# Source it as follows:
# bash <(curl -H 'Cache-Control: no-cache, no-store' -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/resume.sh) hotmoka 1.5.0
# No keys will be generated. The node will be resumed as it was when it got stopped.

TYPE=${1:-hotmoka}
DIR=${TYPE}_node_info
TYPE_CAPITALIZED=${TYPE^}
if [ $TYPE = hotmoka ];
then
    DOCKER_ID=hotmoka
    CLI=moka
    GITHUB_ID=Hotmoka
    VERSION=${2:-1.5.0}
else
    DOCKER_ID=veroforchain
    CLI=blue
    GITHUB_ID=Vero4Chain
    VERSION=${2:-1.5.0}
fi;

case $(uname -m) in
    arm64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    aarch64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    x86_64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node:${VERSION};;
esac

echo "Starting a node of the $TYPE_CAPITALIZED blockchain, version $VERSION:"
docker rm $TYPE 2>/dev/null >/dev/null

echo " * downloading the blockchain CLI"
mkdir $DIR/$CLI
cd $DIR/$CLI
wget --quiet https://github.com/${GITHUB_ID}/${TYPE}/releases/download/v${VERSION}/${CLI}_${VERSION}.tar.gz
tar zxf ${CLI}_${VERSION}.tar.gz
cd ../..

echo " * starting the docker container"
docker run -dit --name $TYPE -p 80:8001 -p 26656:26656 -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} resume >/dev/null

echo " * waiting for the node to complete its initialization"
sleep 10
echo "     waiting..."
while !(./${CLI}/${CLI} info --uri ws://localhost:80 2>/dev/null >/dev/null)
do
    echo "     waiting..."
done
