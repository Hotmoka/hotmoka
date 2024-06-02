#!/bin/bash

# An example of a script that runs a node of a blockchain
# that clones and synchronizes with a remote node

# Source it as follows (to clone the node at panarea.hotmoka.io)
# bash <(curl -H 'Cache-Control: no-cache, no-store' -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/clone.sh) hotmoka 1.6.0 ws://panarea.hotmoka.io
# The validation keys of the node will be randomly generated. If you want to specify
# such keys (because, for instance, you were a validator already and want to start the
# same node again) then you can provided the address of the validator account:
# this script will assume that you possess the corresponding pem file in the
# hotmoka_node_info directory:
# bash <(curl -H 'Cache-Control: no-cache, no-store' -s https://raw.githubusercontent.com/Hotmoka/hotmoka/master/scripts/clone.sh) hotmoka 1.6.0 ws://panarea.hotmoka.io validator

TYPE=${1:-hotmoka}
VERSION=${2:-1.6.0}

TYPE_CAPITALIZED=${TYPE^}
DIR=${TYPE}_node_info
if [ $TYPE = hotmoka ];
then
    DOCKER_ID=hotmoka
    NETWORK_URI=${3:-ws://panarea.hotmoka.io}
    GITHUB_ID=Hotmoka
    CLI=moka
else
    DOCKER_ID=veroforchain
    NETWORK_URI=${3:-ws://blueknot.vero4chain.it}
    GITHUB_ID=Vero4Chain
    CLI=blue
fi;

case $(uname -m) in
    arm64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    aarch64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node-arm64:${VERSION};;
    x86_64) DOCKER_IMAGE=${DOCKER_ID}/tendermint-node:${VERSION};;
esac

echo "Starting a node of the $TYPE_CAPITALIZED blockchain at $NETWORK_URI, version $VERSION:"
docker rm $TYPE 2>/dev/null >/dev/null

if [ ! -z "$4" ]
then
    echo "Assuming the pem of $4 to be in the $DIR directory."

    echo " * downloading the blockchain CLI"
    rm -r $DIR/${CLI} 2>/dev/null
    mkdir $DIR/${CLI}
    cd $DIR/${CLI}
    wget --quiet https://github.com/${GITHUB_ID}/${TYPE}/releases/download/v${VERSION}/${CLI}_${VERSION}.tar.gz
    tar zxf ${CLI}_${VERSION}.tar.gz
    cd ../..

    echo " * extracting keys of the previous validator"
    cd $DIR
    KEYS=$(./${CLI}/${CLI} show-account ${4} --keys --interactive=false --password= --uri ${NETWORK_URI})
    cd ..
    LINE6=$(echo "$KEYS"| sed '6!d')
    PUBLIC_KEY_BASE58=${LINE6:19}
    echo "   -> public key base58 of this node as validator: $PUBLIC_KEY_BASE58"
    LINE7=$(echo "$KEYS"| sed '7!d')
    PUBLIC_KEY_BASE64=${LINE7:19}
    echo "   -> public key base64 of this node as validator: $PUBLIC_KEY_BASE64"
    LINE8=$(echo "$KEYS"| sed '8!d')
    CONCATENATED_KEYS_BASE64=${LINE8:40}
    LINE9=$(echo "$KEYS"| sed '9!d')
    TENDERMINT_ADDRESS=${LINE9:25}
    echo "   -> Tendermint address of this node as validator: $TENDERMINT_ADDRESS"

    KEYS=
    rm -r ${DIR}/${CLI}
fi;

echo " * starting the docker container"
if [ ! -z "$4" ]
then
    docker run -dit --name $TYPE -p 80:8001 -p 26656:26656 -e NETWORK_URI=${NETWORK_URI} -e PUBLIC_KEY_BASE58=${PUBLIC_KEY_BASE58} -e PUBLIC_KEY_BASE64=${PUBLIC_KEY_BASE64} -e CONCATENATED_KEYS_BASE64=${CONCATENATED_KEYS_BASE64} -e TENDERMINT_ADDRESS=${TENDERMINT_ADDRESS} -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} start >/dev/null
else
	rm -r $DIR 2>/dev/null
	mkdir -m700 $DIR
    docker run -dit --name ${TYPE} -p 80:8001 -p 26656:26656 -e NETWORK_URI=${NETWORK_URI} -v chain:/home/${TYPE}/chain ${DOCKER_IMAGE} start >/dev/null
fi;

CONCATENATED_KEYS_BASE64=

echo " * waiting for the node to complete its initialization"
sleep 10
echo "     waiting..."
while !(docker exec ${TYPE} bash -c "mkdir -p extract; cp -f *.pem extract" 2>/dev/null)
do
    sleep 10
    echo "     waiting..."
done

if [ -z "$4" ]
then
	echo " * extracting the pem of the validator key"
	cd $DIR
	docker cp ${TYPE}:/home/${TYPE}/extract/. . >/dev/null
	VALIDATOR_KEY=$(ls *.pem)
	ln -s ${VALIDATOR_KEY} validator_key.pem

	echo
	echo "The pem file of the key to control the node as validator"
	echo "(if it will ever be a validator) has been saved in the directory \"${DIR}\"."
	echo "Move that directory to the clients that need to control the node and delete it from this server."
	echo
	echo "The password of the validator key is empty."
fi;

cd ..
