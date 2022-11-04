#!/bin/bash

# an example of a script that runs a node of a blockchain
# that clones and synchronizes with a remote node

# source it as follows (to clone the node at panarea.hotmoka.io)
# curl -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/clone.sh | bash -s hotmoka panarea.hotmoka.io

TYPE=${1:-hotmoka}

TYPE_CAPITALIZED=${TYPE^}
DIR=${TYPE}_node_info
if [ $TYPE = hotmoka ];
then
    DOCKER_ID=hotmoka
    NETWORK_URL=${2:-panarea.hotmoka.io}
else
    DOCKER_ID=veroforchain
    NETWORK_URL=${2:-blueknot.vero4chain.it}
fi;

VERSION=$(curl --silent http://$NETWORK_URL/get/nodeID| python3 -c "import sys, json; print(json.load(sys.stdin)['version'])")

case $(uname -m) in
    arm64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    aarch64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    x86_64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node:${VERSION};;
esac

echo "Starting a node of the $TYPE_CAPITALIZED blockchain at $NETWORK_URL, version $VERSION:"
docker rm $TYPE 2>/dev/null >/dev/null
rm -r $DIR 2>/dev/null
mkdir $DIR

echo " * starting the docker container"
docker run -dit --name $TYPE -p 80:8080 -p 26656:26656 -e NETWORK_URL=$NETWORK_URL -v chain:/home/$TYPE/chain $DOCKER_IMAGE start >/dev/null

echo " * waiting for the node to complete its initialization"
sleep 10
echo "     waiting..."
while !(docker exec $TYPE bash -c "mkdir -p extract; cp -f *.pem extract" 2>/dev/null)
do
    sleep 10
    echo "     waiting..."
done

echo " * extracting the pem of the validator key"
cd $DIR
docker cp $TYPE:/home/$TYPE/extract/. .
VALIDATOR_KEY=$(ls *.pem)
ln -s $VALIDATOR_KEY validator_key.pem
cd ..

echo
echo "The pem file of the key to control the node as validator"
echo "(if it will ever be a validator) has been saved in the directory \"${DIR}\"."
echo "Move that directory to the clients that need to control the node and delete it from this server."
echo
echo "The password of the validator key is empty."
